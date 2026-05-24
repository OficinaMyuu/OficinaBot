package service

import (
	"fmt"
	"github.com/playwright-community/playwright-go"
)

const (
	Thousand = 1_000
	Million  = 1_000 * Thousand
	Billion  = 1_000 * Million

	MaxColorValue = 0xFFFFFF
)

type Color struct {
	Red   int
	Green int
	Blue  int
}

func (c *Color) ToHtmlRGB() string {
	return fmt.Sprintf("rgb(%d, %d, %d)", c.Red, c.Green, c.Blue)
}

func (c *Color) ToHtmlRGBA(alpha float32) string {
	return fmt.Sprintf("rgba(%d, %d, %d, %.2f)", c.Red, c.Green, c.Blue, alpha)
}

func FromRGB(rgb int) *Color {
	return &Color{
		Red:   (rgb >> 16) & 0xFF,
		Green: (rgb >> 8) & 0xFF,
		Blue:  rgb & 0xFF,
	}
}

func IsColorValid(rgb int) bool {
	return rgb >= 0 && rgb <= MaxColorValue
}

func HumanizeNumber(value int) string {
	num := float64(value)
	switch {
	case num >= Billion:
		return fmt.Sprintf("%.2fB", num/Billion)
	case num >= Million:
		return fmt.Sprintf("%.2fM", num/Million)
	case num >= Thousand:
		return fmt.Sprintf("%.2fK", num/Thousand)
	default:
		return fmt.Sprintf("%d", value)
	}
}

func getLaunchOptions() playwright.BrowserTypeLaunchOptions {
	return playwright.BrowserTypeLaunchOptions{
		Args: []string{"--headless"},
	}
}

func getPageOptions(viewport *playwright.Size) playwright.BrowserNewPageOptions {
	return playwright.BrowserNewPageOptions{
		Viewport: viewport,
	}
}

func getPageLoadOptions() playwright.PageSetContentOptions {
	return playwright.PageSetContentOptions{
		WaitUntil: playwright.WaitUntilStateLoad,
	}
}

func getScreenshotOptions(fullPage bool) playwright.PageScreenshotOptions {
	return playwright.PageScreenshotOptions{
		Type:     playwright.ScreenshotTypePng,
		FullPage: playwright.Bool(fullPage),
	}
}
