#!/usr/bin/env bash
pid=$(lsof -ti tcp:8080)
if [ -n "$pid" ]; then kill $pid && echo "Terminato processo $pid sulla 8080"; else echo "Nessun processo sulla 8080"; fi
