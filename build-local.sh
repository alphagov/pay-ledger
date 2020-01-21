#!/bin/bash

set -e

cd "$(dirname "$0")"

mvn -DskipITs -DskipTests clean verify
docker build -t govukpay/ledger:local .
