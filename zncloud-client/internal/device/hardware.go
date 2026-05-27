package device

import (
	"fmt"
	"os/exec"
	"regexp"
	"runtime"
	"strconv"
	"strings"
)

// ConfigLevel represents the device configuration level
type ConfigLevel string

const (
	ConfigLevelEntry           ConfigLevel = "ENTRY"
	ConfigLevelMainstream      ConfigLevel = "MAINSTREAM"
	ConfigLevelHighPerformance ConfigLevel = "HIGH_PERFORMANCE"
)

// Device represents the hardware device information sent during registration
type Device struct {
	CPUInfo     string      `json:"cpuInfo"`
	GPUInfo     string      `json:"gpuInfo"`
	MemoryGb    int         `json:"memoryGb"`
	DiskGb      int         `json:"diskGb"`
	OSVersion   string      `json:"osVersion"`
	MACAddress  string      `json:"macAddress"`
	PublicIP    string      `json:"publicIp"`
	ConfigLevel ConfigLevel `json:"configLevel"`
}

// CollectHardwareInfo gathers hardware information from the Windows system
func CollectHardwareInfo() (*Device, error) {
	cpuInfo := getCPUInfo()
	gpuInfo := getGPUInfo()
	memoryGb := getTotalMemoryGB()
	diskGb := getTotalDiskGB()
	osVersion := getOSVersion()
	macAddr := getMACAddress()

	configLevel := determineConfigLevel(gpuInfo, memoryGb)

	return &Device{
		CPUInfo:     cpuInfo,
		GPUInfo:     gpuInfo,
		MemoryGb:    memoryGb,
		DiskGb:      diskGb,
		OSVersion:   osVersion,
		MACAddress:  macAddr,
		PublicIP:    "auto-detected",
		ConfigLevel: configLevel,
	}, nil
}

// getCPUInfo retrieves CPU name and core count using wmic
func getCPUInfo() string {
	// Try wmic first
	cmd := exec.Command("wmic", "cpu", "get", "name", "/format:csv")
	output, err := cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		if len(lines) >= 2 {
			parts := strings.Split(lines[1], ",")
			if len(parts) >= 2 {
				name := strings.TrimSpace(parts[1])
				if name != "" {
					return name
				}
			}
		}
	}

	// Fallback: try reading from registry via reg query
	cmd = exec.Command("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString")
	output, err = cmd.Output()
	if err == nil {
		// Parse output like: ProcessorNameString    REG_SZ    Intel(R) Core(TM) i7-12700
		re := regexp.MustCompile(`REG_SZ\s+(.+)`)
		matches := re.FindStringSubmatch(string(output))
		if len(matches) >= 2 {
			return strings.TrimSpace(matches[1])
		}
	}

	return fmt.Sprintf("Unknown CPU (%s)", runtime.GOARCH)
}

// getGPUInfo retrieves GPU name using wmic
func getGPUInfo() string {
	cmd := exec.Command("wmic", "path", "win32_VideoController", "get", "name", "/format:csv")
	output, err := cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		for i := 1; i < len(lines); i++ {
			parts := strings.Split(lines[i], ",")
			if len(parts) >= 2 {
				name := strings.TrimSpace(parts[1])
				if name != "" {
					return name
				}
			}
		}
	}

	return "Unknown GPU"
}

// getTotalMemoryGB retrieves total physical memory in GB
func getTotalMemoryGB() int {
	// Try wmic
	cmd := exec.Command("wmic", "memorychip", "get", "Capacity", "/format:csv")
	output, err := cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		var totalBytes uint64
		for i := 1; i < len(lines); i++ {
			parts := strings.Split(lines[i], ",")
			if len(parts) >= 2 {
				capStr := strings.TrimSpace(parts[1])
				if capStr == "" {
					continue
				}
				capacity, parseErr := strconv.ParseUint(capStr, 10, 64)
				if parseErr == nil {
					totalBytes += capacity
				}
			}
		}
		if totalBytes > 0 {
			return int(totalBytes / (1024 * 1024 * 1024))
		}
	}

	// Fallback: use wmic os get TotalVisibleMemorySize (in KB)
	cmd = exec.Command("wmic", "os", "get", "TotalVisibleMemorySize", "/format:csv")
	output, err = cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		if len(lines) >= 2 {
			parts := strings.Split(lines[1], ",")
			if len(parts) >= 2 {
				memKBStr := strings.TrimSpace(parts[1])
				if memKB, parseErr := strconv.ParseUint(memKBStr, 10, 64); parseErr == nil {
					return int(memKB / (1024 * 1024))
				}
			}
		}
	}

	return 0
}

// getTotalDiskGB retrieves total disk space in GB
func getTotalDiskGB() int {
	// Try wmic
	cmd := exec.Command("wmic", "logicaldisk", "where", "drivetype=3", "get", "size", "/format:csv")
	output, err := cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		var totalBytes uint64
		for i := 1; i < len(lines); i++ {
			parts := strings.Split(lines[i], ",")
			if len(parts) >= 2 {
				sizeStr := strings.TrimSpace(parts[1])
				if sizeStr == "" {
					continue
				}
				size, parseErr := strconv.ParseUint(sizeStr, 10, 64)
				if parseErr == nil {
					totalBytes += size
				}
			}
		}
		if totalBytes > 0 {
			return int(totalBytes / (1024 * 1024 * 1024))
		}
	}

	return 0
}

// getOSVersion retrieves the operating system version string
func getOSVersion() string {
	// Try wmic
	cmd := exec.Command("wmic", "os", "get", "Caption,Version", "/format:csv")
	output, err := cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		if len(lines) >= 2 {
			parts := strings.Split(lines[1], ",")
			if len(parts) >= 3 {
				caption := strings.TrimSpace(parts[1])
				version := strings.TrimSpace(parts[2])
				if caption != "" {
					return fmt.Sprintf("%s %s", caption, version)
				}
			}
		}
	}

	return fmt.Sprintf("Windows (%s)", runtime.GOARCH)
}

// getMACAddress retrieves the MAC address of the primary network adapter
func getMACAddress() string {
	// Try getmac command
	cmd := exec.Command("getmac", "/fo", "csv", "/nh")
	output, err := cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		if len(lines) >= 1 {
			// Parse CSV: "MAC","Name","Transport Name"
			line := strings.TrimSpace(lines[0])
			parts := strings.Split(line, ",")
			if len(parts) >= 1 {
				mac := strings.Trim(parts[0], "\"")
				if mac != "" && mac != "N/A" {
					return mac
				}
			}
		}
	}

	// Fallback: wmic
	cmd = exec.Command("wmic", "nic", "where", "NetEnabled=true", "get", "MACAddress", "/format:csv")
	output, err = cmd.Output()
	if err == nil && len(output) > 0 {
		lines := strings.Split(strings.TrimSpace(string(output)), "\n")
		for i := 1; i < len(lines); i++ {
			parts := strings.Split(lines[i], ",")
			if len(parts) >= 2 {
				mac := strings.TrimSpace(parts[1])
				if mac != "" && mac != "N/A" {
					return mac
				}
			}
		}
	}

	return "00-00-00-00-00-00"
}

// determineConfigLevel determines the configuration level based on GPU and memory
func determineConfigLevel(gpuInfo string, memoryGB int) ConfigLevel {
	// Check for high-performance GPUs
	gpuUpper := strings.ToUpper(gpuInfo)

	highPerformanceGPUs := []string{
		"RTX 3060", "RTX 3060 TI", "RTX 3070", "RTX 3070 TI",
		"RTX 3080", "RTX 3080 TI", "RTX 3090", "RTX 3090 TI",
		"RTX 4060", "RTX 4060 TI", "RTX 4070", "RTX 4070 TI",
		"RTX 4080", "RTX 4080 TI", "RTX 4090",
		"RX 6700", "RX 6800", "RX 6900", "RX 7700", "RX 7800", "RX 7900",
	}

	for _, hpGPU := range highPerformanceGPUs {
		if strings.Contains(gpuUpper, strings.ToUpper(hpGPU)) {
			if memoryGB >= 16 {
				return ConfigLevelHighPerformance
			}
		}
	}

	// Check for mainstream GPUs
	mainstreamGPUs := []string{
		"GTX 1050", "GTX 1060", "GTX 1070", "GTX 1080",
		"GTX 1650", "GTX 1660",
		"RTX 2050", "RTX 2060", "RTX 2070", "RTX 2080",
		"RTX 3050",
		"RX 570", "RX 580", "RX 590",
		"RX 5500", "RX 5600", "RX 5700",
		"RX 6400", "RX 6500", "RX 6600",
		"INTEL ARC", "IRIS XE",
	}

	for _, msGPU := range mainstreamGPUs {
		if strings.Contains(gpuUpper, strings.ToUpper(msGPU)) {
			if memoryGB >= 8 {
				return ConfigLevelMainstream
			}
		}
	}

	// Fallback based on memory alone
	if memoryGB >= 16 {
		return ConfigLevelMainstream
	}

	return ConfigLevelEntry
}
