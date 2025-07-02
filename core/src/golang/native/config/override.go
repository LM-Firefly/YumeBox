package config

import (
	"io"
	"os"
	"sync"

	"github.com/metacubex/mihomo/constant"
)

type OverrideSlot int

const (
	OverrideSlotPersist OverrideSlot = iota
	OverrideSlotSession
)

const defaultPersistOverride = `{}`
const defaultSessionOverride = `{}`

var (
	sessionOverride     = defaultSessionOverride
	sessionOverrideMu   sync.RWMutex
)

func overridePersistPath() string {
	return constant.Path.Resolve("override.json")
}

func ReadOverride(slot OverrideSlot) string {
	switch slot {
	case OverrideSlotPersist:
		file, err := os.OpenFile(overridePersistPath(), os.O_RDONLY, 0600)
		if err != nil {
			return defaultPersistOverride
		}
		defer file.Close()

		buf, err := io.ReadAll(file)
		if err != nil {
			return defaultPersistOverride
		}

		return string(buf)
	case OverrideSlotSession:
		sessionOverrideMu.RLock()
		defer sessionOverrideMu.RUnlock()
		return sessionOverride
	}

	return ""
}

func WriteOverride(slot OverrideSlot, content string) {
	switch slot {
	case OverrideSlotPersist:
		file, err := os.OpenFile(overridePersistPath(), os.O_WRONLY|os.O_TRUNC|os.O_CREATE, 0600)
		if err != nil {
			return
		}
		defer file.Close()

		_, _ = file.Write([]byte(content))
	case OverrideSlotSession:
		sessionOverrideMu.Lock()
		sessionOverride = content
		sessionOverrideMu.Unlock()
	}
}

func ClearOverride(slot OverrideSlot) {
	switch slot {
	case OverrideSlotPersist:
		_ = os.Remove(overridePersistPath())
	case OverrideSlotSession:
		sessionOverrideMu.Lock()
		sessionOverride = defaultSessionOverride
		sessionOverrideMu.Unlock()
	}
}
