package app

import "testing"

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
