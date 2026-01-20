package main

//#include "bridge.h"
import "C"

import (
	"time"
	"unsafe"

	"cfa/native/app"
	"cfa/native/tunnel"
	"github.com/metacubex/mihomo/tunnel/statistic"
)

//export queryTunnelState
func queryTunnelState() *C.char {
	mode := tunnel.QueryMode()

	response := &struct {
		Mode string `json:"mode"`
	}{mode}

	return marshalJson(response)
}

//export queryNow
func queryNow(upload, download *C.uint64_t) {
	up, down := tunnel.Now()

	*upload = C.uint64_t(up)
	*download = C.uint64_t(down)
}

//export queryTotal
func queryTotal(upload, download *C.uint64_t) {
	up, down := tunnel.Total()

	*upload = C.uint64_t(up)
	*download = C.uint64_t(down)
}

//export queryGroupNames
func queryGroupNames(excludeNotSelectable C.int) *C.char {
	return marshalJson(tunnel.QueryProxyGroupNames(excludeNotSelectable != 0))
}

//export queryGroup
func queryGroup(name C.c_string, sortMode C.c_string) *C.char {
	n := C.GoString(name)
	s := C.GoString(sortMode)

	mode := tunnel.Default

	switch s {
	case "Title":
		mode = tunnel.Title
	case "Delay":
		mode = tunnel.Delay
	}

	response := tunnel.QueryProxyGroup(n, mode, app.SubtitlePattern())

	if response == nil {
		return nil
	}

	return marshalJson(response)
}

//export healthCheck
func healthCheck(completable unsafe.Pointer, name C.c_string) {
	go func(name string) {
		tunnel.HealthCheck(name)

		C.complete(completable, nil)
	}(C.GoString(name))
}

//export healthCheckAll
func healthCheckAll() {
	tunnel.HealthCheckAll()
}

//export patchSelector
func patchSelector(selector, name C.c_string) C.int {
	s := C.GoString(selector)
	n := C.GoString(name)

	if tunnel.PatchSelector(s, n) {
		return 1
	}

	return 0
}

//export patchForceSelector
func patchForceSelector(selector, name C.c_string) C.int {
	s := C.GoString(selector)
	n := C.GoString(name)
	if tunnel.PatchForceSelector(s, n) {
		return 1
	}
	return 0
}

//export queryProviders
func queryProviders() *C.char {
	return marshalJson(tunnel.QueryProviders())
}

//export updateProvider
func updateProvider(completable unsafe.Pointer, pType C.c_string, name C.c_string) {
	go func(pType, name string) {
		C.complete(completable, marshalString(tunnel.UpdateProvider(pType, name)))

		C.release_object(completable)
	}(C.GoString(pType), C.GoString(name))
}

//export suspend
func suspend(suspended C.int) {
	tunnel.Suspend(suspended != 0)
}

//export queryConnections
func queryConnections() *C.char {
	return marshalJson(statistic.DefaultManager.Snapshot())
}

//export subscribeConnections
func subscribeConnections(remote unsafe.Pointer) {
	go func(remote unsafe.Pointer) {
		ch := make(chan struct{}, 256)
		prev := statistic.DefaultRequestNotify
		statistic.DefaultRequestNotify = func(c statistic.Tracker) {
			select {
			case ch <- struct{}{}:
			default:
			}
		}
		go func() {
			defer func() {
				statistic.DefaultRequestNotify = prev
			}()
			ticker := time.NewTicker(1 * time.Second)
			defer ticker.Stop()
			const aggWindow = 200 * time.Millisecond
			const minInterval = 500 * time.Millisecond
			var lastSent time.Time
			sendSnapshot := func() bool {
				now := time.Now()
				if !lastSent.IsZero() {
					d := now.Sub(lastSent)
					if d < minInterval {
						time.Sleep(minInterval - d)
					}
				}
				snap := statistic.DefaultManager.Snapshot()
				if C.connection_received(remote, marshalJson(snap)) != 0 {
					C.release_object(remote)
					statistic.DefaultRequestNotify = prev
					return false // stop loop
				}
				lastSent = time.Now()
				return true // continue loop
			}
			if !sendSnapshot() {
				return
			}
			for {
				select {
				case <-ticker.C:
					if !sendSnapshot() {
						return
					}
				case <-ch:
					timer := time.NewTimer(aggWindow)
				loop:
					for {
						select {
						case <-ch:
						case <-ticker.C:
						case <-timer.C:
							break loop
						}
					}
					if !sendSnapshot() {
						return
					}
				}
			}
		}()
		// keep outer goroutine alive while remote is valid; it can exit if connection_received returns non-zero
	}(remote)
}

//export closeConnection
func closeConnection(id C.c_string) C.int {
	i := C.GoString(id)
	c := statistic.DefaultManager.Get(i)
	if c != nil {
		_ = c.Close()
		return 1
	}
	return 0
}

//export closeAllConnections
func closeAllConnections() C.int {
	tunnel.CloseAllConnections()
	return 1
}
