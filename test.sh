#!/usr/bin/env bash
curl -X POST http://localhost:8080/messages -H 'Content-Type: application/json' \
  -d "{\"sender\":\"${1:-valerio}\",\"text\":\"${2:-ciao rabbit}\",\"timestamp\":$(date +%s%3N)}"
echo
