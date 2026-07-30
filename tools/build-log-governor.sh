#!/usr/bin/env bash
set -euo pipefail

repo_root=$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
source_dir="$repo_root/log-governor"
output=${1:-"$repo_root/build/ts18-log-governor.zip"}
if [[ $output != /* ]]; then
  output="$repo_root/$output"
fi
stage=$(mktemp -d)
trap 'rm -rf -- "$stage"' EXIT
mkdir -p "$(dirname "$output")"
rm -f "$output"
cp -a "$source_dir/." "$stage/"
find "$stage" -exec touch -h -d '@315532800' {} +
(
  cd "$stage"
  find . -type f -print0 | LC_ALL=C sort -z | xargs -0 zip -q -0 -X "$output"
)
unzip -t "$output" >/dev/null
printf '%s\n' "$output"
