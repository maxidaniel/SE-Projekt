#!/usr/bin/env bash
set -euo pipefail

echo "=== Running tests with coverage ==="
sbt clean coverage test coverageReport
