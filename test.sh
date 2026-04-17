#!/usr/bin/env bash

# e = exit immediately on failure
# x = print all commands to the terminal
set -ex 

sbt clean coverage test
sbt coverageReport
