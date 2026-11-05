#!/usr/bin/env bash
# uso: ./test.sh [sender] [text] [replyTo]   (replyTo: demo.reply.a | demo.reply.b)
URL=http://localhost:8080/messages${3:+?replyTo=$3}
curl -X POST "$URL" -H 'Content-Type: application/json' \
  -d "{\"sender\":\"${1:-valerio}\",\"text\":\"${2:-ciao rabbit}\",\"timestamp\":$(date +%s%3N)}"
echo
