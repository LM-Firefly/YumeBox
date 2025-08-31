package config

import (
	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/hub/executor"
)

func applyConfigWithoutAutoDownloadUI(cfg *config.Config) {
	restore := executor.DisableAutoDownloadUI()
	defer restore()

	hub.ApplyConfig(cfg)
}
