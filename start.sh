#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
set -a; source .env; set +a
cd demo && mvn spring-boot:run
