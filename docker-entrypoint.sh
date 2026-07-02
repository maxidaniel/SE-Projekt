#!/usr/bin/env bash
set -euo pipefail

mode="${APP_MODE:-tui}"
save_format="${SAVE_FORMAT:-json}"

mode="${mode,,}"
save_format="${save_format,,}"

if [[ $# -eq 0 ]]; then
  case "$mode" in
    gui|tui) ;;
    *)
      echo "Invalid APP_MODE '$mode' (expected: gui or tui)" >&2
      exit 2
      ;;
  esac

  case "$save_format" in
    json|xml) ;;
    *)
      echo "Invalid SAVE_FORMAT '$save_format' (expected: json or xml)" >&2
      exit 2
      ;;
  esac

  set -- "--$mode" "--$save_format"
fi

run_cmd="run"
for arg in "$@"; do
  run_cmd+=" $arg"
done

exec sbt --batch "$run_cmd"
