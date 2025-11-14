package app

import "time"

var appVersionName string
var platformVersion int

func ApplyVersionName(versionName string) {
	appVersionName = versionName
}

func ApplyPlatformVersion(version int) {
	platformVersion = version
}

func VersionName() string {
	return appVersionName
}

func PlatformVersion() int {
	return platformVersion
}

func NotifyTimeZoneChanged(name string, offset int) {
	time.Local = time.FixedZone(name, offset)
}
