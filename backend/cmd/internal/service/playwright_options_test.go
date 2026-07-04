package service

import "testing"

func TestGetLaunchOptionsUseConfiguredChromiumPath(t *testing.T) {
	t.Setenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH", "/custom/chromium")

	options := getLaunchOptions()

	if options.ExecutablePath == nil || *options.ExecutablePath != "/custom/chromium" {
		t.Fatalf("ExecutablePath = %v, want /custom/chromium", options.ExecutablePath)
	}
}

func TestGetLaunchOptionsDefaultChromiumPath(t *testing.T) {
	t.Setenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH", "")

	options := getLaunchOptions()

	if options.ExecutablePath == nil || *options.ExecutablePath != defaultChromiumExecutablePath {
		t.Fatalf("ExecutablePath = %v, want %q", options.ExecutablePath, defaultChromiumExecutablePath)
	}
}
