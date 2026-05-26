//go:build windows

package main

import (
	"fmt"
	"os"
	"time"

	"golang.org/x/sys/windows/svc"
	"golang.org/x/sys/windows/svc/debug"
	"golang.org/x/sys/windows/svc/eventlog"

	"zncloud-client/internal/command"
	"zncloud-client/internal/config"
	"zncloud-client/internal/heartbeat"
	"zncloud-client/internal/logger"
	"zncloud-client/internal/registry"
)

// ServiceName is the name of the Windows Service
const ServiceName = "ZNCloudAgent"

// zncloudService implements svc.Handler for the ZNCloud Agent Windows Service
type zncloudService struct {
	cfg            *config.Config
	heartbeatMgr   *heartbeat.Manager
	cmdHandler     *command.Handler
	elog           *eventlog.Log
}

// Execute is the main service handler called by the Windows SCM
func (s *zncloudService) Execute(args []string, requests <-chan svc.ChangeRequest, changes chan<- svc.Status) (svcSpecificEC bool, exitCode uint32) {
	const cmdsAccepted = svc.AcceptStop | svc.AcceptShutdown | svc.AcceptPauseAndContinue

	// Notify SCM that service is starting
	changes <- svc.Status{State: svc.StartPending}

	// Initialize the service
	if err := s.initialize(); err != nil {
		logger.Error("Service initialization failed: %v", err)
		changes <- svc.Status{State: svc.Stopped, Win32ExitCode: 1}
		return false, 1
	}

	// Notify SCM that service is running
	changes <- svc.Status{State: svc.Running, Accepts: cmdsAccepted}

	logger.Info("ZNCloud Agent service started successfully")

	// Main service loop - handle SCM commands
	for {
		select {
		case c := <-requests:
			switch c.Cmd {
			case svc.Interrogate:
				changes <- c.CurrentStatus
			case svc.Stop, svc.Shutdown:
				logger.Info("Service stop/shutdown requested")
				s.shutdown()
				changes <- svc.Status{State: svc.Stopped}
				return false, 0
			case svc.Pause:
				logger.Info("Service pause requested")
				s.pause()
				changes <- svc.Status{State: svc.Paused, Accepts: cmdsAccepted}
			case svc.Continue:
				logger.Info("Service continue requested")
				s.resume()
				changes <- svc.Status{State: svc.Running, Accepts: cmdsAccepted}
			default:
				logger.Debug("Unexpected service control request: %v", c.Cmd)
			}
		}
	}
}

// initialize sets up all service components
func (s *zncloudService) initialize() error {
	logger.Info("Initializing ZNCloud Agent service...")

	// Load configuration
	cfg, err := config.LoadConfig("")
	if err != nil {
		return fmt.Errorf("failed to load config: %w", err)
	}
	s.cfg = cfg

	// Re-initialize logger with correct log directory
	logger.Init(cfg.GetLogDir())
	logger.Info("Configuration loaded. Server address: %s", cfg.GetServerAddr())

	// Register device with backend
	deviceID, err := registry.RegisterDevice(cfg)
	if err != nil {
		return fmt.Errorf("device registration failed: %w", err)
	}

	logger.Info("Device registered with ID: %s", deviceID)

	// Create command handler
	s.cmdHandler = command.NewHandler(cfg)

	// Create and start heartbeat manager
	s.heartbeatMgr = heartbeat.NewManager(cfg, deviceID)
	s.heartbeatMgr.SetCommandHandler(s.cmdHandler)
	s.heartbeatMgr.Start()

	return nil
}

// shutdown gracefully stops all service components
func (s *zncloudService) shutdown() {
	logger.Info("Shutting down ZNCloud Agent...")

	if s.heartbeatMgr != nil {
		s.heartbeatMgr.Stop()
	}

	logger.Info("ZNCloud Agent shutdown complete")
}

// pause temporarily suspends heartbeat/WebSocket activity
func (s *zncloudService) pause() {
	logger.Info("Pausing ZNCloud Agent...")
	if s.heartbeatMgr != nil {
		s.heartbeatMgr.Stop()
	}
}

// resume restarts the heartbeat/WebSocket activity
func (s *zncloudService) resume() {
	logger.Info("Resuming ZNCloud Agent...")
	deviceID := s.cfg.GetDeviceID()
	if deviceID != "" {
		s.heartbeatMgr = heartbeat.NewManager(s.cfg, deviceID)
		s.heartbeatMgr.SetCommandHandler(s.cmdHandler)
		s.heartbeatMgr.Start()
	}
}

// runService starts the Windows Service
func runService(name string, isDebug bool) error {
	service := &zncloudService{}

	if isDebug {
		// Run in debug/console mode (for testing)
		logger.Info("Running in debug mode")
		return debug.Run(name, service)
	}

	// Run as Windows Service
	logger.Info("Running as Windows Service")
	return svc.Run(name, service)
}

func main() {
	// Initialize logging
	logger.Init("")

	// Check if running with -debug flag for console mode
	isDebug := false
	if len(os.Args) > 1 {
		for _, arg := range os.Args[1:] {
			if arg == "-debug" || arg == "--debug" {
				isDebug = true
				break
			}
		}
	}

	// Set up Windows Event Log
	elog, err := eventlog.Open(ServiceName)
	if err != nil {
		// If event log isn't available, we still continue
		logger.Warn("Failed to open Windows Event Log: %v", err)
	} else {
		defer elog.Close()
		elog.Info(1, fmt.Sprintf("%s service starting...", ServiceName))
	}

	// Wait a moment for any setup
	time.Sleep(100 * time.Millisecond)

	// Run the service
	if err := runService(ServiceName, isDebug); err != nil {
		logger.Error("Service execution failed: %v", err)
		if elog != nil {
			elog.Error(1, fmt.Sprintf("Service execution failed: %v", err))
		}
		os.Exit(1)
	}
}
