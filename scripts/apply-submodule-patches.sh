#!/usr/bin/env bash
set -euo pipefail

# Usage: apply-submodule-patches.sh <submodule-path> <patches-dir>
# Applies all .patch files from patches-dir into submodule and commits them if they change.

SUBMODULE_PATH="$1"
PATCHES_DIR="$2"

if [ ! -d "$SUBMODULE_PATH" ]; then
  echo "Submodule path $SUBMODULE_PATH does not exist"
  exit 1
fi

if [ ! -d "$PATCHES_DIR" ]; then
  echo "Patches dir $PATCHES_DIR does not exist, skipping"
  exit 0
fi

set +e
changes_made=0
for p in "$PATCHES_DIR"/*.patch; do
  [ -e "$p" ] || continue
  echo "Processing patch: $p"
  # Check if patch already applies cleanly
  git -C "$SUBMODULE_PATH" apply --check "$p"
  rc=$?
  if [ $rc -eq 0 ]; then
    # Clean apply
    git -C "$SUBMODULE_PATH" apply --index "$p"
    git -C "$SUBMODULE_PATH" add -A
    if ! git -C "$SUBMODULE_PATH" diff --cached --quiet; then
      git -C "$SUBMODULE_PATH" commit -m "Apply local patch: $(basename "$p")"
      changes_made=1
    fi
  else
    # Try 3-way apply (better for upstream changes)
    git -C "$SUBMODULE_PATH" apply --3way "$p" 2>/dev/null || true
    git -C "$SUBMODULE_PATH" add -A
    if ! git -C "$SUBMODULE_PATH" diff --cached --quiet; then
      git -C "$SUBMODULE_PATH" commit -m "Apply local patch (3-way): $(basename "$p")"
      changes_made=1
    else
      echo "Patch $(basename "$p") appears already applied or cannot be cleanly applied; skipping"
    fi
  fi
done
set -e

if [ "$changes_made" -eq 1 ]; then
  echo "Patches applied in $SUBMODULE_PATH"
  exit 0
else
  echo "No patches applied for $SUBMODULE_PATH"
  exit 0
fi
