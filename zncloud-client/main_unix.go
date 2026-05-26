//go:build !windows

package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"

	"zncloud-client/internal/command"
	"zncloud-client/internal/config"
	"zncloud-client/internal/heartbeat"
	"zncloud-client/internal/logger"
	"zncloud-client/internal/registry"
)

// ServiceName is the name of the service
const ServiceName = "ZNCloudAgent"

func main() {
	logger.Init("")
	logger.Info("ZNCloud Agent starting in console mode (non-Windows)...")

	// Load configuration
	cfg, err := config.LoadConfig("")
	if err != nil {
		logger.Error("Failed to load config: %v", err)
		os.Exit(1)
	}

	// Re-init logger with correct log dir
	logger.Init(cfg.GetLogDir())
	logger.Info("Configuration loaded: server=%s", cfg.GetServerAddr())

	// Register device
	deviceID, err := registry.RegisterDevice(cfg)
	if err != nil {
		logger.Error("Device registration failed: %v", err)
		os.Exit(1)
	}
	logger.Info("Device registered: %s", deviceID)

	// Create command handler
	cmdHandler := command.NewHandler(cfg)

	// Create and start heartbeat manager
	heartbeatMgr := heartbeat.NewManager(cfg, deviceID)
	heartbeatMgr.SetCommandHandler(cmdHandler)
	heartbeatMgr.Start()

	logger.Info("Agent started. Press Ctrl+C to stop.")

	// Wait for termination signal
	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

	select {
	case sig := <-sigCh:
		logger.Info("Received signal: %v, shutting down...", sig)
	case <-time.After(24 * time.Hour):
		logger.Info("24-hour timeout reached, shutting down...")
	}

	// Graceful shutdown
	heartbeatMgr.Stop()
	logger.Info("ZNCloud Agent stopped.")
	fmt.Println("ZNCloud Agent stopped.")
}
