package config

import (
	"encoding/json"
	"strings"

	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/log"
)

type rawRoutingOverride struct {
	RuleProvider *map[string]map[string]any `json:"rule-providers"`
	ProxyGroup   *[]map[string]any          `json:"proxy-groups"`
	Rules        *[]string                  `json:"rules"`
	PrependRules *[]string                  `json:"prepend-rules"`
	SubRules     *map[string][]string       `json:"sub-rules"`
}

func patchAdvancedRouting(cfg *config.RawConfig, _ string) error {
	persist := loadRoutingOverride(OverrideSlotPersist)
	session := loadRoutingOverride(OverrideSlotSession)

	mergeRuleProviders(cfg, persist.RuleProvider)
	mergeRuleProviders(cfg, session.RuleProvider)
	applyProxyGroups(cfg, persist.ProxyGroup, session.ProxyGroup)
	applyRules(cfg, persist, session)
	mergeSubRules(cfg, persist.SubRules)
	mergeSubRules(cfg, session.SubRules)

	return nil
}

func loadRoutingOverride(slot OverrideSlot) rawRoutingOverride {
	content := strings.TrimSpace(ReadOverride(slot))
	if content == "" {
		return rawRoutingOverride{}
	}

	var override rawRoutingOverride
	if err := json.Unmarshal([]byte(content), &override); err != nil {
		log.Warnln("Parse routing override failed (slot=%d): %s", slot, err.Error())
		return rawRoutingOverride{}
	}

	return override
}

func mergeRuleProviders(cfg *config.RawConfig, providerMap *map[string]map[string]any) {
	if providerMap == nil {
		return
	}

	if cfg.RuleProvider == nil {
		cfg.RuleProvider = make(map[string]map[string]any)
	}

	for name, provider := range *providerMap {
		cfg.RuleProvider[name] = cloneAnyMap(provider)
	}
}

func applyProxyGroups(cfg *config.RawConfig, persistGroups *[]map[string]any, sessionGroups *[]map[string]any) {
	if sessionGroups != nil {
		cfg.ProxyGroup = cloneAnyMapSlice(*sessionGroups)
		return
	}

	if persistGroups != nil {
		cfg.ProxyGroup = cloneAnyMapSlice(*persistGroups)
	}
}

func applyRules(cfg *config.RawConfig, persist rawRoutingOverride, session rawRoutingOverride) {
	if session.Rules != nil {
		cfg.Rule = cloneStringSlice(*session.Rules)
		return
	}

	if persist.Rules != nil {
		cfg.Rule = cloneStringSlice(*persist.Rules)
		return
	}

	var prependRules *[]string
	if session.PrependRules != nil {
		prependRules = session.PrependRules
	} else if persist.PrependRules != nil {
		prependRules = persist.PrependRules
	}

	if prependRules == nil || len(*prependRules) == 0 {
		return
	}

	cfg.Rule = append(cloneStringSlice(*prependRules), cloneStringSlice(cfg.Rule)...)
}

func mergeSubRules(cfg *config.RawConfig, subRules *map[string][]string) {
	if subRules == nil {
		return
	}

	if cfg.SubRules == nil {
		cfg.SubRules = make(map[string][]string)
	}

	for name, rules := range *subRules {
		cfg.SubRules[name] = cloneStringSlice(rules)
	}
}

func cloneAnyMap(src map[string]any) map[string]any {
	if src == nil {
		return nil
	}

	dst := make(map[string]any, len(src))
	for k, v := range src {
		dst[k] = v
	}

	return dst
}

func cloneAnyMapSlice(src []map[string]any) []map[string]any {
	if src == nil {
		return nil
	}

	dst := make([]map[string]any, 0, len(src))
	for _, item := range src {
		dst = append(dst, cloneAnyMap(item))
	}

	return dst
}

func cloneStringSlice(src []string) []string {
	if src == nil {
		return nil
	}

	dst := make([]string, len(src))
	copy(dst, src)
	return dst
}
