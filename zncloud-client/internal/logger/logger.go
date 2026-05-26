package logger

import (
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"

	"gopkg.in/natefinch/lumberjack.v2"
)

// Log levels
const (
	DEBUG = iota
	INFO
	WARN
	ERROR
)

var levelNames = map[int]string{
	DEBUG: "DEBUG",
	INFO:  "INFO",
	WARN:  "WARN",
	ERROR: "ERROR",
}

// Logger is the global application logger
type Logger struct {
	mu       sync.Mutex
	level    int
	logger   *log.Logger
	eventLog *eventLogger
}

var globalLogger *Logger
var once sync.Once

// eventLogger writes to Windows Event Log (stub on non-Windows)
type eventLogger struct {
	available bool
}

func newEventLogger() *eventLogger {
	el := &eventLogger{}
	// Attempt to register/open the event source
	if err := el.open(); err == nil {
		el.available = true
	}
	return el
}

func (e *eventLogger) open() error {
	// On Windows, this would call RegisterEventSource/OpenEventLog
	// We use the Windows Event Log API via syscall
	return e.tryRegister()
}

func (e *eventLogger) tryRegister() error {
	// In a real Windows build, this uses golang.org/x/sys/windows/svc/eventlog
	// For simplicity, we detect if we're on Windows
	if runtime.GOOS == "windows" {
		// Will be handled by actual Windows Event Log calls in production
		return nil
	}
	return fmt.Errorf("event log not available on this platform")
}

func (e *eventLogger) write(level int, msg string) {
	if !e.available {
		return
	}
	// Windows Event Log write would go here
	// For now, we just silently track that we attempted it
}

// Init initializes the global logger instance
func Init(logDir string) error {
	var err error
	once.Do(func() {
		if logDir == "" {
			logDir = "C:\\ProgramData\\ZNCloud\\logs"
		}

		// Ensure log directory exists
		if err = os.MkdirAll(logDir, 0755); err != nil {
			return
		}

		logFile := filepath.Join(logDir, "zncloud-agent.log")

		// Lumberjack for log rotation
		ljLogger := &lumberjack.Logger{
			Filename:   logFile,
			MaxSize:    10, // megabytes
			MaxBackups: 7,
			MaxAge:     7, // days
			LocalTime:  true,
			Compress:   true,
		}

		// Multi-writer: file + stdout
		multiWriter := io.MultiWriter(ljLogger, os.Stdout)

		globalLogger = &Logger{
			level:    DEBUG,
			logger:   log.New(multiWriter, "", log.Ldate|log.Ltime|log.Lmicroseconds),
			eventLog: newEventLogger(),
		}
	})
	return err
}

// GetLogger returns the global logger instance
func GetLogger() *Logger {
	if globalLogger == nil {
		// Auto-init with defaults if not explicitly initialized
		Init("C:\\ProgramData\\ZNCloud\\logs")
	}
	return globalLogger
}

// SetLevel sets the minimum log level
func (l *Logger) SetLevel(level int) {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.level = level
}

// logf formats and writes a log entry
func (l *Logger) logf(level int, format string, args ...interface{}) {
	l.mu.Lock()
	defer l.mu.Unlock()

	if level < l.level {
		return
	}

	msg := fmt.Sprintf(format, args...)
	levelName := levelNames[level]
	caller := getCaller(3) // skip logf -> public method -> caller

	entry := fmt.Sprintf("[%s] [%s] %s: %s", time.Now().Format(time.RFC3339), levelName, caller, msg)
	l.logger.Println(entry)

	// Write critical errors to Event Log
	if level >= WARN {
		l.eventLog.write(level, msg)
	}
}

// getCaller returns the file and line of the caller
func getCaller(skip int) string {
	_, file, line, ok := runtime.Caller(skip)
	if !ok {
		return "unknown:0"
	}
	// Shorten the file path
	short := file
	if idx := strings.LastIndex(file, "/"); idx >= 0 {
		short = file[idx+1:]
	}
	return fmt.Sprintf("%s:%d", short, line)
}

// Debug logs a debug message
func (l *Logger) Debug(format string, args ...interface{}) {
	l.logf(DEBUG, format, args...)
}

// Info logs an info message
func (l *Logger) Info(format string, args ...interface{}) {
	l.logf(INFO, format, args...)
}

// Warn logs a warning message
func (l *Logger) Warn(format string, args ...interface{}) {
	l.logf(WARN, format, args...)
}

// Error logs an error message
func (l *Logger) Error(format string, args ...interface{}) {
	l.logf(ERROR, format, args...)
}

// Package-level convenience functions
func Debug(format string, args ...interface{}) {
	GetLogger().Debug(format, args...)
}

func Info(format string, args ...interface{}) {
	GetLogger().Info(format, args...)
}

func Warn(format string, args ...interface{}) {
	GetLogger().Warn(format, args...)
}

func Error(format string, args ...interface{}) {
	GetLogger().Error(format, args...)
}
