package config

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"
)

// Default configuration paths
const (
	DefaultLogDir    = "C:\\ProgramData\\ZNCloud\\logs"
	DefaultConfigDir = "C:\\ProgramData\\ZNCloud\\config"
)

// Config holds all configuration for the ZNCloud Agent
type Config struct {
	mu sync.RWMutex

	// ServerAddr is the backend server address (e.g., http://localhost:8080)
	ServerAddr string `json:"server_addr"`

	// DeviceID is the unique device identifier assigned after registration
	DeviceID string `json:"device_id,omitempty"`

	// HeartbeatInterval is the interval in seconds between heartbeats
	HeartbeatInterval int `json:"heartbeat_interval"`

	// ReconnectMinInterval is the minimum reconnection interval in seconds
	ReconnectMinInterval int `json:"reconnect_min_interval"`

	// ReconnectMaxInterval is the maximum reconnection interval in seconds
	ReconnectMaxInterval int `json:"reconnect_max_interval"`

	// LogDir is the directory for log files
	LogDir string `json:"log_dir"`

	// ConfigDir is the directory for config files
	ConfigDir string `json:"config_dir"`

	// Registered indicates whether this device has been registered
	Registered bool `json:"registered"`

	// configFile is the path to the config file
	configFile string
}

// NewConfig creates a new Config with default values
func NewConfig(serverAddr string) *Config {
	if serverAddr == "" {
		serverAddr = "http://localhost:8080"
	}

	return &Config{
		ServerAddr:           serverAddr,
		HeartbeatInterval:    30,
		ReconnectMinInterval: 1,
		ReconnectMaxInterval: 60,
		LogDir:               DefaultLogDir,
		ConfigDir:            DefaultConfigDir,
	}
}

// LoadConfig loads configuration from the specified file path
// If the file doesn't exist, it creates one with default values
func LoadConfig(configDir string) (*Config, error) {
	if configDir == "" {
		configDir = DefaultConfigDir
	}

	// Ensure config directory exists
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return nil, fmt.Errorf("failed to create config directory %s: %w", configDir, err)
	}

	configFile := filepath.Join(configDir, "config.json")

	cfg := NewConfig("http://localhost:8080")
	cfg.ConfigDir = configDir
	cfg.configFile = configFile

	// Try to load existing config
	if data, err := os.ReadFile(configFile); err == nil {
		if err := json.Unmarshal(data, cfg); err != nil {
			return nil, fmt.Errorf("failed to parse config file %s: %w", configFile, err)
		}
	}

	// Always write the config back (ensures any new default fields are saved)
	if err := cfg.Save(); err != nil {
		return nil, fmt.Errorf("failed to save config: %w", err)
	}

	return cfg, nil
}

// Save persists the configuration to disk
func (c *Config) Save() error {
	c.mu.RLock()
	defer c.mu.RUnlock()

	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return fmt.Errorf("failed to marshal config: %w", err)
	}

	if err := os.WriteFile(c.configFile, data, 0644); err != nil {
		return fmt.Errorf("failed to write config file %s: %w", c.configFile, err)
	}

	return nil
}

// GetDeviceID returns the stored device ID
func (c *Config) GetDeviceID() string {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.DeviceID
}

// SetDeviceID sets and persists the device ID
func (c *Config) SetDeviceID(deviceID string) error {
	c.mu.Lock()
	c.DeviceID = deviceID
	c.Registered = true
	c.mu.Unlock()

	return c.Save()
}

// IsRegistered returns whether the device has been registered
func (c *Config) IsRegistered() bool {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.Registered && c.DeviceID != ""
}

// GetServerAddr returns the server address
func (c *Config) GetServerAddr() string {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.ServerAddr
}

// GetHeartbeatInterval returns the heartbeat interval in seconds
func (c *Config) GetHeartbeatInterval() int {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.HeartbeatInterval
}

// GetReconnectMinInterval returns the minimum reconnection interval in seconds
func (c *Config) GetReconnectMinInterval() int {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.ReconnectMinInterval
}

// GetReconnectMaxInterval returns the maximum reconnection interval in seconds
func (c *Config) GetReconnectMaxInterval() int {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.ReconnectMaxInterval
}

// GetLogDir returns the log directory
func (c *Config) GetLogDir() string {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.LogDir
}

// SetServerAddr updates the server address and persists
func (c *Config) SetServerAddr(addr string) error {
	c.mu.Lock()
	c.ServerAddr = addr
	c.mu.Unlock()
	return c.Save()
}
