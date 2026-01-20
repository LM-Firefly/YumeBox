/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumelira.yumebox.core.util.Parcelizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ConfigurationOverride(

    @SerialName("port")
    var httpPort: Int? = null,

    @SerialName("socks-port")
    var socksPort: Int? = null,

    @SerialName("redir-port")
    var redirectPort: Int? = null,

    @SerialName("tproxy-port")
    var tproxyPort: Int? = null,

    @SerialName("mixed-port")
    var mixedPort: Int? = null,

    @SerialName("authentication")
    var authentication: List<String>? = null,

    @SerialName("authentication-start")
    var authenticationStart: List<String>? = null,

    @SerialName("authentication-end")
    var authenticationEnd: List<String>? = null,

    @SerialName("skip-auth-prefixes")
    var skipAuthPrefixes: List<String>? = null,

    @SerialName("skip-auth-prefixes-start")
    var skipAuthPrefixesStart: List<String>? = null,

    @SerialName("skip-auth-prefixes-end")
    var skipAuthPrefixesEnd: List<String>? = null,

    @SerialName("lan-allowed-ips")
    var lanAllowedIps: List<String>? = null,

    @SerialName("lan-allowed-ips-start")
    var lanAllowedIpsStart: List<String>? = null,

    @SerialName("lan-allowed-ips-end")
    var lanAllowedIpsEnd: List<String>? = null,

    @SerialName("lan-disallowed-ips")
    var lanDisallowedIps: List<String>? = null,

    @SerialName("lan-disallowed-ips-start")
    var lanDisallowedIpsStart: List<String>? = null,

    @SerialName("lan-disallowed-ips-end")
    var lanDisallowedIpsEnd: List<String>? = null,

    @SerialName("allow-lan")
    var allowLan: Boolean? = null,

    @SerialName("bind-address")
    var bindAddress: String? = null,

    @SerialName("mode")
    var mode: TunnelState.Mode? = null,

    @SerialName("log-level")
    var logLevel: LogMessage.Level? = null,

    @SerialName("ipv6")
    var ipv6: Boolean? = null,

    @SerialName("external-controller")
    var externalController: String? = null,

    @SerialName("external-controller-tls")
    var externalControllerTLS: String? = null,

    @SerialName("external-doh-server")
    var externalDohServer: String? = null,

    @SerialName("external-controller-cors")
    var externalControllerCors: ExternalControllerCors = ExternalControllerCors(),

    @SerialName("external-controller-cors-force")
    var externalControllerCorsForce: ExternalControllerCors? = null,

    @SerialName("secret")
    var secret: String? = null,

    @SerialName("hosts")
    var hosts: Map<String, String>? = null,

    @SerialName("hosts-merge")
    var hostsMerge: Map<String, String>? = null,

    @SerialName("unified-delay")
    var unifiedDelay: Boolean? = null,

    @SerialName("geodata-mode")
    var geodataMode: Boolean? = null,

    @SerialName("tcp-concurrent")
    var tcpConcurrent: Boolean? = null,

    @SerialName("find-process-mode")
    var findProcessMode: FindProcessMode? = null,

    @SerialName("keep-alive-interval")
    var keepAliveInterval: Int? = null,

    @SerialName("keep-alive-idle")
    var keepAliveIdle: Int? = null,

    @SerialName("interface-name")
    var interfaceName: String? = null,

    @SerialName("routing-mark")
    var routingMark: Int? = null,

    @SerialName("geosite-matcher")
    var geositeMatcher: String? = null,

    @SerialName("global-client-fingerprint")
    var globalClientFingerprint: String? = null,

    @SerialName("geo-auto-update")
    var geoAutoUpdate: Boolean? = null,

    @SerialName("geo-update-interval")
    var geoUpdateInterval: Int? = null,

    @SerialName("dns")
    val dns: Dns = Dns(),

    @SerialName("dns-force")
    var dnsForce: Dns? = null,

    @SerialName("clash-for-android")
    val app: App = App(),

    @SerialName("profile")
    val profile: Profile = Profile(),

    @SerialName("sniffer")
    val sniffer: Sniffer = Sniffer(),

    @SerialName("sniffer-force")
    var snifferForce: Sniffer? = null,

    @SerialName("geox-url")
    val geoxurl: GeoXUrl = GeoXUrl(),

    @SerialName("geox-url-force")
    var geoxurlForce: GeoXUrl? = null,
) : Parcelable {

    @Serializable
    data class Dns(

        @SerialName("enable")
        var enable: Boolean? = null,

        @SerialName("cache-algorithm")
        var cacheAlgorithm: String? = null,

        @SerialName("prefer-h3")
        var preferH3: Boolean? = null,

        @SerialName("listen")
        var listen: String? = null,

        @SerialName("ipv6")
        var ipv6: Boolean? = null,

        @SerialName("use-hosts")
        var useHosts: Boolean? = null,

        @SerialName("use-system-hosts")
        var useSystemHosts: Boolean? = null,

        @SerialName("respect-rules")
        var respectRules: Boolean? = null,

        @SerialName("enhanced-mode")
        var enhancedMode: DnsEnhancedMode? = null,

        @SerialName("fake-ip-range")
        var fakeIpRange: String? = null,

        @SerialName("fake-ip-range6")
        var fakeIpRange6: String? = null,

        @SerialName("fake-ip-filter-mode")
        var fakeIPFilterMode: FilterMode? = null,

        @SerialName("fake-ip-ttl")
        var fakeIpTtl: Int? = null,

        @SerialName("ipv6-timeout")
        var ipv6Timeout: Int? = null,

        @SerialName("cache-max-size")
        var cacheMaxSize: Int? = null,

        @SerialName("direct-nameserver-follow-policy")
        var directFollowPolicy: Boolean? = null,

        @SerialName("nameserver")
        var nameServer: List<String>? = null,

        @SerialName("nameserver-start")
        var nameServerStart: List<String>? = null,

        @SerialName("nameserver-end")
        var nameServerEnd: List<String>? = null,

        @SerialName("fallback")
        var fallback: List<String>? = null,

        @SerialName("fallback-start")
        var fallbackStart: List<String>? = null,

        @SerialName("fallback-end")
        var fallbackEnd: List<String>? = null,

        @SerialName("default-nameserver")
        var defaultServer: List<String>? = null,

        @SerialName("default-nameserver-start")
        var defaultServerStart: List<String>? = null,

        @SerialName("default-nameserver-end")
        var defaultServerEnd: List<String>? = null,

        @SerialName("fake-ip-filter")
        var fakeIpFilter: List<String>? = null,

        @SerialName("fake-ip-filter-start")
        var fakeIpFilterStart: List<String>? = null,

        @SerialName("fake-ip-filter-end")
        var fakeIpFilterEnd: List<String>? = null,

        @SerialName("proxy-server-nameserver")
        var proxyServerNameserver: List<String>? = null,

        @SerialName("proxy-server-nameserver-start")
        var proxyServerNameserverStart: List<String>? = null,

        @SerialName("proxy-server-nameserver-end")
        var proxyServerNameserverEnd: List<String>? = null,

        @SerialName("direct-nameserver")
        var directNameserver: List<String>? = null,

        @SerialName("direct-nameserver-start")
        var directNameserverStart: List<String>? = null,

        @SerialName("direct-nameserver-end")
        var directNameserverEnd: List<String>? = null,

        @SerialName("nameserver-policy")
        var nameserverPolicy: Map<String, String>? = null,

        @SerialName("nameserver-policy-merge")
        var nameserverPolicyMerge: Map<String, String>? = null,

        @SerialName("proxy-server-nameserver-policy")
        var proxyServerNameserverPolicy: Map<String, String>? = null,

        @SerialName("proxy-server-nameserver-policy-merge")
        var proxyServerNameserverPolicyMerge: Map<String, String>? = null,

        @SerialName("fallback-filter")
        val fallbackFilter: DnsFallbackFilter = DnsFallbackFilter(),

        @SerialName("fallback-filter-force")
        var fallbackFilterForce: DnsFallbackFilter? = null,
    )

    @Serializable
    data class DnsFallbackFilter(
        @SerialName("geoip")
        var geoIp: Boolean? = null,

        @SerialName("geoip-code")
        var geoIpCode: String? = null,

        @SerialName("ipcidr")
        var ipcidr: List<String>? = null,

        @SerialName("ipcidr-start")
        var ipcidrStart: List<String>? = null,

        @SerialName("ipcidr-end")
        var ipcidrEnd: List<String>? = null,

        @SerialName("geosite")
        var geosite: List<String>? = null,

        @SerialName("geosite-start")
        var geositeStart: List<String>? = null,

        @SerialName("geosite-end")
        var geositeEnd: List<String>? = null,

        @SerialName("domain")
        var domain: List<String>? = null,

        @SerialName("domain-start")
        var domainStart: List<String>? = null,

        @SerialName("domain-end")
        var domainEnd: List<String>? = null,
    )

    @Serializable
    data class App(
        @SerialName("append-system-dns")
        var appendSystemDns: Boolean? = null,
    )

    @Serializable
    data class Profile(
        @SerialName("store-selected")
        var storeSelected: Boolean? = null,

        @SerialName("store-fake-ip")
        var storeFakeIp: Boolean? = null,
    )

    @Serializable
    enum class FindProcessMode {
        @SerialName("off")
        Off,

        @SerialName("strict")
        Strict,

        @SerialName("always")
        Always,
    }

    @Serializable
    enum class DnsEnhancedMode {
        @SerialName("normal")
        None,

        @SerialName("redir-host")
        Mapping,

        @SerialName("fake-ip")
        FakeIp,
    }

    @Serializable
    enum class FilterMode {
        @SerialName("blacklist")
        BlackList,

        @SerialName("whitelist")
        WhiteList,

        @SerialName("rule")
        Rule,
    }

    @Serializable
    data class Sniffer(
        @SerialName("enable")
        var enable: Boolean? = null,

        @SerialName("sniff")
        var sniff: Sniff = Sniff(),

        @SerialName("sniff-force")
        var sniffForce: Sniff? = null,

        @SerialName("force-dns-mapping")
        var forceDnsMapping: Boolean? = null,

        @SerialName("parse-pure-ip")
        var parsePureIp: Boolean? = null,

        @SerialName("override-destination")
        var overrideDestination: Boolean? = null,

        @SerialName("force-domain")
        var forceDomain: List<String>? = null,

        @SerialName("force-domain-start")
        var forceDomainStart: List<String>? = null,

        @SerialName("force-domain-end")
        var forceDomainEnd: List<String>? = null,

        @SerialName("skip-domain")
        var skipDomain: List<String>? = null,

        @SerialName("skip-domain-start")
        var skipDomainStart: List<String>? = null,

        @SerialName("skip-domain-end")
        var skipDomainEnd: List<String>? = null,

        @SerialName("skip-src-address")
        var skipSrcAddress: List<String>? = null,

        @SerialName("skip-src-address-start")
        var skipSrcAddressStart: List<String>? = null,

        @SerialName("skip-src-address-end")
        var skipSrcAddressEnd: List<String>? = null,

        @SerialName("skip-dst-address")
        var skipDstAddress: List<String>? = null,

        @SerialName("skip-dst-address-start")
        var skipDstAddressStart: List<String>? = null,

        @SerialName("skip-dst-address-end")
        var skipDstAddressEnd: List<String>? = null,
    )

    @Serializable
    data class GeoXUrl(
        @SerialName("geoip")
        var geoip: String? = null,

        @SerialName("mmdb")
        var mmdb: String? = null,

        @SerialName("geosite")
        var geosite: String? = null,
    )

    @Serializable
    data class ExternalControllerCors(
        @SerialName("allow-origins")
        var allowOrigins: List<String>? = null,

        @SerialName("allow-origins-start")
        var allowOriginsStart: List<String>? = null,

        @SerialName("allow-origins-end")
        var allowOriginsEnd: List<String>? = null,

        @SerialName("allow-private-network")
        var allowPrivateNetwork: Boolean? = null,
    )

    @Serializable
    data class Sniff(
        @SerialName("HTTP")
        var http: ProtocolConfig = ProtocolConfig(),

        @SerialName("TLS")
        var tls: ProtocolConfig = ProtocolConfig(),

        @SerialName("QUIC")
        var quic: ProtocolConfig = ProtocolConfig(),
    )

    @Serializable
    data class ProtocolConfig(
        @SerialName("ports")
        var ports: List<String>? = null,

        @SerialName("ports-start")
        var portsStart: List<String>? = null,

        @SerialName("ports-end")
        var portsEnd: List<String>? = null,

        @SerialName("override-destination")
        var overrideDestination: Boolean? = null,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ConfigurationOverride> {
        override fun createFromParcel(parcel: Parcel): ConfigurationOverride {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<ConfigurationOverride?> {
            return arrayOfNulls(size)
        }
    }
}
