package command

import (
	"encoding/json"
	"fmt"
	"os/exec"
	"time"

	"zncloud-client/internal/config"
	"zncloud-client/internal/logger"
)

// CommandResult represents the result of executing a command
type CommandResult struct {
	Type    string `json:"type"`
	Action  string `json:"action"`
	Success bool   `json:"success"`
	Message string `json:"message"`
}

// PrepareConnectionParams represents parameters for preparing a remote connection
type PrepareConnectionParams struct {
	SessionID     string `json:"sessionId"`
	ServerAddress string `json:"serverAddress"`
	Port          int    `json:"port"`
	Protocol      string `json:"protocol"`
	Credentials   string `json:"credentials,omitempty"`
	ExtraParams   map[string]interface{} `json:"extraParams,omitempty"`
}

// UpdateConfigParams represents parameters for updating the local configuration
type UpdateConfigParams struct {
	ServerAddr *string `json:"server_addr,omitempty"`
	LogDir     *string `json:"log_dir,omitempty"`
}

// Handler processes commands received from the server via WebSocket
type Handler struct {
	cfg      *config.Config
	activeSession string
}

// NewHandler creates a new command handler
func NewHandler(cfg *config.Config) *Handler {
	return &Handler{
		cfg: cfg,
	}
}

// HandleCommand processes a command and sends the result via the provided callback
func (h *Handler) HandleCommand(action string, params json.RawMessage, callback func(CommandResult)) {
	switch action {
	case "prepare_connection":
		h.handlePrepareConnection(params, callback)
	case "disconnect":
		h.handleDisconnect(callback)
	case "update_config":
		h.handleUpdateConfig(params, callback)
	case "report_status":
		h.handleReportStatus(callback)
	case "reboot":
		h.handleReboot(callback)
	case "poweroff":
		h.handlePowerOff(callback)
	default:
		logger.Warn("Unknown command action: %s", action)
		callback(CommandResult{
			Type:    "command_result",
			Action:  action,
			Success: false,
			Message: fmt.Sprintf("Unknown command action: %s", action),
		})
	}
}

// handlePrepareConnection processes a prepare_connection command
func (h *Handler) handlePrepareConnection(params json.RawMessage, callback func(CommandResult)) {
	logger.Info("Handling prepare_connection command")

	var connParams PrepareConnectionParams
	if err := json.Unmarshal(params, &connParams); err != nil {
		logger.Error("Failed to parse prepare_connection params: %v", err)
		callback(CommandResult{
			Type:    "command_result",
			Action:  "prepare_connection",
			Success: false,
			Message: fmt.Sprintf("Invalid params: %v", err),
		})
		return
	}

	logger.Info("Connection params: SessionID=%s, Server=%s:%d, Protocol=%s",
		connParams.SessionID, connParams.ServerAddress, connParams.Port, connParams.Protocol)

	// Store active session
	h.activeSession = connParams.SessionID

	// Prepare for remote session based on protocol
	switch connParams.Protocol {
	case "rdp":
		h.prepareRDPConnection(connParams)
	case "vnc":
		h.prepareVNCConnection(connParams)
	case "custom":
		h.prepareCustomConnection(connParams)
	default:
		logger.Warn("Unknown connection protocol: %s", connParams.Protocol)
	}

	callback(CommandResult{
		Type:    "command_result",
		Action:  "prepare_connection",
		Success: true,
		Message: fmt.Sprintf("Connection prepared for session %s", connParams.SessionID),
	})
}

// prepareRDPConnection prepares a Remote Desktop connection
func (h *Handler) prepareRDPConnection(params PrepareConnectionParams) {
	logger.Info("Preparing RDP connection to %s:%d", params.ServerAddress, params.Port)
	// In a real implementation, this would configure RDP settings,
	// potentially launch mstsc.exe with appropriate parameters,
	// or configure the Windows Remote Desktop client
}

// prepareVNCConnection prepares a VNC connection
func (h *Handler) prepareVNCConnection(params PrepareConnectionParams) {
	logger.Info("Preparing VNC connection to %s:%d", params.ServerAddress, params.Port)
	// In a real implementation, this would set up a VNC viewer connection
}

// prepareCustomConnection prepares a custom protocol connection
func (h *Handler) prepareCustomConnection(params PrepareConnectionParams) {
	logger.Info("Preparing custom connection to %s:%d (protocol: %s)",
		params.ServerAddress, params.Port, params.Protocol)
	// In a real implementation, this would handle custom connection protocols
}

// handleDisconnect processes a disconnect command
func (h *Handler) handleDisconnect(callback func(CommandResult)) {
	logger.Info("Handling disconnect command")

	if h.activeSession == "" {
		callback(CommandResult{
			Type:    "command_result",
			Action:  "disconnect",
			Success: true,
			Message: "No active session to disconnect",
		})
		return
	}

	sessionID := h.activeSession
	h.activeSession = ""

	logger.Info("Disconnecting session: %s", sessionID)
	// In a real implementation, this would terminate the active remote session

	callback(CommandResult{
		Type:    "command_result",
		Action:  "disconnect",
		Success: true,
		Message: fmt.Sprintf("Session %s disconnected", sessionID),
	})
}

// handleUpdateConfig processes an update_config command
func (h *Handler) handleUpdateConfig(params json.RawMessage, callback func(CommandResult)) {
	logger.Info("Handling update_config command")

	var updateParams UpdateConfigParams
	if err := json.Unmarshal(params, &updateParams); err != nil {
		logger.Error("Failed to parse update_config params: %v", err)
		callback(CommandResult{
			Type:    "command_result",
			Action:  "update_config",
			Success: false,
			Message: fmt.Sprintf("Invalid params: %v", err),
		})
		return
	}

	// Update server address if provided
	if updateParams.ServerAddr != nil {
		if err := h.cfg.SetServerAddr(*updateParams.ServerAddr); err != nil {
			logger.Error("Failed to update server address: %v", err)
			callback(CommandResult{
				Type:    "command_result",
				Action:  "update_config",
				Success: false,
				Message: fmt.Sprintf("Failed to update server address: %v", err),
			})
			return
		}
		logger.Info("Server address updated to: %s", *updateParams.ServerAddr)
	}

	callback(CommandResult{
		Type:    "command_result",
		Action:  "update_config",
		Success: true,
		Message: "Configuration updated successfully",
	})
}

// handleReportStatus processes a report_status command
func (h *Handler) handleReportStatus(callback func(CommandResult)) {
	logger.Info("Handling report_status command")

	// Collect status information
	status := map[string]interface{}{
		"deviceId":       h.cfg.GetDeviceID(),
		"serverAddr":     h.cfg.GetServerAddr(),
		"activeSession":  h.activeSession,
		"uptime":         getSystemUptime(),
		"timestamp":      time.Now().UTC().Format(time.RFC3339),
	}

	statusJSON, err := json.Marshal(status)
	if err != nil {
		logger.Error("Failed to marshal status: %v", err)
		callback(CommandResult{
			Type:    "command_result",
			Action:  "report_status",
			Success: false,
			Message: fmt.Sprintf("Failed to collect status: %v", err),
		})
		return
	}

	callback(CommandResult{
		Type:    "command_result",
		Action:  "report_status",
		Success: true,
		Message: string(statusJSON),
	})
}

// handleReboot processes a reboot command
func (h *Handler) handleReboot(callback func(CommandResult)) {
	logger.Info("Handling reboot command")

	callback(CommandResult{
		Type:    "command_result",
		Action:  "reboot",
		Success: true,
		Message: "System reboot initiated",
	})

	// Execute reboot in a goroutine to allow the response to be sent first
	go func() {
		logger.Info("Executing system reboot...")
		cmd := exec.Command("shutdown", "/r", "/t", "5", "/c", "ZNCloud Agent initiated system reboot")
		if err := cmd.Run(); err != nil {
			logger.Error("Failed to execute reboot: %v", err)
		}
	}()
}

// handlePowerOff processes a poweroff command
func (h *Handler) handlePowerOff(callback func(CommandResult)) {
	logger.Info("Handling poweroff command")

	callback(CommandResult{
		Type:    "command_result",
		Action:  "poweroff",
		Success: true,
		Message: "System shutdown initiated",
	})

	// Execute shutdown in a goroutine to allow the response to be sent first
	go func() {
		logger.Info("Executing system shutdown...")
		cmd := exec.Command("shutdown", "/s", "/t", "5", "/c", "ZNCloud Agent initiated system shutdown")
		if err := cmd.Run(); err != nil {
			logger.Error("Failed to execute shutdown: %v", err)
		}
	}()
}

// getSystemUptime returns the system uptime in seconds (simplified)
func getSystemUptime() int64 {
	// In a real implementation, this would query Windows performance counters
	// or use GetTickCount64 via syscall
	cmd := exec.Command("wmic", "os", "get", "LastBootUpTime", "/format:csv")
	output, err := cmd.Output()
	if err != nil {
		return 0
	}
	_ = output // In full implementation, parse the boot time and compute uptime
	return 0
}
