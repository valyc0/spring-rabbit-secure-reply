#!/usr/bin/env bash
curl -X POST http://localhost:8080/messages -H 'Content-Type: text/plain' -d "${1:-ciao rabbit}"
echo
