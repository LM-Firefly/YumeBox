import hashlib
import json
import os
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


def parse_gradle_property(name: str) -> str:
    path = Path("gradle.properties")
    if not path.exists():
        return ""
    text = path.read_text(encoding="utf-8")
    for line in text.splitlines():
        if line.startswith(f"{name}="):
            return line.split("=", 1)[1].strip()
    return ""


def default_meta_name(channel: str) -> str:
    if not channel:
        return "yumebox-meta.json"
    return f"yumebox-{channel}-meta.json"


def release_notes(channel: str, release_tag: str) -> str:
    if channel == "stable":
        try:
            tag_list = subprocess.check_output(
                ["git", "tag", "--sort=-version:refname"],
                text=True,
            ).strip().splitlines()
            tag_list = [t for t in tag_list if t != release_tag and t.startswith("v")]
            prev_tag = tag_list[0] if tag_list else None
        except Exception:
            prev_tag = None
        if prev_tag:
            cmd = ["git", "log", f"{prev_tag}..HEAD", "--pretty=- %s (%h)"]
        else:
            cmd = ["git", "log", "-n", "20", "--pretty=- %s (%h)"]
    else:
        cmd = ["git", "log", "-n", "20", "--pretty=- %s (%h)"]
    try:
        notes = subprocess.check_output(cmd, text=True).strip()
        return notes or "- No release notes."
    except Exception:
        return "- No release notes."


def main() -> int:
    repo = os.environ.get("GITHUB_REPOSITORY", "LM-Firefly/YumeBox")
    release_tag = os.environ.get("RELEASE_TAG", "")
    channel = os.environ.get("CHANNEL", "")
    commit_sha = os.environ.get("GITHUB_SHA", "")
    publish_dir = Path("publish")

    apk_files = sorted(publish_dir.rglob("*.apk"))
    if not apk_files:
        print("No APKs found in publish directory")
        return 1

    version_name = parse_gradle_property("project.version.name") or release_tag
    version_code_str = parse_gradle_property("project.version.code")
    try:
        version_code = int(version_code_str)
    except ValueError:
        version_code = 0

    packages = []
    for apk in apk_files:
        name = apk.name
        abi = "universal"
        if name.endswith("-arm64-v8a.apk"):
            abi = "arm64-v8a"
        elif name.endswith("-armeabi-v7a.apk"):
            abi = "armeabi-v7a"
        elif name.endswith("-x86_64.apk"):
            abi = "x86_64"
        elif name.endswith("-x86.apk"):
            abi = "x86"

        sha256 = hashlib.sha256()
        with apk.open("rb") as f:
            for chunk in iter(lambda: f.read(1024 * 1024), b""):
                sha256.update(chunk)

        packages.append(
            {
                "abi": abi,
                "fileName": name,
                "downloadUrl": f"https://github.com/{repo}/releases/download/{release_tag}/{name}",
                "size": apk.stat().st_size,
                "sha256": sha256.hexdigest(),
                "isUniversal": abi == "universal",
            }
        )

    meta_name = ""
    if len(sys.argv) > 1:
        meta_name = sys.argv[1]
    if not meta_name:
        meta_name = os.environ.get("META_ASSET_NAME", "")
    if not meta_name:
        meta_name = default_meta_name(channel)

    meta_dir = publish_dir / "meta"
    meta_dir.mkdir(parents=True, exist_ok=True)
    meta = {
        "schemaVersion": 1,
        "channel": channel,
        "module": "all",
        "artifactPrefix": f"yumebox-{channel}" if channel else "yumebox",
        "tag": release_tag,
        "versionName": version_name,
        "versionCode": version_code,
        "publishedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "commitSha": commit_sha,
        "releaseNotes": release_notes(channel, release_tag),
        "releaseUrl": f"https://github.com/{repo}/releases/tag/{release_tag}",
        "packages": packages,
    }

    output_path = meta_dir / meta_name
    output_path.write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {output_path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
