#!/usr/bin/env bash
# Esempio: il REST sceglie la coda su cui il listener deve rispondere.
# Richiede l'app avviata (./start.sh). Le risposte compaiono nel log dell'app.
cd "$(dirname "$0")"

./test.sh valerio "rispondi su A" demo.reply.a
./test.sh valerio "rispondi su B" demo.reply.b
./test.sh valerio "nessuna risposta"
