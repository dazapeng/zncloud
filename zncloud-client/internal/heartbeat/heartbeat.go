package heartbeat

import (
	"encoding/json"
	"fmt"
	"math"
	"net/url"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"zncloud-client/internal/command"
	"zncloud-client/internal/config"
	"zncloud-client/internal/logger"
)

const (
	// Max consecutive heartbeat timeouts before declaring connection dead
	maxHeartbeatTimeouts = 3

	// Write deadline for WebSocket messages
	writeDeadline = 10 * time.Second

	// Read deadline for WebSocket messages
	readDeadline = 60 * time.Second
)

// HeartbeatMessage represents the heartbeat JSON message sent via WebSocket
type HeartbeatMessage struct {
	Type        string  `json:"type"`
	DeviceID    string  `json:"deviceId"`
	CPUUsage    float64 `json:"cpuUsage"`
	MemoryUsage float64 `json:"memoryUsage"`
	Timestamp   string  `json:"timestamp"`
}

// ServerMessage represents a message received from the server via WebSocket
type ServerMessage struct {
	Type   string          `json:"type"`
	Action string          `json:"action,omitempty"`
	Params json.RawMessage `json:"params,omitempty"`
}

// Manager manages the WebSocket connection and heartbeat loop
type Manager struct {
	cfg        *config.Config
	deviceID   string
	conn       *websocket.Conn
	mu         sync.RWMutex
	stopCh     chan struct{}
	stopped    bool
	connected  bool
	timeoutCnt int
	cmdHandler *command.Handler
}

// NewManager creates a new WebSocket heartbeat manager
func NewManager(cfg *config.Config, deviceID string) *Manager {
	return &Manager{
		cfg:      cfg,
		deviceID: deviceID,
		stopCh:   make(chan struct{}),
	}
}

// SetCommandHandler sets the command handler for processing server commands
func (m *Manager) SetCommandHandler(handler *command.Handler) {
	m.cmdHandler = handler
}

// Start begins the WebSocket connection and heartbeat loop
func (m *Manager) Start() {
	logger.Info("Starting WebSocket heartbeat manager for device %s", m.deviceID)

	go m.run()
}

// Stop gracefully shuts down the WebSocket connection
func (m *Manager) Stop() {
	m.mu.Lock()
	if m.stopped {
		m.mu.Unlock()
		return
	}
	m.stopped = true
	close(m.stopCh)
	m.mu.Unlock()

	// Close WebSocket connection
	m.mu.RLock()
	if m.conn != nil {
		m.conn.Close()
	}
	m.mu.RUnlock()

	logger.Info("WebSocket heartbeat manager stopped")
}

// IsConnected returns whether the WebSocket is currently connected
func (m *Manager) IsConnected() bool {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.connected
}

// run is the main loop that manages connection and heartbeat
func (m *Manager) run() {
	reconnectInterval := m.cfg.GetReconnectMinInterval()

	for {
		select {
		case <-m.stopCh:
			return
		default:
		}

		// Connect to WebSocket
		if err := m.connect(); err != nil {
			logger.Error("WebSocket connection failed: %v", err)

			// Wait before reconnecting with exponential backoff
			select {
			case <-m.stopCh:
				return
			case <-time.After(time.Duration(reconnectInterval) * time.Second):
			}

			// Exponential backoff
			reconnectInterval = int(math.Min(
				float64(reconnectInterval*2),
				float64(m.cfg.GetReconnectMaxInterval()),
			))
			continue
		}

		// Reset reconnect interval on successful connection
		reconnectInterval = m.cfg.GetReconnectMinInterval()
		m.timeoutCnt = 0

		logger.Info("WebSocket connected successfully")

		// Handle the connection (heartbeat loop + read loop)
		m.handleConnection()

		// If we get here, connection was lost
		logger.Info("WebSocket connection lost, reconnecting...")
	}
}

// connect establishes a WebSocket connection to the server
func (m *Manager) connect() error {
	serverAddr := m.cfg.GetServerAddr()

	// Build WebSocket URL from HTTP server address
	wsScheme := "ws"
	if len(serverAddr) > 4 && serverAddr[:5] == "https" {
		wsScheme = "wss"
	}

	// Remove protocol prefix for URL construction
	host := serverAddr
	if len(host) > 7 && host[:7] == "http://" {
		host = host[7:]
	} else if len(host) > 8 && host[:8] == "https://" {
		host = host[8:]
	}

	u := url.URL{
		Scheme: wsScheme,
		Host:   host,
		Path:   fmt.Sprintf("/ws/device/%s", m.deviceID),
	}

	logger.Debug("Connecting to WebSocket: %s", u.String())

	// Connect with dialer
	dialer := websocket.Dialer{
		HandshakeTimeout: 10 * time.Second,
	}

	conn, _, err := dialer.Dial(u.String(), nil)
	if err != nil {
		return fmt.Errorf("WebSocket dial failed: %w", err)
	}

	m.mu.Lock()
	m.conn = conn
	m.connected = true
	m.mu.Unlock()

	return nil
}

// handleConnection manages the WebSocket connection once established
func (m *Manager) handleConnection() {
	var wg sync.WaitGroup
	wg.Add(2)

	// Goroutine for sending heartbeats
	go func() {
		defer wg.Done()
		m.heartbeatLoop()
	}()

	// Goroutine for reading server messages
	go func() {
		defer wg.Done()
		m.readLoop()
	}()

	wg.Wait()

	// Clean up connection
	m.mu.Lock()
	if m.conn != nil {
		m.conn.Close()
		m.conn = nil
	}
	m.connected = false
	m.mu.Unlock()
}

// heartbeatLoop sends periodic heartbeat messages
func (m *Manager) heartbeatLoop() {
	interval := m.cfg.GetHeartbeatInterval()
	ticker := time.NewTicker(time.Duration(interval) * time.Second)
	defer ticker.Stop()

	// Send initial heartbeat immediately
	m.sendHeartbeat()

	for {
		select {
		case <-m.stopCh:
			return
		case <-ticker.C:
			m.sendHeartbeat()
		}
	}
}

// sendHeartbeat constructs and sends a heartbeat message
func (m *Manager) sendHeartbeat() {
	msg := HeartbeatMessage{
		Type:        "heartbeat",
		DeviceID:    m.deviceID,
		CPUUsage:    getCPUUsage(),
		MemoryUsage: getMemoryUsage(),
		Timestamp:   time.Now().UTC().Format(time.RFC3339),
	}

	data, err := json.Marshal(msg)
	if err != nil {
		logger.Error("Failed to marshal heartbeat: %v", err)
		return
	}

	m.mu.RLock()
	conn := m.conn
	m.mu.RUnlock()

	if conn == nil {
		return
	}

	conn.SetWriteDeadline(time.Now().Add(writeDeadline))
	if err := conn.WriteMessage(websocket.TextMessage, data); err != nil {
		logger.Error("Failed to send heartbeat: %v", err)
		m.timeoutCnt++

		if m.timeoutCnt >= maxHeartbeatTimeouts {
			logger.Warn("Exceeded max heartbeat timeouts (%d), closing connection", maxHeartbeatTimeouts)
			conn.Close()
		}
		return
	}

	m.timeoutCnt = 0
	logger.Debug("Heartbeat sent: CPU=%.1f%%, Memory=%.1f%%", msg.CPUUsage, msg.MemoryUsage)
}

// readLoop reads incoming messages from the WebSocket connection
func (m *Manager) readLoop() {
	for {
		select {
		case <-m.stopCh:
			return
		default:
		}

		m.mu.RLock()
		conn := m.conn
		m.mu.RUnlock()

		if conn == nil {
			return
		}

		conn.SetReadDeadline(time.Now().Add(readDeadline))
		_, message, err := conn.ReadMessage()
		if err != nil {
			// Check if this is a normal close
			if websocket.IsCloseError(err, websocket.CloseNormalClosure, websocket.CloseGoingAway) {
				logger.Info("WebSocket closed normally")
				return
			}
			if websocket.IsUnexpectedCloseError(err, websocket.CloseNormalClosure, websocket.CloseGoingAway) {
				logger.Error("WebSocket unexpected close: %v", err)
				return
			}
			logger.Error("WebSocket read error: %v", err)
			return
		}

		// Process the message
		m.processMessage(message)
	}
}

// processMessage handles an incoming WebSocket message
func (m *Manager) processMessage(data []byte) {
	logger.Debug("Received WebSocket message: %s", string(data))

	var msg ServerMessage
	if err := json.Unmarshal(data, &msg); err != nil {
		logger.Error("Failed to parse server message: %v", err)
		return
	}

	switch msg.Type {
	case "command":
		m.handleCommand(msg)
	case "heartbeat_ack":
		logger.Debug("Heartbeat acknowledged by server")
	case "ping":
		m.handlePing()
	default:
		logger.Debug("Unknown message type: %s", msg.Type)
	}
}

// handleCommand dispatches a command to the command handler
func (m *Manager) handleCommand(msg ServerMessage) {
	if m.cmdHandler == nil {
		logger.Warn("No command handler set, ignoring command: %s", msg.Action)
		return
	}

	logger.Info("Processing command: %s", msg.Action)
	m.cmdHandler.HandleCommand(msg.Action, msg.Params, func(result command.CommandResult) {
		m.sendCommandResult(result)
	})
}

// handlePing responds to a ping message
func (m *Manager) handlePing() {
	m.mu.RLock()
	conn := m.conn
	m.mu.RUnlock()

	if conn == nil {
		return
	}

	pongMsg := map[string]string{"type": "pong", "deviceId": m.deviceID}
	data, _ := json.Marshal(pongMsg)

	conn.SetWriteDeadline(time.Now().Add(writeDeadline))
	if err := conn.WriteMessage(websocket.TextMessage, data); err != nil {
		logger.Error("Failed to send pong: %v", err)
	}
}

// sendCommandResult sends a command result back to the server
func (m *Manager) sendCommandResult(result command.CommandResult) {
	data, err := json.Marshal(result)
	if err != nil {
		logger.Error("Failed to marshal command result: %v", err)
		return
	}

	m.mu.RLock()
	conn := m.conn
	m.mu.RUnlock()

	if conn == nil {
		logger.Warn("Cannot send command result, not connected")
		return
	}

	conn.SetWriteDeadline(time.Now().Add(writeDeadline))
	if err := conn.WriteMessage(websocket.TextMessage, data); err != nil {
		logger.Error("Failed to send command result: %v", err)
	}
}

// getCPUUsage returns current CPU usage percentage (simplified)
func getCPUUsage() float64 {
	// In a real implementation, this would query Windows performance counters
	// For now, return a reasonable default
	return 0.0
}

// getMemoryUsage returns current memory usage percentage (simplified)
func getMemoryUsage() float64 {
	// In a real implementation, this would query Windows performance counters
	// For now, return a reasonable default
	return 0.0
}
