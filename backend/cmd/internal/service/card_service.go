package service

import (
	"fmt"
	"github.com/playwright-community/playwright-go"
	"math/rand/v2"
	"os"
	"sort"
	"strings"
)

var (
	levelCardViewport = &playwright.Size{Width: 934, Height: 282}
	pw                *playwright.Playwright
	chromium          playwright.Browser
)

const (
	MaxLevelRoleLength = 100

	LevelCardPath      = "./static/templates/levels/cards/template.html"
	LevelsRolesPath    = "./static/templates/levels/roles/template.html"
	LevelsRolesRowPath = "./static/templates/levels/roles/role_row.html"
)

func InitializePlaywrightService(playwright *playwright.Playwright) {
	pw = playwright
	browser, err := pw.Chromium.Launch(getLaunchOptions())
	if err != nil {
		panic(err)
	}
	chromium = browser
}

var OnlineColors = map[string]*Color{
	"ONLINE":  {35, 165, 90},
	"IDLE":    {240, 178, 50},
	"DND":     {242, 63, 67},
	"OFFLINE": {128, 128, 128},
}

type LevelDataDTO struct {
	Username     string `json:"username"`
	AvatarUrl    string `json:"avatar_url"`
	Rank         int    `json:"rank"`
	Level        int    `json:"level"`
	Xp           int    `json:"xp"`
	XpNext       int    `json:"xp_next"`
	ThemeColor   int    `json:"theme_color"`
	OnlineStatus string `json:"online_status"`
	StatusColor  *Color
}

type LevelRoleDTO struct {
	Name  string `json:"name"`
	Color int    `json:"color"`
	Level int    `json:"level"`
}

type GuildDTO struct {
	Name    string `json:"name"`
	IconUrl string `json:"icon_url"`
}

type LevelsRolesData struct {
	LevelsRoles     []*LevelRoleDTO `json:"levels"`
	Guild           *GuildDTO       `json:"guild"`
	BackgroundColor int             `json:"background_color"`
}

func GenerateLevelCard(ld *LevelDataDTO) ([]byte, *APIError) {
	if err := checkLevelData(ld); err != nil {
		return nil, err
	}

	ld.StatusColor = OnlineColors[ld.OnlineStatus]
	html, err := getLevelCardTemplate(ld)
	if err != nil {
		return nil, err
	}

	page, perr := chromium.NewPage(getPageOptions(levelCardViewport))
	if perr != nil {
		fmt.Printf("Could not create page\n%s", perr)
		return nil, ErrorInternalServer
	}
	defer page.Close()

	if err := page.SetContent(html, getPageLoadOptions()); err != nil {
		fmt.Printf("Could not set page content\n%s", err)
		return nil, ErrorInternalServer
	}

	img, serr := page.Screenshot(getScreenshotOptions(false))
	if serr != nil {
		fmt.Printf("Could not take screenshot\n%s", serr)
		return nil, ErrorInternalServer
	}
	return img, nil
}

func GenerateLevelsRoles(lrd *LevelsRolesData) ([]byte, *APIError) {
	if err := checkLevelRoles(lrd); err != nil {
		return nil, err
	}

	html, err := getLevelsRolesTemplate(lrd)
	if err != nil {
		return nil, err
	}

	page, perr := chromium.NewPage()
	if perr != nil {
		fmt.Printf("Could not create page\n%s", perr)
		return nil, ErrorInternalServer
	}
	defer page.Close()

	if err := page.SetContent(html, getPageLoadOptions()); err != nil {
		fmt.Printf("Could not set page content\n%s", err)
		return nil, ErrorInternalServer
	}

	img, serr := page.Screenshot(getScreenshotOptions(true))
	if serr != nil {
		fmt.Printf("Could not take screenshot\n%s", serr)
		return nil, ErrorInternalServer
	}
	return img, nil
}

func getLevelsRolesTemplate(lrd *LevelsRolesData) (string, *APIError) {
	lrs := lrd.LevelsRoles
	bgColor := FromRGB(lrd.BackgroundColor)
	rowTemplate, err := os.ReadFile(LevelsRolesRowPath)
	if err != nil {
		fmt.Printf("Could not read levels roles row\n%s", err)
		return "", ErrorInternalServer
	}

	// Sorting array
	sort.Slice(lrd.LevelsRoles, func(i, j int) bool {
		return lrd.LevelsRoles[i].Level < lrd.LevelsRoles[j].Level
	})

	// Formatting each row of roles
	var htmlRows strings.Builder
	for _, lr := range lrs {
		color := FromRGB(lr.Color)
		html := string(rowTemplate)
		html = strings.ReplaceAll(html, "{{name}}", lr.Name)
		html = strings.ReplaceAll(html, "{{color}}", color.ToHtmlRGB())
		html = strings.ReplaceAll(html, "{{color-bg}}", color.ToHtmlRGBA(0.3))
		html = strings.ReplaceAll(html, "{{level}}", fmt.Sprintf("%02d", lr.Level))
		htmlRows.WriteString(html)
	}

	formatter := func(html string) string {
		html = strings.ReplaceAll(html, "'{{background.color}}'", bgColor.ToHtmlRGB())
		html = strings.ReplaceAll(html, "{{rows}}", htmlRows.String())
		html = strings.ReplaceAll(html, "{{guild.name}}", lrd.Guild.Name)
		html = strings.ReplaceAll(html, "{{guild.icon}}", lrd.Guild.IconUrl)
		return html
	}
	return getHTML(LevelsRolesPath, formatter)
}

func getLevelCardTemplate(ld *LevelDataDTO) (string, *APIError) {
	progress := float32(ld.Xp) * 100 / float32(ld.XpNext)
	themeColor := FromRGB(ld.ThemeColor).ToHtmlRGB()
	backImg := rand.IntN(9) + 1
	color := ld.StatusColor

	formatter := func(html string) string {
		html = strings.ReplaceAll(html, "'{{online.color}}'", color.ToHtmlRGB())
		html = strings.ReplaceAll(html, "'{{online.shadow}}'", color.ToHtmlRGBA(0.5))
		html = strings.ReplaceAll(html, "'{{progress.bar.percent}}'", fmt.Sprintf("%.2f%%", progress))
		html = strings.ReplaceAll(html, "'{{theme.color}}'", themeColor)
		html = strings.ReplaceAll(html, "{{username}}", ld.Username)
		html = strings.ReplaceAll(html, "{{avatar.url}}", ld.AvatarUrl)
		html = strings.ReplaceAll(html, "{{rank}}", fmt.Sprintf("%d", ld.Rank))
		html = strings.ReplaceAll(html, "{{level}}", fmt.Sprintf("%d", ld.Level))
		html = strings.ReplaceAll(html, "{{background.image}}", fmt.Sprintf("background-%d.png", backImg))
		html = strings.ReplaceAll(html, "{{xp.now}}", HumanizeNumber(ld.Xp))
		html = strings.ReplaceAll(html, "{{xp.next}}", HumanizeNumber(ld.XpNext))
		return html
	}
	return getHTML(LevelCardPath, formatter)
}

func getHTML(filePath string, formatter func(html string) string) (string, *APIError) {
	template, err := os.ReadFile(filePath)
	if err != nil {
		fmt.Printf("Could not read template file\n%s", err)
		return "", ErrorInternalServer
	}
	html := string(template)
	html = formatter(html)
	return html, nil
}
