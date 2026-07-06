package service

import "testing"

func TestGetLaunchOptionsUseConfiguredChromiumPath(t *testing.T) {
	t.Setenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH", "/custom/chromium")

	options := getLaunchOptions()

	if options.ExecutablePath == nil || *options.ExecutablePath != "/custom/chromium" {
		t.Fatalf("ExecutablePath = %v, want /custom/chromium", options.ExecutablePath)
	}
}

func TestGetLaunchOptionsUsePlaywrightManagedChromiumByDefault(t *testing.T) {
	t.Setenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH", "")

	options := getLaunchOptions()

	if options.ExecutablePath != nil {
		t.Fatalf("ExecutablePath = %q, want nil for Playwright-managed browser", *options.ExecutablePath)
	}
}

func TestGetLaunchOptionsKeepContainerFlags(t *testing.T) {
	options := getLaunchOptions()

	wantArgs := []string{"--no-sandbox", "--disable-dev-shm-usage"}
	if len(options.Args) != len(wantArgs) {
		t.Fatalf("Args = %v, want %v", options.Args, wantArgs)
	}
	for i, want := range wantArgs {
		if options.Args[i] != want {
			t.Fatalf("Args[%d] = %q, want %q", i, options.Args[i], want)
		}
	}
}
