package config

import (
	"encoding/json"
	"reflect"
	"testing"

	mihomoConfig "github.com/metacubex/mihomo/config"
)

func TestConfigurationOverride_UnmarshalGeositeMatcher(t *testing.T) {
	var override ConfigurationOverride

	if err := json.Unmarshal([]byte(`{"geosite-matcher":"mph"}`), &override); err != nil {
		t.Fatalf("unexpected unmarshal error: %v", err)
	}

	if override.GeositeMatcher == nil {
		t.Fatal("expected geosite matcher to be populated")
	}

	if *override.GeositeMatcher != "mph" {
		t.Fatalf("unexpected geosite matcher: %s", *override.GeositeMatcher)
	}
}

func TestOverrideProcessor_ApplyOverrideUpdatesExternalControllerCors(t *testing.T) {
	overrideProcessor := &OverrideProcessor{}
	allowPrivateNetwork := true
	overrideConfig := &ConfigurationOverride{
		ExternalControllerCors: &ExternalControllerCors{
			AllowOriginsStart:   &[]string{"https://start.example"},
			AllowOrigins:        &[]string{"https://replace.example"},
			AllowOriginsEnd:     &[]string{"https://end.example"},
			AllowPrivateNetwork: &allowPrivateNetwork,
		},
	}
	rawConfig := &mihomoConfig.RawConfig{
		ExternalControllerCors: mihomoConfig.RawCors{
			AllowOrigins:        []string{"https://base.example"},
			AllowPrivateNetwork: false,
		},
	}

	overrideProcessor.applyOverride(rawConfig, overrideConfig)

	expectedOrigins := []string{
		"https://start.example",
		"https://replace.example",
		"https://end.example",
	}
	if !reflect.DeepEqual(rawConfig.ExternalControllerCors.AllowOrigins, expectedOrigins) {
		t.Fatalf("unexpected allow-origins: %#v", rawConfig.ExternalControllerCors.AllowOrigins)
	}

	if !rawConfig.ExternalControllerCors.AllowPrivateNetwork {
		t.Fatal("expected allow-private-network to be true")
	}
}

func TestOverrideProcessor_ApplyOverrideForcesExternalControllerCors(t *testing.T) {
	overrideProcessor := &OverrideProcessor{}
	forcedCors := &ExternalControllerCors{
		AllowOrigins: &[]string{"https://forced.example"},
	}
	overrideConfig := &ConfigurationOverride{
		ExternalControllerCorsForce: forcedCors,
	}
	rawConfig := &mihomoConfig.RawConfig{
		ExternalControllerCors: mihomoConfig.RawCors{
			AllowOrigins:        []string{"https://base.example"},
			AllowPrivateNetwork: true,
		},
	}

	overrideProcessor.applyOverride(rawConfig, overrideConfig)

	expectedOrigins := []string{"https://forced.example"}
	if !reflect.DeepEqual(rawConfig.ExternalControllerCors.AllowOrigins, expectedOrigins) {
		t.Fatalf("unexpected forced allow-origins: %#v", rawConfig.ExternalControllerCors.AllowOrigins)
	}

	if rawConfig.ExternalControllerCors.AllowPrivateNetwork {
		t.Fatal("expected force mode to reset allow-private-network")
	}
}

func TestMergeConfigurationOverride_PreservesForceFields(t *testing.T) {
	trueValue := true
	falseValue := false
	geoIPRange := "198.18.0.1/16"
	geoSiteURL := "https://example.com/geosite.dat"

	merged := mergeConfigurationOverride(
		&ConfigurationOverride{},
		&ConfigurationOverride{
			DNSForce: &DNSOverride{
				Enable:      &trueValue,
				FakeIPRange: &geoIPRange,
			},
			SnifferForce: &SnifferOverride{
				Enable: &trueValue,
				SniffForce: &SniffOverride{
					HTTP: &ProtocolOverride{
						OverrideDest: &trueValue,
					},
				},
			},
			GeoXUrlForce: &GeoXUrlOverride{
				GeoSite: &geoSiteURL,
			},
			ExternalControllerCorsForce: &ExternalControllerCors{
				AllowPrivateNetwork: &falseValue,
			},
		},
	)

	if merged.DNSForce == nil || merged.DNSForce.FakeIPRange == nil || *merged.DNSForce.FakeIPRange != geoIPRange {
		t.Fatal("expected dns-force to survive merge")
	}
	if merged.SnifferForce == nil || merged.SnifferForce.SniffForce == nil || merged.SnifferForce.SniffForce.HTTP == nil {
		t.Fatal("expected sniffer-force to survive merge")
	}
	if merged.GeoXUrlForce == nil || merged.GeoXUrlForce.GeoSite == nil || *merged.GeoXUrlForce.GeoSite != geoSiteURL {
		t.Fatal("expected geox-url-force to survive merge")
	}
	if merged.ExternalControllerCorsForce == nil || merged.ExternalControllerCorsForce.AllowPrivateNetwork == nil {
		t.Fatal("expected external-controller-cors-force to survive merge")
	}
}

func TestMergeConfigurationOverride_MergesExtendedFields(t *testing.T) {
	baseAllowOrigins := []string{"https://base.example"}
	incomingAllowOriginsStart := []string{"https://start.example"}
	incomingAllowOrigins := []string{"https://override.example"}
	trueValue := true
	appendSystemDNS := true
	storeSelected := true
	autoRoute := true
	mtu := uint32(1500)
	interfaceName := "wlan0"
	fingerprint := "chrome"
	ipv6Timeout := uint(120)
	cacheMaxSize := 4096

	merged := mergeConfigurationOverride(
		&ConfigurationOverride{
			ExternalControllerCors: &ExternalControllerCors{
				AllowOrigins: &baseAllowOrigins,
			},
			DNS:     &DNSOverride{},
			App:     &AppOverride{},
			Profile: &ProfileOverride{},
			Tun:     &TunOverride{},
		},
		&ConfigurationOverride{
			ExternalControllerCors: &ExternalControllerCors{
				AllowOriginsStart:   &incomingAllowOriginsStart,
				AllowOrigins:        &incomingAllowOrigins,
				AllowPrivateNetwork: &trueValue,
			},
			InterfaceName:           &interfaceName,
			GlobalClientFingerprint: &fingerprint,
			DNS: &DNSOverride{
				IPv6Timeout:  &ipv6Timeout,
				CacheMaxSize: &cacheMaxSize,
			},
			App: &AppOverride{
				AppendSystemDns: &appendSystemDNS,
			},
			Profile: &ProfileOverride{
				StoreSelected: &storeSelected,
			},
			Tun: &TunOverride{
				AutoRoute: &autoRoute,
				MTU:       &mtu,
			},
		},
	)

	expectedOrigins := []string{"https://start.example", "https://override.example"}
	if merged.ExternalControllerCors == nil || !reflect.DeepEqual(*merged.ExternalControllerCors.AllowOrigins, expectedOrigins) {
		t.Fatalf("unexpected merged cors allow-origins: %#v", merged.ExternalControllerCors)
	}
	if merged.InterfaceName == nil || *merged.InterfaceName != interfaceName {
		t.Fatal("expected interface-name to be merged")
	}
	if merged.GlobalClientFingerprint == nil || *merged.GlobalClientFingerprint != fingerprint {
		t.Fatal("expected global-client-fingerprint to be merged")
	}
	if merged.DNS == nil || merged.DNS.IPv6Timeout == nil || *merged.DNS.IPv6Timeout != ipv6Timeout {
		t.Fatal("expected dns ipv6-timeout to be merged")
	}
	if merged.DNS.CacheMaxSize == nil || *merged.DNS.CacheMaxSize != cacheMaxSize {
		t.Fatal("expected dns cache-max-size to be merged")
	}
	if merged.App == nil || merged.App.AppendSystemDns == nil || *merged.App.AppendSystemDns != appendSystemDNS {
		t.Fatal("expected app override to be merged")
	}
	if merged.Profile == nil || merged.Profile.StoreSelected == nil || *merged.Profile.StoreSelected != storeSelected {
		t.Fatal("expected profile override to be merged")
	}
	if merged.Tun == nil || merged.Tun.AutoRoute == nil || *merged.Tun.AutoRoute != autoRoute || merged.Tun.MTU == nil || *merged.Tun.MTU != mtu {
		t.Fatal("expected tun override to be merged")
	}
}
