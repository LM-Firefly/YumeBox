use jni::objects::{JObject, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use serde::{Deserialize, Serialize};
use serde_json::{Map as JsonMap, Value as JsonValue};
use serde_yaml::{Mapping as YamlMapping, Value as YamlValue};
use sha2::{Digest, Sha256};
use std::fs;
use std::path::Path;

const DEFAULT_NAME_SERVERS: &[&str] = &["223.5.5.5", "119.29.29.29", "8.8.4.4", "1.0.0.1"];
const DEFAULT_FAKE_IP_FILTER: &[&str] = &[
    "+.stun.*.*",
    "+.stun.*.*.*",
    "+.stun.*.*.*.*",
    "+.stun.*.*.*.*.*",
    "lens.l.google.com",
    "*.n.n.srv.nintendo.net",
    "+.stun.playstation.net",
    "xbox.*.*.microsoft.com",
    "*.*.xboxlive.com",
    "*.msftncsi.com",
    "*.msftconnecttest.com",
    "*.mcdn.bilivideo.cn",
    "WORKGROUP",
];
const DEFAULT_FAKE_IP_RANGE: &str = "28.0.0.0/8";

const ROOT_ORDER: &[&str] = &[
    "mode",
    "log-level",
    "ipv6",
    "find-process-mode",
    "keep-alive-interval",
    "keep-alive-idle",
    "unified-delay",
    "tcp-concurrent",
    "geodata-mode",
    "allow-lan",
    "bind-address",
    "lan-allowed-ips",
    "lan-disallowed-ips",
    "authentication",
    "skip-auth-prefixes",
    "external-controller",
    "external-controller-tls",
    "external-doh-server",
    "secret",
    "external-controller-cors",
    "profile",
    "interface-name",
    "routing-mark",
    "geosite-matcher",
    "global-client-fingerprint",
    "geo-auto-update",
    "geo-update-interval",
    "geox-url",
    "dns",
    "clash-for-android",
    "hosts",
    "sniffer",
    "port",
    "socks-port",
    "mixed-port",
    "redir-port",
    "tproxy-port",
    "tun",
    "proxies",
    "proxy-providers",
    "proxy-groups",
    "rules",
    "rule-providers",
    "sub-rules",
];

const DNS_ORDER: &[&str] = &[
    "enable",
    "cache-algorithm",
    "prefer-h3",
    "listen",
    "ipv6",
    "use-hosts",
    "use-system-hosts",
    "respect-rules",
    "enhanced-mode",
    "fake-ip-range",
    "fake-ip-range6",
    "fake-ip-filter-mode",
    "fake-ip-ttl",
    "ipv6-timeout",
    "cache-max-size",
    "direct-nameserver-follow-policy",
    "default-nameserver",
    "nameserver",
    "fallback",
    "proxy-server-nameserver",
    "direct-nameserver",
    "nameserver-policy",
    "proxy-server-nameserver-policy",
    "fake-ip-filter",
    "fallback-filter",
];

const DNS_FALLBACK_FILTER_ORDER: &[&str] = &["geoip", "geoip-code", "domain", "ipcidr", "geosite"];
const SNIFFER_ORDER: &[&str] = &[
    "enable",
    "force-dns-mapping",
    "parse-pure-ip",
    "override-destination",
    "sniff",
    "force-domain",
    "skip-domain",
    "skip-src-address",
    "skip-dst-address",
];
const SNIFF_ORDER: &[&str] = &["HTTP", "TLS", "QUIC"];
const PROTOCOL_ORDER: &[&str] = &["ports", "override-destination"];
const TUN_ORDER: &[&str] = &[
    "enable",
    "stack",
    "auto-route",
    "auto-redirect",
    "auto-detect-interface",
    "strict-route",
    "endpoint-independent-nat",
    "mtu",
    "gso",
    "gso-max-size",
    "disable-icmp-forwarding",
    "dns-hijack",
    "route-address",
    "route-exclude-address",
    "include-package",
    "exclude-package",
];
const EXTERNAL_CONTROLLER_CORS_ORDER: &[&str] = &["allow-origins", "allow-private-network"];
const PROFILE_ORDER: &[&str] = &["store-selected", "store-fake-ip"];
const GEOX_URL_ORDER: &[&str] = &["geoip", "mmdb", "geosite"];
const APP_ORDER: &[&str] = &["append-system-dns"];
const PROXY_ITEM_ORDER: &[&str] = &[
    "name",
    "type",
    "server",
    "port",
    "ip-version",
    "udp",
    "interface-name",
    "routing-mark",
    "tfo",
    "mptcp",
    "dialer-proxy",
];
const PROXY_GROUP_ITEM_ORDER: &[&str] = &[
    "name",
    "type",
    "proxies",
    "use",
    "url",
    "interval",
    "lazy",
    "timeout",
    "max-failed-times",
    "disable-udp",
    "interface-name",
    "routing-mark",
    "include-all",
    "include-all-proxies",
    "include-all-providers",
    "filter",
    "exclude-filter",
    "exclude-type",
    "expected-status",
    "hidden",
    "icon",
];
const PROVIDER_ITEM_ORDER: &[&str] = &[
    "type",
    "url",
    "path",
    "behavior",
    "interval",
    "format",
    "proxy",
    "filter",
    "exclude-filter",
    "exclude-type",
    "size-limit",
    "health-check",
];

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
struct CompileRequest {
    profile_uuid: String,
    profile_dir: String,
    profile_path: String,
    #[serde(default)]
    override_paths: Vec<String>,
    output_path: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct CompileResult {
    success: bool,
    fingerprint: String,
    final_yaml: String,
    warnings: Vec<String>,
    error: Option<String>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum PatchModifier {
    Replace,
    Start,
    End,
    Merge,
    Force,
}

#[derive(Clone, Copy, Debug)]
struct ParsedKey<'a> {
    base: &'a str,
    modifier: PatchModifier,
}

#[derive(Clone, Copy, Debug)]
enum SchemaId {
    Root,
    Dns,
    DnsFallbackFilter,
    Sniffer,
    Sniff,
    Protocol,
    Tun,
    ExternalControllerCors,
    Profile,
    GeoxUrl,
    App,
    ProxyItem,
    ProxyGroupItem,
    ProviderItem,
}

#[derive(Clone, Copy, Debug)]
enum ListStyle {
    Plain,
    NamedObjects,
}

#[derive(Clone, Copy, Debug)]
enum FieldBehavior {
    Scalar,
    List(ListStyle),
    Map,
    Object(SchemaId),
    Rules,
}

#[derive(Default)]
struct PatchOperations {
    replace: Option<JsonValue>,
    start: Option<JsonValue>,
    end: Option<JsonValue>,
    merge: Option<JsonValue>,
    force: Option<JsonValue>,
}

fn error_result(message: impl Into<String>) -> String {
    serde_json::to_string(&CompileResult {
        success: false,
        fingerprint: String::new(),
        final_yaml: String::new(),
        warnings: Vec::new(),
        error: Some(message.into()),
    })
    .unwrap_or_else(|_| "{\"success\":false,\"fingerprint\":\"\",\"finalYaml\":\"\",\"warnings\":[],\"error\":\"override processor failed\"}".to_string())
}

fn success_result(fingerprint: String, final_yaml: String) -> String {
    serde_json::to_string(&CompileResult {
        success: true,
        fingerprint,
        final_yaml,
        warnings: Vec::new(),
        error: None,
    })
    .unwrap_or_else(|_| "{\"success\":false,\"fingerprint\":\"\",\"finalYaml\":\"\",\"warnings\":[],\"error\":\"override result encode failed\"}".to_string())
}

fn compile_request(request_json: &str, write_output: bool) -> Result<CompileResult, String> {
    let request: CompileRequest = serde_json::from_str(request_json)
        .map_err(|err| format!("decode override request: {err}"))?;
    let source_yaml = fs::read_to_string(&request.profile_path)
        .map_err(|err| format!("read profile yaml: {err}"))?;

    if request.override_paths.is_empty() {
        let fingerprint = {
            let mut hasher = Sha256::new();
            hasher.update(request.profile_uuid.as_bytes());
            hasher.update(source_yaml.as_bytes());
            format!("{:x}", hasher.finalize())
        };

        if write_output {
            write_atomic(Path::new(&request.output_path), source_yaml.as_bytes())
                .map_err(|err| format!("write runtime yaml: {err}"))?;
        }

        return Ok(CompileResult {
            success: true,
            fingerprint,
            final_yaml: source_yaml,
            warnings: Vec::new(),
            error: None,
        });
    }

    let mut root: JsonValue =
        serde_yaml::from_str(&source_yaml).map_err(|err| format!("parse source yaml: {err}"))?;

    patch_static_runtime(&mut root, Path::new(&request.profile_dir));

    if let Some(override_patch) = load_combined_override_patch(&request.override_paths)? {
        apply_override_document(&mut root, &override_patch);
    }

    let final_yaml = serde_yaml::to_string(&normalize_root(&root))
        .map_err(|err| format!("encode final yaml: {err}"))?;
    let fingerprint = {
        let mut hasher = Sha256::new();
        hasher.update(request.profile_uuid.as_bytes());
        hasher.update(final_yaml.as_bytes());
        format!("{:x}", hasher.finalize())
    };

    if write_output {
        write_atomic(Path::new(&request.output_path), final_yaml.as_bytes())
            .map_err(|err| format!("write runtime yaml: {err}"))?;
    }

    Ok(CompileResult {
        success: true,
        fingerprint,
        final_yaml,
        warnings: Vec::new(),
        error: None,
    })
}

fn load_combined_override_patch(override_paths: &[String]) -> Result<Option<JsonValue>, String> {
    let mut combined = JsonValue::Object(JsonMap::new());
    let mut has_patch = false;

    for override_path in override_paths {
        let content = fs::read_to_string(override_path)
            .map_err(|err| format!("read override file {}: {err}", override_path))?;
        if content.trim().is_empty() {
            continue;
        }
        let patch: JsonValue = serde_json::from_str(&content)
            .map_err(|err| format!("parse override json {}: {err}", override_path))?;
        merge_override_document(&mut combined, &patch);
        has_patch = true;
    }

    if has_patch {
        Ok(Some(combined))
    } else {
        Ok(None)
    }
}

fn write_atomic(path: &Path, bytes: &[u8]) -> Result<(), String> {
    let parent = path
        .parent()
        .ok_or_else(|| "runtime output path has no parent".to_string())?;
    fs::create_dir_all(parent).map_err(|err| err.to_string())?;
    let tmp = path.with_extension("yaml.tmp");
    fs::write(&tmp, bytes).map_err(|err| err.to_string())?;
    fs::rename(&tmp, path).map_err(|err| err.to_string())?;
    Ok(())
}

fn patch_static_runtime(root: &mut JsonValue, profile_dir: &Path) {
    let Some(object) = root.as_object_mut() else {
        return;
    };

    object.insert(
        "interface-name".to_string(),
        JsonValue::String(String::new()),
    );
    object.insert("routing-mark".to_string(), JsonValue::from(0));

    if has_non_empty_string(object.get("external-controller"))
        || has_non_empty_string(object.get("external-controller-tls"))
    {
        object.insert(
            "external-ui".to_string(),
            JsonValue::String("./ui".to_string()),
        );
    }

    let profile = ensure_object_field(object, "profile");
    profile.insert("store-selected".to_string(), JsonValue::Bool(false));
    profile.insert("store-fake-ip".to_string(), JsonValue::Bool(true));

    let append_system_dns = object
        .get("clash-for-android")
        .and_then(JsonValue::as_object)
        .and_then(|value| value.get("append-system-dns"))
        .and_then(JsonValue::as_bool)
        .unwrap_or(false);

    let dns_enabled = object
        .get("dns")
        .and_then(JsonValue::as_object)
        .and_then(|value| value.get("enable"))
        .and_then(JsonValue::as_bool)
        .unwrap_or(false);

    if !dns_enabled {
        let dns = ensure_object_field(object, "dns");
        dns.insert("enable".to_string(), JsonValue::Bool(true));
        dns.insert("use-hosts".to_string(), JsonValue::Bool(true));
        dns.insert(
            "default-nameserver".to_string(),
            JsonValue::Array(
                DEFAULT_NAME_SERVERS
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            ),
        );
        dns.insert(
            "nameserver".to_string(),
            JsonValue::Array(
                DEFAULT_NAME_SERVERS
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            ),
        );
        dns.insert(
            "enhanced-mode".to_string(),
            JsonValue::String("fake-ip".to_string()),
        );
        dns.insert(
            "fake-ip-range".to_string(),
            JsonValue::String(DEFAULT_FAKE_IP_RANGE.to_string()),
        );
        dns.insert(
            "fake-ip-filter".to_string(),
            JsonValue::Array(
                DEFAULT_FAKE_IP_FILTER
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            ),
        );
        let cfa = ensure_object_field(object, "clash-for-android");
        cfa.insert("append-system-dns".to_string(), JsonValue::Bool(true));
    }

    if object
        .get("clash-for-android")
        .and_then(JsonValue::as_object)
        .and_then(|value| value.get("append-system-dns"))
        .and_then(JsonValue::as_bool)
        .unwrap_or(append_system_dns)
    {
        let dns = ensure_object_field(object, "dns");
        let nameserver = ensure_array_field(dns, "nameserver");
        if !nameserver
            .iter()
            .any(|item| item.as_str() == Some("system://"))
        {
            nameserver.push(JsonValue::String("system://".to_string()));
        }
    }

    let tun = ensure_object_field(object, "tun");
    tun.insert("enable".to_string(), JsonValue::Bool(false));
    tun.insert("auto-route".to_string(), JsonValue::Bool(false));
    tun.insert("auto-detect-interface".to_string(), JsonValue::Bool(false));

    patch_listeners(object);
    patch_providers(object, profile_dir);
}

fn patch_listeners(object: &mut JsonMap<String, JsonValue>) {
    let Some(listeners) = object
        .get_mut("listeners")
        .and_then(JsonValue::as_array_mut)
    else {
        return;
    };
    listeners.retain(|listener| {
        listener
            .as_object()
            .and_then(|value| value.get("type"))
            .and_then(JsonValue::as_str)
            .map(|kind| !matches!(kind, "tproxy" | "redir" | "tun"))
            .unwrap_or(true)
    });
}

fn patch_providers(object: &mut JsonMap<String, JsonValue>, profile_dir: &Path) {
    for (field, prefix) in [("proxy-providers", "proxies"), ("rule-providers", "rules")] {
        let Some(providers) = object.get_mut(field).and_then(JsonValue::as_object_mut) else {
            continue;
        };
        for (_, provider) in providers.iter_mut() {
            let Some(provider_object) = provider.as_object_mut() else {
                continue;
            };
            if let Some(path) = provider_object.get("path").and_then(JsonValue::as_str) {
                if !path.trim().is_empty() {
                    let normalized = normalize_provider_path(path, profile_dir);
                    provider_object.insert("path".to_string(), JsonValue::String(normalized));
                    continue;
                }
            }
            let Some(url) = provider_object.get("url").and_then(JsonValue::as_str) else {
                continue;
            };
            let mut hasher = Sha256::new();
            hasher.update(url.as_bytes());
            let hash = format!("{:x}", hasher.finalize());
            provider_object.insert(
                "path".to_string(),
                JsonValue::String(format!("./providers/{prefix}/{hash}.yaml")),
            );
        }
    }
}

fn normalize_provider_path(path: &str, profile_dir: &Path) -> String {
    let raw = Path::new(path);
    if raw.is_absolute() {
        if let Ok(relative) = raw.strip_prefix(profile_dir) {
            let relative = relative.to_string_lossy().replace('\\', "/");
            if relative.starts_with("./") || relative.starts_with("../") {
                return relative;
            }
            return format!("./{relative}");
        }
        return path.replace('\\', "/");
    }
    path.replace('\\', "/")
}

fn merge_override_document(target: &mut JsonValue, patch: &JsonValue) {
    let (Some(target_object), Some(patch_object)) = (target.as_object_mut(), patch.as_object())
    else {
        *target = patch.clone();
        return;
    };

    for (key, value) in patch_object {
        let parsed = parse_modifier_key(key);
        match parsed.modifier {
            PatchModifier::Start | PatchModifier::End => {
                let entry = target_object
                    .entry(key.clone())
                    .or_insert_with(|| JsonValue::Array(Vec::new()));
                *entry = append_patch_array(entry, value);
            }
            PatchModifier::Merge => {
                let entry = target_object
                    .entry(key.clone())
                    .or_insert_with(|| JsonValue::Object(JsonMap::new()));
                merge_patch_value(entry, value);
            }
            PatchModifier::Force => {
                target_object.insert(key.clone(), value.clone());
            }
            PatchModifier::Replace => match (target_object.get_mut(key), value.as_object()) {
                (Some(existing), Some(_)) if existing.is_object() => {
                    merge_override_document(existing, value)
                }
                _ => {
                    target_object.insert(key.clone(), value.clone());
                }
            },
        }
    }
}

fn append_patch_array(existing: &JsonValue, incoming: &JsonValue) -> JsonValue {
    let mut items = existing.as_array().cloned().unwrap_or_default();
    match incoming.as_array() {
        Some(values) => items.extend(values.iter().cloned()),
        None => items.push(incoming.clone()),
    }
    JsonValue::Array(items)
}

fn merge_patch_value(target: &mut JsonValue, patch: &JsonValue) {
    match (target.as_object_mut(), patch.as_object()) {
        (Some(target_object), Some(patch_object)) => {
            for (key, value) in patch_object {
                let entry = target_object.entry(key.clone()).or_insert(JsonValue::Null);
                merge_patch_value(entry, value);
            }
        }
        _ => {
            *target = patch.clone();
        }
    }
}

fn apply_override_document(target: &mut JsonValue, patch: &JsonValue) {
    let Some(patch_object) = patch.as_object() else {
        *target = clone_raw_value(patch);
        return;
    };

    if !target.is_object() {
        *target = JsonValue::Object(JsonMap::new());
    }

    let target_object = target.as_object_mut().expect("target object");
    for (base_key, operations) in group_patch_keys(patch_object) {
        match field_behavior(SchemaId::Root, &base_key) {
            Some(behavior) => apply_field(target_object, &base_key, behavior, operations),
            None => apply_generic_field(target_object, &base_key, operations),
        }
    }
}

fn apply_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    behavior: FieldBehavior,
    operations: PatchOperations,
) {
    match behavior {
        FieldBehavior::Scalar => apply_scalar_field(target_object, base_key, operations),
        FieldBehavior::List(style) => apply_list_field(target_object, base_key, style, operations),
        FieldBehavior::Map => apply_map_field(target_object, base_key, operations),
        FieldBehavior::Object(schema) => {
            apply_object_field(target_object, base_key, schema, operations)
        }
        FieldBehavior::Rules => apply_rules_field(target_object, operations),
    }
}

fn apply_scalar_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    operations: PatchOperations,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(&force));
        return;
    }
    if let Some(replace) = operations.replace {
        target_object.insert(base_key.to_string(), clone_raw_value(&replace));
    }
}

fn apply_list_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    style: ListStyle,
    operations: PatchOperations,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(&force));
        return;
    }

    let should_write =
        operations.replace.is_some() || operations.start.is_some() || operations.end.is_some();
    if !should_write {
        return;
    }

    let mut items = Vec::<JsonValue>::new();
    if let Some(start) = operations.start.as_ref() {
        items.extend(collect_array_items(start));
    }

    let middle = match operations.replace.as_ref() {
        Some(replace) => collect_array_items(replace),
        None => target_object
            .get(base_key)
            .and_then(JsonValue::as_array)
            .cloned()
            .unwrap_or_default(),
    };
    items.extend(middle);

    if let Some(end) = operations.end.as_ref() {
        items.extend(collect_array_items(end));
    }

    if matches!(style, ListStyle::NamedObjects) {
        items = dedup_named_items(items);
    }

    target_object.insert(base_key.to_string(), JsonValue::Array(items));
}

fn apply_map_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    operations: PatchOperations,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(&force));
        return;
    }

    if let Some(replace) = operations.replace {
        target_object.insert(base_key.to_string(), clone_raw_value(&replace));
        return;
    }

    if let Some(merge) = operations.merge {
        let entry = target_object
            .entry(base_key.to_string())
            .or_insert_with(|| JsonValue::Object(JsonMap::new()));
        merge_raw_map(entry, &merge);
    }
}

fn apply_object_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    _schema: SchemaId,
    operations: PatchOperations,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(&force));
        return;
    }

    if let Some(replace) = operations.replace {
        if replace.is_object() {
            let entry = target_object
                .entry(base_key.to_string())
                .or_insert_with(|| JsonValue::Object(JsonMap::new()));
            apply_nested_object(entry, &replace, base_key);
        } else {
            target_object.insert(base_key.to_string(), clone_raw_value(&replace));
        }
    }
}

fn apply_rules_field(target_object: &mut JsonMap<String, JsonValue>, operations: PatchOperations) {
    let should_write =
        operations.replace.is_some() || operations.start.is_some() || operations.end.is_some();
    if !should_write {
        return;
    }

    let mut items = Vec::<JsonValue>::new();
    if let Some(start) = operations.start.as_ref() {
        items.extend(collect_array_items(start));
    }

    let middle = match operations.replace.as_ref() {
        Some(replace) => collect_array_items(replace),
        None => target_object
            .get("rules")
            .and_then(JsonValue::as_array)
            .cloned()
            .unwrap_or_default(),
    };
    items.extend(middle);

    if let Some(end) = operations.end.as_ref() {
        items.extend(collect_array_items(end));
    }

    target_object.insert("rules".to_string(), JsonValue::Array(items));
}

fn apply_generic_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    operations: PatchOperations,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(&force));
        return;
    }

    if let Some(merge) = operations.merge {
        let entry = target_object
            .entry(base_key.to_string())
            .or_insert_with(|| JsonValue::Object(JsonMap::new()));
        merge_raw_map(entry, &merge);
    }

    if let Some(replace) = operations.replace {
        if replace.is_object()
            && target_object
                .get(base_key)
                .and_then(JsonValue::as_object)
                .is_some()
        {
            let entry = target_object
                .entry(base_key.to_string())
                .or_insert_with(|| JsonValue::Object(JsonMap::new()));
            apply_override_document(entry, &replace);
        } else {
            target_object.insert(base_key.to_string(), clone_raw_value(&replace));
        }
    }

    if operations.start.is_some() || operations.end.is_some() {
        let mut items = Vec::<JsonValue>::new();
        if let Some(start) = operations.start.as_ref() {
            items.extend(collect_array_items(start));
        }
        if let Some(existing) = target_object.get(base_key).and_then(JsonValue::as_array) {
            items.extend(existing.iter().cloned());
        }
        if let Some(end) = operations.end.as_ref() {
            items.extend(collect_array_items(end));
        }
        target_object.insert(base_key.to_string(), JsonValue::Array(items));
    }
}

fn apply_nested_object(target: &mut JsonValue, patch: &JsonValue, key: &str) {
    let schema = match key {
        "dns" => SchemaId::Dns,
        "fallback-filter" => SchemaId::DnsFallbackFilter,
        "sniffer" => SchemaId::Sniffer,
        "sniff" => SchemaId::Sniff,
        "HTTP" | "TLS" | "QUIC" => SchemaId::Protocol,
        "tun" => SchemaId::Tun,
        "external-controller-cors" => SchemaId::ExternalControllerCors,
        "profile" => SchemaId::Profile,
        "geox-url" => SchemaId::GeoxUrl,
        "clash-for-android" => SchemaId::App,
        _ => {
            apply_override_document(target, patch);
            return;
        }
    };

    let Some(patch_object) = patch.as_object() else {
        *target = clone_raw_value(patch);
        return;
    };

    if !target.is_object() {
        *target = JsonValue::Object(JsonMap::new());
    }

    let target_object = target.as_object_mut().expect("nested target object");
    for (base_key, operations) in group_patch_keys(patch_object) {
        match field_behavior(schema, &base_key) {
            Some(behavior) => apply_field(target_object, &base_key, behavior, operations),
            None => apply_generic_field(target_object, &base_key, operations),
        }
    }
}

fn group_patch_keys(map: &JsonMap<String, JsonValue>) -> Vec<(String, PatchOperations)> {
    let mut grouped = Vec::<(String, PatchOperations)>::new();

    for (key, value) in map {
        let parsed = parse_modifier_key(key);
        let base_key = parsed.base.to_string();
        let modifier = parsed.modifier;

        let index = grouped
            .iter()
            .position(|(existing, _)| existing == &base_key)
            .unwrap_or_else(|| {
                grouped.push((base_key.clone(), PatchOperations::default()));
                grouped.len() - 1
            });

        let operations = &mut grouped[index].1;
        match modifier {
            PatchModifier::Replace => operations.replace = Some(value.clone()),
            PatchModifier::Start => operations.start = Some(value.clone()),
            PatchModifier::End => operations.end = Some(value.clone()),
            PatchModifier::Merge => operations.merge = Some(value.clone()),
            PatchModifier::Force => operations.force = Some(value.clone()),
        }
    }

    grouped
}

fn parse_modifier_key(key: &str) -> ParsedKey<'_> {
    if let Some(inner) = literal_key_inner(key) {
        return ParsedKey {
            base: inner,
            modifier: PatchModifier::Replace,
        };
    }

    for (suffix, modifier) in [
        ("-start", PatchModifier::Start),
        ("-end", PatchModifier::End),
        ("-merge", PatchModifier::Merge),
        ("-force", PatchModifier::Force),
    ] {
        if let Some(base) = key.strip_suffix(suffix) {
            return ParsedKey { base, modifier };
        }
    }

    ParsedKey {
        base: key,
        modifier: PatchModifier::Replace,
    }
}

fn literal_key_inner(key: &str) -> Option<&str> {
    key.strip_prefix('<')
        .and_then(|value| value.strip_suffix('>'))
}

fn merge_raw_map(target: &mut JsonValue, patch: &JsonValue) {
    let Some(patch_object) = patch.as_object() else {
        *target = clone_raw_value(patch);
        return;
    };

    if !target.is_object() {
        *target = JsonValue::Object(JsonMap::new());
    }

    let target_object = target.as_object_mut().expect("raw map target");
    for (key, value) in patch_object {
        let key = unescape_literal_key(key);
        match (target_object.get_mut(&key), value.as_object()) {
            (Some(existing), Some(_)) if existing.is_object() => merge_raw_map(existing, value),
            _ => {
                target_object.insert(key, clone_raw_value(value));
            }
        }
    }
}

fn clone_raw_value(value: &JsonValue) -> JsonValue {
    match value {
        JsonValue::Object(object) => JsonValue::Object(
            object
                .iter()
                .map(|(key, value)| (unescape_literal_key(key), clone_raw_value(value)))
                .collect(),
        ),
        JsonValue::Array(items) => JsonValue::Array(items.iter().map(clone_raw_value).collect()),
        _ => value.clone(),
    }
}

fn unescape_literal_key(key: &str) -> String {
    literal_key_inner(key).unwrap_or(key).to_string()
}

fn collect_array_items(value: &JsonValue) -> Vec<JsonValue> {
    match value {
        JsonValue::Null => Vec::new(),
        JsonValue::Array(items) => items.iter().map(clone_raw_value).collect(),
        _ => vec![clone_raw_value(value)],
    }
}

fn dedup_named_items(items: Vec<JsonValue>) -> Vec<JsonValue> {
    let mut out = Vec::<JsonValue>::new();
    let mut seen = std::collections::HashSet::<String>::new();

    for item in items.into_iter().rev() {
        let Some(name) = extract_name(&item) else {
            out.push(item);
            continue;
        };
        if seen.insert(name) {
            out.push(item);
        }
    }

    out.reverse();
    out
}

fn extract_name(value: &JsonValue) -> Option<String> {
    value
        .as_object()
        .and_then(|map| map.get("name"))
        .and_then(JsonValue::as_str)
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty())
}

fn field_behavior(schema: SchemaId, key: &str) -> Option<FieldBehavior> {
    match schema {
        SchemaId::Root => match key {
            "port"
            | "socks-port"
            | "mixed-port"
            | "redir-port"
            | "tproxy-port"
            | "allow-lan"
            | "bind-address"
            | "mode"
            | "log-level"
            | "ipv6"
            | "external-controller"
            | "external-controller-tls"
            | "external-doh-server"
            | "secret"
            | "unified-delay"
            | "geodata-mode"
            | "tcp-concurrent"
            | "find-process-mode"
            | "keep-alive-interval"
            | "keep-alive-idle"
            | "interface-name"
            | "routing-mark"
            | "geosite-matcher"
            | "global-client-fingerprint"
            | "geo-auto-update"
            | "geo-update-interval" => Some(FieldBehavior::Scalar),
            "authentication" | "skip-auth-prefixes" | "lan-allowed-ips" | "lan-disallowed-ips" => {
                Some(FieldBehavior::List(ListStyle::Plain))
            }
            "hosts" | "rule-providers" | "proxy-providers" | "sub-rules" => {
                Some(FieldBehavior::Map)
            }
            "proxies" | "proxy-groups" => Some(FieldBehavior::List(ListStyle::NamedObjects)),
            "rules" => Some(FieldBehavior::Rules),
            "dns" => Some(FieldBehavior::Object(SchemaId::Dns)),
            "external-controller-cors" => {
                Some(FieldBehavior::Object(SchemaId::ExternalControllerCors))
            }
            "profile" => Some(FieldBehavior::Object(SchemaId::Profile)),
            "tun" => Some(FieldBehavior::Object(SchemaId::Tun)),
            "sniffer" => Some(FieldBehavior::Object(SchemaId::Sniffer)),
            "geox-url" => Some(FieldBehavior::Object(SchemaId::GeoxUrl)),
            "clash-for-android" => Some(FieldBehavior::Object(SchemaId::App)),
            _ => None,
        },
        SchemaId::Dns => match key {
            "enable"
            | "cache-algorithm"
            | "prefer-h3"
            | "listen"
            | "ipv6"
            | "use-hosts"
            | "use-system-hosts"
            | "respect-rules"
            | "enhanced-mode"
            | "fake-ip-range"
            | "fake-ip-range6"
            | "fake-ip-filter-mode"
            | "fake-ip-ttl"
            | "ipv6-timeout"
            | "cache-max-size"
            | "direct-nameserver-follow-policy" => Some(FieldBehavior::Scalar),
            "nameserver"
            | "fallback"
            | "default-nameserver"
            | "proxy-server-nameserver"
            | "direct-nameserver"
            | "fake-ip-filter" => Some(FieldBehavior::List(ListStyle::Plain)),
            "nameserver-policy" | "proxy-server-nameserver-policy" => Some(FieldBehavior::Map),
            "fallback-filter" => Some(FieldBehavior::Object(SchemaId::DnsFallbackFilter)),
            _ => None,
        },
        SchemaId::DnsFallbackFilter => match key {
            "geoip" | "geoip-code" => Some(FieldBehavior::Scalar),
            "domain" | "ipcidr" | "geosite" => Some(FieldBehavior::List(ListStyle::Plain)),
            _ => None,
        },
        SchemaId::Sniffer => match key {
            "enable" | "force-dns-mapping" | "parse-pure-ip" | "override-destination" => {
                Some(FieldBehavior::Scalar)
            }
            "force-domain" | "skip-domain" | "skip-src-address" | "skip-dst-address" => {
                Some(FieldBehavior::List(ListStyle::Plain))
            }
            "sniff" => Some(FieldBehavior::Object(SchemaId::Sniff)),
            _ => None,
        },
        SchemaId::Sniff => match key {
            "HTTP" | "TLS" | "QUIC" => Some(FieldBehavior::Object(SchemaId::Protocol)),
            _ => None,
        },
        SchemaId::Protocol => match key {
            "ports" => Some(FieldBehavior::List(ListStyle::Plain)),
            "override-destination" => Some(FieldBehavior::Scalar),
            _ => None,
        },
        SchemaId::Tun => match key {
            "enable"
            | "stack"
            | "auto-route"
            | "auto-detect-interface"
            | "auto-redirect"
            | "mtu"
            | "gso"
            | "gso-max-size"
            | "strict-route"
            | "disable-icmp-forwarding"
            | "endpoint-independent-nat" => Some(FieldBehavior::Scalar),
            "dns-hijack"
            | "route-address"
            | "route-exclude-address"
            | "include-package"
            | "exclude-package" => Some(FieldBehavior::List(ListStyle::Plain)),
            _ => None,
        },
        SchemaId::ExternalControllerCors => match key {
            "allow-origins" => Some(FieldBehavior::List(ListStyle::Plain)),
            "allow-private-network" => Some(FieldBehavior::Scalar),
            _ => None,
        },
        SchemaId::Profile => match key {
            "store-selected" | "store-fake-ip" => Some(FieldBehavior::Scalar),
            _ => None,
        },
        SchemaId::GeoxUrl => match key {
            "geoip" | "mmdb" | "geosite" => Some(FieldBehavior::Scalar),
            _ => None,
        },
        SchemaId::App => match key {
            "append-system-dns" => Some(FieldBehavior::Scalar),
            _ => None,
        },
        SchemaId::ProxyItem | SchemaId::ProxyGroupItem | SchemaId::ProviderItem => None,
    }
}

fn normalize_root(value: &JsonValue) -> YamlValue {
    normalize_object_with_schema(value, SchemaId::Root)
}

fn normalize_object_with_schema(value: &JsonValue, schema: SchemaId) -> YamlValue {
    let Some(object) = value.as_object() else {
        return normalize_generic_value(value);
    };

    let mut mapping = YamlMapping::new();
    let mut written = std::collections::HashSet::<String>::new();

    for key in ordered_keys(schema) {
        if let Some(field_value) = object.get(*key) {
            mapping.insert(
                YamlValue::String((*key).to_string()),
                normalize_field_value(schema, key, field_value),
            );
            written.insert((*key).to_string());
        }
    }

    let mut remaining = object
        .keys()
        .filter(|key| !written.contains(*key))
        .cloned()
        .collect::<Vec<_>>();
    remaining.sort();

    for key in remaining {
        if let Some(field_value) = object.get(&key) {
            mapping.insert(YamlValue::String(key), normalize_generic_value(field_value));
        }
    }

    YamlValue::Mapping(mapping)
}

fn normalize_field_value(schema: SchemaId, key: &str, value: &JsonValue) -> YamlValue {
    match (schema, key) {
        (SchemaId::Root, "dns") => normalize_object_with_schema(value, SchemaId::Dns),
        (SchemaId::Root, "external-controller-cors") => {
            normalize_object_with_schema(value, SchemaId::ExternalControllerCors)
        }
        (SchemaId::Root, "profile") => normalize_object_with_schema(value, SchemaId::Profile),
        (SchemaId::Root, "tun") => normalize_object_with_schema(value, SchemaId::Tun),
        (SchemaId::Root, "sniffer") => normalize_object_with_schema(value, SchemaId::Sniffer),
        (SchemaId::Root, "geox-url") => normalize_object_with_schema(value, SchemaId::GeoxUrl),
        (SchemaId::Root, "clash-for-android") => normalize_object_with_schema(value, SchemaId::App),
        (SchemaId::Dns, "fallback-filter") => {
            normalize_object_with_schema(value, SchemaId::DnsFallbackFilter)
        }
        (SchemaId::Sniffer, "sniff") => normalize_object_with_schema(value, SchemaId::Sniff),
        (SchemaId::Sniff, "HTTP") | (SchemaId::Sniff, "TLS") | (SchemaId::Sniff, "QUIC") => {
            normalize_object_with_schema(value, SchemaId::Protocol)
        }
        (SchemaId::Root, "proxies") => normalize_object_list(value, SchemaId::ProxyItem),
        (SchemaId::Root, "proxy-groups") => normalize_object_list(value, SchemaId::ProxyGroupItem),
        (SchemaId::Root, "rule-providers") | (SchemaId::Root, "proxy-providers") => {
            normalize_object_map(value, Some(SchemaId::ProviderItem))
        }
        (SchemaId::Root, "hosts") | (SchemaId::Root, "sub-rules") => {
            normalize_object_map(value, None)
        }
        _ => normalize_generic_value(value),
    }
}

fn normalize_object_list(value: &JsonValue, item_schema: SchemaId) -> YamlValue {
    let Some(items) = value.as_array() else {
        return normalize_generic_value(value);
    };
    YamlValue::Sequence(
        items
            .iter()
            .map(|item| normalize_object_with_schema(item, item_schema))
            .collect(),
    )
}

fn normalize_object_map(value: &JsonValue, item_schema: Option<SchemaId>) -> YamlValue {
    let Some(object) = value.as_object() else {
        return normalize_generic_value(value);
    };

    let mut keys = object.keys().cloned().collect::<Vec<_>>();
    keys.sort();

    let mut mapping = YamlMapping::new();
    for key in keys {
        let field_value = object.get(&key).expect("map key exists");
        let normalized = match item_schema {
            Some(schema) if field_value.is_object() => {
                normalize_object_with_schema(field_value, schema)
            }
            _ => normalize_generic_value(field_value),
        };
        mapping.insert(YamlValue::String(key), normalized);
    }
    YamlValue::Mapping(mapping)
}

fn normalize_generic_value(value: &JsonValue) -> YamlValue {
    match value {
        JsonValue::Null => YamlValue::Null,
        JsonValue::Bool(value) => YamlValue::Bool(*value),
        JsonValue::Number(value) => normalize_number(value),
        JsonValue::String(value) => YamlValue::String(value.clone()),
        JsonValue::Array(items) => {
            YamlValue::Sequence(items.iter().map(normalize_generic_value).collect())
        }
        JsonValue::Object(object) => {
            let mut keys = object.keys().cloned().collect::<Vec<_>>();
            keys.sort();
            let mut mapping = YamlMapping::new();
            for key in keys {
                let field_value = object.get(&key).expect("object key exists");
                mapping.insert(YamlValue::String(key), normalize_generic_value(field_value));
            }
            YamlValue::Mapping(mapping)
        }
    }
}

fn normalize_number(value: &serde_json::Number) -> YamlValue {
    if let Some(number) = value.as_i64() {
        return serde_yaml::to_value(number).expect("yaml i64");
    }
    if let Some(number) = value.as_u64() {
        return serde_yaml::to_value(number).expect("yaml u64");
    }
    if let Some(number) = value.as_f64() {
        return serde_yaml::to_value(number).expect("yaml f64");
    }
    YamlValue::Null
}

fn ordered_keys(schema: SchemaId) -> &'static [&'static str] {
    match schema {
        SchemaId::Root => ROOT_ORDER,
        SchemaId::Dns => DNS_ORDER,
        SchemaId::DnsFallbackFilter => DNS_FALLBACK_FILTER_ORDER,
        SchemaId::Sniffer => SNIFFER_ORDER,
        SchemaId::Sniff => SNIFF_ORDER,
        SchemaId::Protocol => PROTOCOL_ORDER,
        SchemaId::Tun => TUN_ORDER,
        SchemaId::ExternalControllerCors => EXTERNAL_CONTROLLER_CORS_ORDER,
        SchemaId::Profile => PROFILE_ORDER,
        SchemaId::GeoxUrl => GEOX_URL_ORDER,
        SchemaId::App => APP_ORDER,
        SchemaId::ProxyItem => PROXY_ITEM_ORDER,
        SchemaId::ProxyGroupItem => PROXY_GROUP_ITEM_ORDER,
        SchemaId::ProviderItem => PROVIDER_ITEM_ORDER,
    }
}

fn ensure_object_field<'a>(
    object: &'a mut JsonMap<String, JsonValue>,
    key: &str,
) -> &'a mut JsonMap<String, JsonValue> {
    if !object.get(key).map(JsonValue::is_object).unwrap_or(false) {
        object.insert(key.to_string(), JsonValue::Object(JsonMap::new()));
    }
    object
        .get_mut(key)
        .and_then(JsonValue::as_object_mut)
        .expect("object field")
}

fn ensure_array_field<'a>(
    object: &'a mut JsonMap<String, JsonValue>,
    key: &str,
) -> &'a mut Vec<JsonValue> {
    if !object.get(key).map(JsonValue::is_array).unwrap_or(false) {
        object.insert(key.to_string(), JsonValue::Array(Vec::new()));
    }
    object
        .get_mut(key)
        .and_then(JsonValue::as_array_mut)
        .expect("array field")
}

fn has_non_empty_string(value: Option<&JsonValue>) -> bool {
    value
        .and_then(JsonValue::as_str)
        .map(|value| !value.trim().is_empty())
        .unwrap_or(false)
}

fn jstring_to_string(env: &mut JNIEnv<'_>, input: JString<'_>) -> Result<String, String> {
    env.get_string(&input)
        .map(|value| value.into())
        .map_err(|err| err.to_string())
}

fn result_to_jstring(env: &mut JNIEnv<'_>, payload: String) -> jstring {
    env.new_string(payload)
        .map(|value| value.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCompilePreview(
    mut env: JNIEnv<'_>,
    _thiz: JObject<'_>,
    request_json: JString<'_>,
) -> jstring {
    let payload = match jstring_to_string(&mut env, request_json) {
        Ok(value) => match compile_request(&value, false) {
            Ok(result) => success_result(result.fingerprint, result.final_yaml),
            Err(err) => error_result(err),
        },
        Err(err) => error_result(err),
    };

    result_to_jstring(&mut env, payload)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCompileToFile(
    mut env: JNIEnv<'_>,
    _thiz: JObject<'_>,
    request_json: JString<'_>,
) -> jstring {
    let payload = match jstring_to_string(&mut env, request_json) {
        Ok(value) => match compile_request(&value, true) {
            Ok(result) => success_result(result.fingerprint, result.final_yaml),
            Err(err) => error_result(err),
        },
        Err(err) => error_result(err),
    };

    result_to_jstring(&mut env, payload)
}
