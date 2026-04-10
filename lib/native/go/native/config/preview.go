package config

import (
	"encoding/json"
	"strings"

	"github.com/dlclark/regexp2"

	"cfa/native/app"

	"github.com/metacubex/mihomo/adapter/outboundgroup"
	mihomoConfig "github.com/metacubex/mihomo/config"
	C "github.com/metacubex/mihomo/constant"
)

// Proxy 代理结构体（本地定义，避免依赖 tunnel 包）
type Proxy struct {
	Name     string `json:"name"`
	Title    string `json:"title"`
	Subtitle string `json:"subtitle"`
	Type     string `json:"type"`
	Delay    int    `json:"delay"`
}

// ProxyGroup 代理组结构体（本地定义，避免依赖 tunnel 包）
type ProxyGroup struct {
	Name    string   `json:"name"`
	Type    string   `json:"type"`
	Now     string   `json:"now"`
	Icon    string   `json:"icon"`
	Hidden  bool     `json:"hidden"`
	Fixed   string   `json:"fixed"`
	Proxies []*Proxy `json:"proxies"`
}

func buildProxyGroupsFromParsed(
	cfg *mihomoConfig.Config,
	orderedNames []string,
	excludeNotSelectable bool,
) []*ProxyGroup {
	if cfg == nil || len(orderedNames) == 0 {
		return []*ProxyGroup{}
	}

	result := make([]*ProxyGroup, 0, len(orderedNames))
	pattern := app.SubtitlePattern()

	for _, name := range orderedNames {
		proxy := cfg.Proxies[name]
		if proxy == nil {
			continue
		}
		if excludeNotSelectable && proxy.Type() != C.Selector {
			continue
		}

		group, ok := proxy.Adapter().(outboundgroup.ProxyGroup)
		if !ok {
			continue
		}

		fixed := ""
		if marshaler, ok := proxy.Adapter().(json.Marshaler); ok {
			if data, err := marshaler.MarshalJSON(); err == nil {
				var mapData map[string]any
				if err := json.Unmarshal(data, &mapData); err == nil {
					if value, ok := mapData["fixed"].(string); ok {
						fixed = value
					}
				}
			}
		}

		result = append(result, &ProxyGroup{
			Name:    name,
			Type:    proxy.Type().String(),
			Now:     group.Now(),
			Icon:    group.Icon(),
			Hidden:  group.Hidden(),
			Fixed:   fixed,
			Proxies: convertPreviewProxies(group.Proxies(), pattern),
		})
	}

	return result
}

func convertPreviewProxies(
	proxies []C.Proxy,
	uiSubtitlePattern *regexp2.Regexp,
) []*Proxy {
	result := make([]*Proxy, 0, len(proxies))
	for _, proxy := range proxies {
		result = append(result, buildPreviewProxy(proxy, uiSubtitlePattern))
	}
	return result
}

func buildPreviewProxy(
	proxy C.Proxy,
	uiSubtitlePattern *regexp2.Regexp,
) *Proxy {
	name := proxy.Name()
	title := name
	subtitle := proxy.Type().String()

	if uiSubtitlePattern != nil {
		if _, ok := proxy.Adapter().(outboundgroup.ProxyGroup); !ok {
			runes := []rune(name)
			match, err := uiSubtitlePattern.FindRunesMatch(runes)
			if err == nil && match != nil {
				title = string(runes[:match.Index]) + string(runes[match.Index+match.Length:])
				subtitle = string(runes[match.Index : match.Index+match.Length])
			}
		}
	}

	return &Proxy{
		Name:     name,
		Title:    strings.TrimSpace(title),
		Subtitle: strings.TrimSpace(subtitle),
		Type:     proxy.Type().String(),
		Delay:    int(proxy.LastDelayForTestUrl(getPreviewTestURL(proxy))),
	}
}

func getPreviewTestURL(proxy C.Proxy) string {
	for key := range proxy.ExtraDelayHistories() {
		if len(key) > 0 {
			return key
		}
	}
	return "https://www.gstatic.com/generate_204"
}
