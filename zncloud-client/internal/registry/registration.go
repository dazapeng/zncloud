package registry

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"zncloud-client/internal/config"
	"zncloud-client/internal/device"
	"zncloud-client/internal/logger"
)

// RegistrationResponse represents the response from the registration API
type RegistrationResponse struct {
	Code    int            `json:"code"`
	Message string         `json:"message"`
	Data    *DeviceData    `json:"data,omitempty"`
}

// DeviceData represents the device data returned by the server
type DeviceData struct {
	ID          string `json:"id"`
	CPUInfo     string `json:"cpuInfo"`
	GPUInfo     string `json:"gpuInfo"`
	MemoryGb    int    `json:"memoryGb"`
	DiskGb      int    `json:"diskGb"`
	OSVersion   string `json:"osVersion"`
	MACAddress  string `json:"macAddress"`
	ConfigLevel string `json:"configLevel"`
}

// RegisterDevice registers the device with the backend server
// If the device is already registered (has a device ID in config), it skips registration
// and returns the existing device ID
func RegisterDevice(cfg *config.Config) (string, error) {
	// Skip registration if already registered
	if cfg.IsRegistered() {
		deviceID := cfg.GetDeviceID()
		logger.Info("Device already registered with ID: %s", deviceID)
		return deviceID, nil
	}

	logger.Info("Starting device registration...")

	// Collect hardware info
	devInfo, err := device.CollectHardwareInfo()
	if err != nil {
		return "", fmt.Errorf("failed to collect hardware info: %w", err)
	}

	logger.Info("Hardware info collected: CPU=%s, GPU=%s, RAM=%dGB, Disk=%dGB, OS=%s, MAC=%s",
		devInfo.CPUInfo, devInfo.GPUInfo, devInfo.MemoryGb, devInfo.DiskGb, devInfo.OSVersion, devInfo.MACAddress)

	// Send registration request
	deviceID, err := sendRegistrationRequest(cfg, devInfo)
	if err != nil {
		return "", fmt.Errorf("registration failed: %w", err)
	}

	// Persist device ID
	if err := cfg.SetDeviceID(deviceID); err != nil {
		return "", fmt.Errorf("failed to persist device ID: %w", err)
	}

	logger.Info("Device registered successfully. Device ID: %s", deviceID)
	return deviceID, nil
}

// ReRegisterDevice re-registers an already registered device (updates hardware info)
func ReRegisterDevice(cfg *config.Config) (string, error) {
	logger.Info("Re-registering device (updating hardware info)...")

	devInfo, err := device.CollectHardwareInfo()
	if err != nil {
		return "", fmt.Errorf("failed to collect hardware info for re-registration: %w", err)
	}

	deviceID, err := sendRegistrationRequest(cfg, devInfo)
	if err != nil {
		return "", fmt.Errorf("re-registration failed: %w", err)
	}

	// Update persisted device ID if it changed
	if existingID := cfg.GetDeviceID(); existingID != deviceID {
		if err := cfg.SetDeviceID(deviceID); err != nil {
			return "", fmt.Errorf("failed to update device ID: %w", err)
		}
	}

	logger.Info("Device re-registered. Device ID: %s", deviceID)
	return deviceID, nil
}

// sendRegistrationRequest sends the HTTP POST request to register the device
func sendRegistrationRequest(cfg *config.Config, devInfo *device.Device) (string, error) {
	serverAddr := cfg.GetServerAddr()
	url := fmt.Sprintf("%s/api/v1/devices/register", serverAddr)

	// Marshal device info to JSON
	body, err := json.Marshal(devInfo)
	if err != nil {
		return "", fmt.Errorf("failed to marshal device info: %w", err)
	}

	logger.Debug("Sending registration request to %s", url)
	logger.Debug("Request body: %s", string(body))

	// Create HTTP client with timeout
	client := &http.Client{
		Timeout: 15 * time.Second,
	}

	// Send POST request
	resp, err := client.Post(url, "application/json", bytes.NewReader(body))
	if err != nil {
		return "", fmt.Errorf("HTTP request failed: %w", err)
	}
	defer resp.Body.Close()

	// Read response body
	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response body: %w", err)
	}

	logger.Debug("Registration response (status=%d): %s", resp.StatusCode, string(respBody))

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("server returned status %d: %s", resp.StatusCode, string(respBody))
	}

	// Parse response
	var regResp RegistrationResponse
	if err := json.Unmarshal(respBody, &regResp); err != nil {
		return "", fmt.Errorf("failed to parse registration response: %w", err)
	}

	if regResp.Code != 200 {
		return "", fmt.Errorf("registration rejected (code=%d): %s", regResp.Code, regResp.Message)
	}

	if regResp.Data == nil || regResp.Data.ID == "" {
		return "", fmt.Errorf("registration response missing device ID")
	}

	return regResp.Data.ID, nil
}
