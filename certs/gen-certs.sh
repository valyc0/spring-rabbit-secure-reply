#!/usr/bin/env bash
# Genera CA, certificato server (localhost) e certificato client (CN=demo-client).
# Il CN del certificato client e' il nome utente RabbitMQ (login senza password).
set -euo pipefail
cd "$(dirname "$0")"
PASS="${CERT_PASS:-changeit}"
CLIENT_CN="${CLIENT_CN:-demo-client}"

openssl req -x509 -newkey rsa:2048 -nodes -days 825 -subj "/CN=demo-ca" \
  -keyout ca.key -out ca.pem 2>/dev/null

# server
openssl req -newkey rsa:2048 -nodes -subj "/CN=localhost" -keyout server.key -out server.csr 2>/dev/null
printf "subjectAltName=DNS:localhost,IP:127.0.0.1\nextendedKeyUsage=serverAuth\n" > server.ext
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -CAcreateserial -days 825 \
  -extfile server.ext -out server.pem 2>/dev/null

# client
openssl req -newkey rsa:2048 -nodes -subj "/CN=${CLIENT_CN}" -keyout client.key -out client.csr 2>/dev/null
printf "extendedKeyUsage=clientAuth\n" > client.ext
openssl x509 -req -in client.csr -CA ca.pem -CAkey ca.key -CAcreateserial -days 825 \
  -extfile client.ext -out client.pem 2>/dev/null

# keystore client + truststore per Java (PKCS12)
openssl pkcs12 -export -in client.pem -inkey client.key -certfile ca.pem \
  -name client -passout "pass:${PASS}" -out client.p12
rm -f truststore.p12
keytool -importcert -noprompt -alias ca -file ca.pem -keystore truststore.p12 \
  -storetype PKCS12 -storepass "${PASS}" >/dev/null

rm -f *.csr *.ext *.srl
# la chiave server deve essere leggibile dall'utente rabbitmq nel container
chmod 644 server.key
echo "Certificati generati in $(pwd)"
