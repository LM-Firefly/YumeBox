package config

import (
	"os"
	P "path"
	"runtime"
	"strings"
	"time"

	"gopkg.in/yaml.v3"

	"cfa/native/app"

	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/log"
)

func logDns(cfg *config.RawConfig) {
	bytes, err := yaml.Marshal(&cfg.DNS)
	if err != nil {
		log.Warnln("Marshal dns: %s", err.Error())

		return
	}

	log.Infoln("dns:")

	for _, line := range strings.Split(string(bytes), "\n") {
		log.Infoln("  %s", line)
	}
}

func UnmarshalAndPatch(profilePath string) (*config.RawConfig, error) {
	configPath := P.Join(profilePath, "config.yaml")

	configData, err := os.ReadFile(configPath)
	if err != nil {
		return nil, err
	}

	rawConfig, err := config.UnmarshalRawConfig(configData)
	if err != nil {
		return nil, err
	}

	if err := process(rawConfig, profilePath); err != nil {
		return nil, err
	}

	return rawConfig, nil
}

func Parse(rawConfig *config.RawConfig) (*config.Config, error) {
	cfg, err := config.ParseRawConfig(rawConfig)
	if err != nil {
		return nil, err
	}

	return cfg, nil
}

func Load(path string) error {
	start := time.Now()
	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		log.Errorln("Load %s: %s", path, err.Error())

		return err
	}
	log.Infoln("Unmarshal and patch done in %s", time.Since(start))

	logDns(rawCfg)

	parseStart := time.Now()
	cfg, err := Parse(rawCfg)
	if err != nil {
		log.Errorln("Load %s: %s", path, err.Error())

		return err
	}
	log.Infoln("Parse done in %s", time.Since(parseStart))

	applyStart := time.Now()
	// like hub.Parse()
	hub.ApplyConfig(cfg)
	log.Infoln("ApplyConfig done in %s", time.Since(applyStart))

	app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)

	runtime.GC()

	log.Infoln("Total config load time for %s: %s", path, time.Since(start))
	return nil
}

func LoadDefault() {
	cfg, err := config.Parse([]byte{})
	if err != nil {
		panic(err.Error())
	}

	hub.ApplyConfig(cfg)
}
