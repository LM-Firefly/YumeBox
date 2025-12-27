package tunnel

import (
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/metacubex/mihomo/constant/provider"
	"github.com/metacubex/mihomo/log"
	"github.com/metacubex/mihomo/tunnel"
)

var ErrInvalidType = errors.New("invalid type")

type Provider struct {
	Name             string            `json:"name"`
	VehicleType      string            `json:"vehicleType"`
	Type             string            `json:"type"`
	UpdatedAt        int64             `json:"updatedAt"`
	Path             string            `json:"path"`
	SubscriptionInfo *SubscriptionInfo `json:"subscriptionInfo,omitempty"`
}

type SubscriptionInfo struct {
	Upload   int64 `json:"Upload"`
	Download int64 `json:"Download"`
	Total    int64 `json:"Total"`
	Expire   int64 `json:"Expire"`
}

type UpdatableProvider interface {
	UpdatedAt() time.Time
}

type VehicleProvider interface {
	Vehicle() provider.Vehicle
}

func QueryProviders() []*Provider {
	r := tunnel.RuleProviders()
	p := tunnel.Providers()

	providers := make([]provider.Provider, 0, len(r)+len(p))

	for _, rule := range r {
		if rule.VehicleType() == provider.Compatible {
			continue
		}

		providers = append(providers, rule)
	}

	for _, proxy := range p {
		if proxy.VehicleType() == provider.Compatible {
			continue
		}

		providers = append(providers, proxy)
	}

	result := make([]*Provider, 0, len(providers))

	for _, p := range providers {
		updatedAt := time.Time{}
		path := ""

		if s, ok := p.(UpdatableProvider); ok {
			updatedAt = s.UpdatedAt()
		}

		if v, ok := p.(VehicleProvider); ok {
			path = v.Vehicle().Path()
		}

		item := &Provider{
			Name:        p.Name(),
			VehicleType: p.VehicleType().String(),
			Type:        p.Type().String(),
			UpdatedAt:   updatedAt.UnixNano() / 1000 / 1000,
			Path:        path,
		}
		if raw, err := json.Marshal(p); err == nil {
			var data struct {
				SubscriptionInfo *SubscriptionInfo `json:"subscriptionInfo,omitempty"`
			}
			if err := json.Unmarshal(raw, &data); err == nil {
				item.SubscriptionInfo = data.SubscriptionInfo
			}
		}
		result = append(result, item)
	}

	return result
}

func UpdateProvider(t string, name string) error {
	err := ErrInvalidType

	switch t {
	case "Rule":
		p := tunnel.RuleProviders()[name]
		if p == nil {
			return fmt.Errorf("%s not found", name)
		}

		err = p.Update()
	case "Proxy":
		p := tunnel.Providers()[name]
		if p == nil {
			return fmt.Errorf("%s not found", name)
		}

		err = p.Update()
	}

	if err != nil {
		log.Warnln("Updating provider %s: %s", name, err.Error())
	}

	return err
}
