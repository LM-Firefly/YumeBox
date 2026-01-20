package tunnel

import (
	"encoding/json"
	"sort"
	"strings"

	"github.com/dlclark/regexp2"

	"github.com/metacubex/mihomo/adapter/outboundgroup"
	C "github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/constant/provider"
	"github.com/metacubex/mihomo/log"
	"github.com/metacubex/mihomo/tunnel"
)

type SortMode int

const (
	Default SortMode = iota
	Title
	Delay
)

type Proxy struct {
	Name     string `json:"name"`
	Title    string `json:"title"`
	Subtitle string `json:"subtitle"`
	Type     string `json:"type"`
	Delay    int    `json:"delay"`
}

type ProxyGroup struct {
	Type    string   `json:"type"`
	Now     string   `json:"now"`
	Icon    string   `json:"icon,omitempty"`
	Fixed   string   `json:"fixed"`
	Proxies []*Proxy `json:"proxies"`
}

type sortableProxyList struct {
	list []*Proxy
	less func(a, b *Proxy) bool
}

func (s *sortableProxyList) Len() int {
	return len(s.list)
}

func (s *sortableProxyList) Less(i, j int) bool {
	return s.less(s.list[i], s.list[j])
}

func (s *sortableProxyList) Swap(i, j int) {
	s.list[i], s.list[j] = s.list[j], s.list[i]
}

func QueryProxyGroupNames(excludeNotSelectable bool) []string {
	mode := tunnel.Mode()

	if mode == tunnel.Direct {
		return []string{}
	}

	globalAdapter := tunnel.Proxies()["GLOBAL"].Adapter()
	if global, ok := globalAdapter.(interface{ Proxies() []C.Proxy }); ok {
		proxies := global.Proxies()
		result := make([]string, 0, len(proxies)+1)
		if mode == tunnel.Global {
			result = append(result, "GLOBAL")
		}
		for _, p := range proxies {
			if _, ok := p.Adapter().(interface{ Proxies() []C.Proxy }); ok {
				if !excludeNotSelectable || p.Type() == C.Selector {
					result = append(result, p.Name())
				}
			}
		}
		return result
	}
	return []string{}
}

func QueryProxyGroup(name string, sortMode SortMode, uiSubtitlePattern *regexp2.Regexp) *ProxyGroup {
	p := tunnel.Proxies()[name]

	if p == nil {
		log.Warnln("Query group `%s`: not found", name)

		return nil
	}

	// 检查是否为代理组类型
	adapter := p.Adapter()
	if _, ok := adapter.(interface{ Proxies() []C.Proxy }); !ok {
		log.Warnln("Query group `%s`: invalid type %s", name, p.Type().String())

		return nil
	}

	// 使用类型断言获取代理组的代理列表和当前选择
	var proxiesList []C.Proxy
	var now string
	var fixed string
	if group, ok := adapter.(interface {
		Proxies() []C.Proxy
		Now() string
	}); ok {
		proxiesList = group.Proxies()
		now = group.Now()
		if marshaler, ok := adapter.(json.Marshaler); ok {
			if data, err := marshaler.MarshalJSON(); err == nil {
				var mapData map[string]interface{}
				if err := json.Unmarshal(data, &mapData); err == nil {
					if v, ok := mapData["fixed"]; ok {
						if s, ok := v.(string); ok {
							fixed = s
						}
					}
				}
			}
		}
	} else {
		log.Warnln("Query group `%s`: unable to get proxies or now", name)
		return nil
	}

	proxies := convertProxies(proxiesList, uiSubtitlePattern)
	// 	proxies := collectProviders(g.Providers(), uiSubtitlePattern)

	switch sortMode {
	case Title:
		wrapper := &sortableProxyList{
			list: proxies,
			less: func(a, b *Proxy) bool {
				return strings.Compare(a.Title, b.Title) < 0
			},
		}
		sort.Sort(wrapper)
	case Delay:
		wrapper := &sortableProxyList{
			list: proxies,
			less: func(a, b *Proxy) bool {
				return a.Delay < b.Delay
			},
		}
		sort.Sort(wrapper)
	case Default:
	default:
	}

	return &ProxyGroup{
		Type:    p.Type().String(),
		Now:     now,
		Icon:    proxyGroupIcon(adapter),
		Fixed:   fixed,
		Proxies: proxies,
	}
}

func proxyGroupIcon(group interface{}) string {
	switch g := group.(type) {
	case *outboundgroup.Selector:
		return g.Icon
	case *outboundgroup.URLTest:
		return g.Icon
	case *outboundgroup.LoadBalance:
		return g.Icon
	case *outboundgroup.Fallback:
		return g.Icon
	default:
		return ""
	}
}

func PatchSelector(selector, name string) bool {
	p := tunnel.Proxies()[selector]

	if p == nil {
		log.Warnln("Patch selector `%s`: not found", selector)
		return false
	}

	// 获取适配器并检查是否为代理组类型
	adapter := p.Adapter()
	if _, ok := adapter.(interface{ Proxies() []C.Proxy }); !ok {
		log.Warnln("Patch selector `%s`: not a proxy group type: %s", selector, p.Type().String())
		return false
	}

	// 尝试获取 Set 方法
	s, ok := adapter.(interface{ Set(string) error })
	if !ok {
		log.Warnln("Patch selector `%s`: does not support Set operation", selector)
		return false
	}

	// 执行设置操作
	if err := s.Set(name); err != nil {
		log.Warnln("Patch selector `%s`: failed to set proxy - %v", selector, err)
		return false
	}

	log.Infoln("Patch selector `%s`: successfully set to %s", selector, name)
	return true
}

// PatchForceSelector forces a selection (pin/fixed) for compatible groups
func PatchForceSelector(selector, name string) bool {
	p := tunnel.Proxies()[selector]
	if p == nil {
		log.Warnln("Force patch selector `%s`: not found", selector)
		return false
	}
	// 检查是否为代理组类型
	adapter := p.Adapter()
	if _, ok := adapter.(interface{ Proxies() []C.Proxy }); !ok {
		log.Warnln("Force patch selector `%s`: invalid type %s", selector, p.Type().String())
		return false
	}
	s, ok := adapter.(interface{ ForceSet(string) })
	if !ok {
		log.Warnln("Force patch selector `%s`: not supported", selector)
		return false
	}
	s.ForceSet(name)
	log.Infoln("Force patch selector `%s` -> %s", selector, name)
	return true
}

func convertProxies(proxies []C.Proxy, uiSubtitlePattern *regexp2.Regexp) []*Proxy {
	result := make([]*Proxy, 0, 128)

	for _, p := range proxies {
		name := p.Name()
		title := name
		subtitle := p.Type().String()

		if uiSubtitlePattern != nil {
			adapter := p.Adapter()
			if _, ok := adapter.(interface{ Proxies() []C.Proxy }); !ok {
				runes := []rune(name)
				match, err := uiSubtitlePattern.FindRunesMatch(runes)
				if err == nil && match != nil {
					title = string(runes[:match.Index]) + string(runes[match.Index+match.Length:])
					subtitle = string(runes[match.Index : match.Index+match.Length])
				}
			}
		}
		testURL := "https://www.gstatic.com/generate_204"
		for k := range p.ExtraDelayHistories() {
			if len(k) > 0 {
				testURL = k
				break
			}
		}

		ld := p.LastDelayForTestUrl(testURL)
		var delay int
		if ld == 0xffff {
			delay = -1 // treat as TIMEOUT
		} else {
			delay = int(ld)
		}
		result = append(result, &Proxy{
			Name:     name,
			Title:    strings.TrimSpace(title),
			Subtitle: strings.TrimSpace(subtitle),
			Type:     p.Type().String(),
			Delay:    delay,
		})
	}
	return result
}

func collectProviders(providers []provider.ProxyProvider, uiSubtitlePattern *regexp2.Regexp) []*Proxy {
	result := make([]*Proxy, 0, 128)

	for _, p := range providers {
		for _, px := range p.Proxies() {
			name := px.Name()
			title := name
			subtitle := px.Type().String()

			if uiSubtitlePattern != nil {
				adapter := px.Adapter()
				if _, ok := adapter.(interface{ Proxies() []C.Proxy }); !ok {
					runes := []rune(name)
					match, err := uiSubtitlePattern.FindRunesMatch(runes)
					if err == nil && match != nil {
						title = string(runes[:match.Index]) + string(runes[match.Index+match.Length:])
						subtitle = string(runes[match.Index : match.Index+match.Length])
					}
				}
			}

			testURL := "https://www.gstatic.com/generate_204"
			for k := range px.ExtraDelayHistories() {
				if len(k) > 0 {
					testURL = k
					break
				}
			}

			ld := px.LastDelayForTestUrl(testURL)
			var delay int
			if ld == 0xffff {
				delay = -1 // treat as TIMEOUT
			} else {
				delay = int(ld)
			}

			result = append(result, &Proxy{
				Name:     name,
				Title:    strings.TrimSpace(title),
				Subtitle: strings.TrimSpace(subtitle),
				Type:     px.Type().String(),
				Delay:    delay,
			})
		}
	}

	return result
}

// QueryProviderNames query provider names
func QueryProviderNames() []string {
	var names []string
	for name := range tunnel.Providers() {
		names = append(names, name)
	}
	return names
}
