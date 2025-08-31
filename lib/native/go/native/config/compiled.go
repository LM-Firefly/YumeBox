package config

import (
	"os"
	"runtime"
	"strings"

	"cfa/native/app"

	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/log"
	"gopkg.in/yaml.v3"
)

func LoadCompiled(path string) error {
	configData, err := os.ReadFile(path)
	if err != nil {
		log.Errorln("Load compiled %s: %s", path, err.Error())
		return err
	}

	rawCfg, err := config.UnmarshalRawConfig(configData)
	if err != nil {
		log.Errorln("Load compiled %s: %s", path, err.Error())
		return err
	}

	cfg, err := config.Parse(configData)
	if err != nil {
		log.Errorln("Load compiled %s: %s", path, err.Error())
		return err
	}

	applyConfigWithoutAutoDownloadUI(cfg)
	app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)
	runtime.GC()
	return nil
}

func QueryProxyGroupsFromCompiledYaml(yamlText string, profileDir string, excludeNotSelectable bool) ([]*ProxyGroup, error) {
	_ = profileDir
	configData := []byte(yamlText)

	rawCfg, err := config.UnmarshalRawConfig(configData)
	if err != nil {
		return nil, err
	}

	cfg, err := config.Parse(configData)
	if err != nil {
		return nil, err
	}

	app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)

	groupNames := make([]string, 0, len(rawCfg.ProxyGroup))
	seen := make(map[string]struct{}, len(rawCfg.ProxyGroup))
	for _, mapping := range rawCfg.ProxyGroup {
		name, _ := mapping["name"].(string)
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}
		if _, ok := seen[name]; ok {
			continue
		}
		seen[name] = struct{}{}
		groupNames = append(groupNames, name)
	}

	return buildProxyGroupsFromParsed(cfg, groupNames, excludeNotSelectable), nil
}

func QueryConfigFromCompiledYaml(yamlText string) (map[string]any, error) {
	var root map[string]any
	if err := yaml.Unmarshal([]byte(yamlText), &root); err != nil {
		return nil, err
	}
	return root, nil
}
