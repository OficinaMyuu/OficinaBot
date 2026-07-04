package app

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestPlaywrightRunOptionsUseConfiguredDriverPath(t *testing.T) {
	t.Setenv("PLAYWRIGHT_DRIVER_PATH", "/tmp/oficina-playwright")

	options := playwrightRunOptions()

	if options.DriverDirectory != "/tmp/oficina-playwright" {
		t.Fatalf("DriverDirectory = %q, want configured path", options.DriverDirectory)
	}
	if !options.SkipInstallBrowsers {
		t.Fatal("SkipInstallBrowsers = false, want true")
	}
}

func TestPlaywrightRunOptionsDefaultDriverPath(t *testing.T) {
	t.Setenv("PLAYWRIGHT_DRIVER_PATH", "")

	options := playwrightRunOptions()

	if options.DriverDirectory != defaultPlaywrightDriverPath {
		t.Fatalf("DriverDirectory = %q, want default path %q", options.DriverDirectory, defaultPlaywrightDriverPath)
	}
}

func TestEnsurePlaywrightDriverAcceptsRequiredFiles(t *testing.T) {
	driverDirectory := t.TempDir()
	writeRequiredDriverFile(t, driverDirectory, "node")
	writeRequiredDriverFile(t, driverDirectory, "package", "cli.js")

	if err := ensurePlaywrightDriver(driverDirectory); err != nil {
		t.Fatalf("ensurePlaywrightDriver() error = %v, want nil", err)
	}
}

func TestEnsurePlaywrightDriverRejectsMissingFile(t *testing.T) {
	driverDirectory := t.TempDir()
	writeRequiredDriverFile(t, driverDirectory, "node")

	err := ensurePlaywrightDriver(driverDirectory)
	if err == nil {
		t.Fatal("ensurePlaywrightDriver() error = nil, want missing file error")
	}
	if !strings.Contains(err.Error(), "package") || !strings.Contains(err.Error(), "cli.js") {
		t.Fatalf("ensurePlaywrightDriver() error = %q, want missing cli.js context", err)
	}
}

func writeRequiredDriverFile(t *testing.T, driverDirectory string, path ...string) {
	t.Helper()

	filePath := filepath.Join(append([]string{driverDirectory}, path...)...)
	if err := os.MkdirAll(filepath.Dir(filePath), 0o755); err != nil {
		t.Fatalf("MkdirAll() error = %v", err)
	}
	if err := os.WriteFile(filePath, []byte("test"), 0o644); err != nil {
		t.Fatalf("WriteFile() error = %v", err)
	}
}
