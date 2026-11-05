# RabbitMQ + Spring Boot demo

Un endpoint REST invia un messaggio su una coda RabbitMQ (`demo.queue`) e un service con `@RabbitListener` lo consuma e lo scrive nel log.

## Requisiti

- Docker + Docker Compose
- Java 17+
- Maven 3.9+

## Struttura

```
.
├── docker-compose.yml   # RabbitMQ con TLS
├── rabbitmq/            # rabbitmq.conf, definitions.json, enabled_plugins
├── certs/               # gen-certs.sh (+ certificati generati, ignorati)
└── demo/                # app Spring Boot 3
```

## Avvio

### 0. Generare i certificati

```bash
cp .env.example .env      # CERT_PASS = password dei file .p12
./certs/gen-certs.sh      # CA, certificato server e certificato client (CN=demo-client)
```

Non ci sono user/password: RabbitMQ autentica l'app dal certificato client (il CN `demo-client` è l'utente, definito in `rabbitmq/definitions.json`). I file in `certs/` e `.env` sono in `.gitignore`.

### 1. Avviare RabbitMQ

```bash
docker compose up -d
```

- AMQPS (TLS + certificato client): `localhost:5671`
- Nessuna management UI né porta in chiaro (non ci sono utenti con password).

### 2. Avviare l'app

```bash
./start.sh   # carica .env e lancia mvn spring-boot:run
```

L'app parte su `http://localhost:8080` e dichiara la coda `demo.queue` all'avvio.

### 3. Inviare un messaggio

Il messaggio è un record Java `Notification(String sender, String text, long timestamp)`, inviato come JSON.

```bash
./test.sh                          # sender "valerio", text "ciao rabbit", timestamp = ora
./test.sh mario "altro messaggio"  # sender e text personalizzati
```

Che equivale a:

```bash
curl -X POST http://localhost:8080/messages \
  -H 'Content-Type: application/json' \
  -d '{"sender":"valerio","text":"ciao rabbit","timestamp":1730000000000}'
```

Risposta:

```
Inviato: Notification[sender=valerio, text=ciao rabbit, timestamp=1730000000000]
```

Nel log dell'app comparirà:

```
Messaggio ricevuto: Notification[sender=valerio, text=ciao rabbit, timestamp=1730000000000]
```

## Verificare la coda

```bash
docker exec rabbitmq rabbitmqctl list_queues name messages
```

## Sicurezza: autenticazione con certificati (mTLS)

RabbitMQ non usa user e password. L'identità dell'app è il **certificato client**: chi non ne possiede uno firmato dalla nostra CA non riesce nemmeno a completare la connessione.

### Il concetto in breve

TLS normale (come HTTPS) autentica solo il server: il client verifica che il server sia chi dice di essere. Con **mTLS** (mutual TLS) vale anche il contrario: il server pretende un certificato dal client e lo verifica. Qui il certificato sostituisce username e password.

```
        App Spring Boot                                   RabbitMQ
   (client.p12 + truststore.p12)                  (server.pem + ca.pem)
              │                                              │
              │ 1. ClientHello ─────────────────────────────▶│
              │ 2. ◀──────────── certificato server (server.pem)
              │    l'app lo verifica con la CA nel truststore
              │    e controlla che il nome sia "localhost"
              │ 3. certificato client (client.pem) ─────────▶│
              │                       RabbitMQ lo verifica con ca.pem
              │                       (firmato dalla CA? non scaduto?)
              │ 4. canale TLS cifrato stabilito              │
              │ 5. AMQP: SASL EXTERNAL ─────────────────────▶│
              │                       utente = CN del certificato ("demo-client")
              │                       esiste? ha i permessi? → connessione OK
```

### I certificati (`certs/gen-certs.sh`)

| File | Cos'è | Dove viene usato |
|---|---|---|
| `ca.pem` / `ca.key` | Autorità di certificazione (CA) locale. Firma gli altri certificati. La chiave `ca.key` serve solo per generarli. | `ca.pem` è dato a RabbitMQ (per verificare i client) e finisce nel truststore dell'app (per verificare il server) |
| `server.pem` / `server.key` | Identità di RabbitMQ, `CN=localhost`, SAN `localhost` e `127.0.0.1` | montati nel container RabbitMQ |
| `client.pem` / `client.key` | Identità dell'app, `CN=demo-client` | impacchettati in `client.p12` |
| `client.p12` | **Keystore** Java (PKCS12): certificato client + chiave privata | l'app lo presenta a RabbitMQ |
| `truststore.p12` | **Truststore** Java (PKCS12): contiene solo `ca.pem` | l'app si fida solo dei server firmati da questa CA |

I file `.p12` sono protetti dalla password `CERT_PASS` (in `.env`). È l'unico segreto, e serve solo a leggere i file locali: non viaggia mai verso RabbitMQ.

### Come RabbitMQ decide chi sei

Configurato in `rabbitmq/rabbitmq.conf`:

| Impostazione | Effetto |
|---|---|
| `listeners.tcp = none` | niente porta 5672 in chiaro, solo TLS |
| `listeners.ssl.default = 5671` | porta AMQPS |
| `ssl_options.verify = verify_peer` + `fail_if_no_peer_cert = true` | il client **deve** presentare un certificato valido firmato dalla CA, altrimenti handshake fallito |
| `auth_mechanisms.1 = EXTERNAL` | l'unico meccanismo ammesso è "l'identità arriva dal canale TLS", non da password (il PLAIN è disabilitato) |
| `ssl_cert_login_from = common_name` | il nome utente è il **CN** del certificato client |
| `load_definitions` | carica `rabbitmq/definitions.json` all'avvio |

`rabbitmq/enabled_plugins` abilita `rabbitmq_auth_mechanism_ssl`, il plugin che implementa `EXTERNAL`.

In `rabbitmq/definitions.json` è definito l'utente `demo-client` **senza password** (hash vuoto) e con permessi sul vhost `/`. Un utente senza password non può fare login in nessun altro modo: l'unico accesso è un certificato con `CN=demo-client` firmato dalla CA. L'utente `guest` non esiste.

### Dove compare `demo-client`

Nel codice Java non compare mai, ed è voluto: l'app non sa chi è e non invia nessun nome utente. Il nome compare in due soli posti, e la connessione funziona perché **coincidono**:

| Dove | Ruolo |
|---|---|
| `certs/gen-certs.sh` (variabile `CLIENT_CN`) | lo scrive come **CN** dentro `client.pem` (verifica: `openssl x509 -in certs/client.pem -noout -subject`) |
| `rabbitmq/definitions.json` | crea l'**utente** `demo-client` (senza password) con i suoi permessi sul vhost `/` |

Cosa succede a ogni connessione:

1. L'app presenta `client.p12`, che contiene il certificato con `CN=demo-client`.
2. RabbitMQ verifica che sia firmato dalla nostra CA.
3. Legge il CN dal certificato (`ssl_cert_login_from = common_name`) e cerca un utente con quel nome.
4. Trova `demo-client` in `definitions.json` e applica i suoi permessi.

`definitions.json` serve perché con `EXTERNAL` l'utente deve comunque esistere nel database interno di RabbitMQ: senza quel file non esisterebbe nessun utente `demo-client` e il login col certificato verrebbe rifiutato.

Se i nomi non coincidono (es. certificato con `CN=pippo` e utente `demo-client`) l'handshake TLS riesce ma il login viene rifiutato, perché l'utente `pippo` non esiste. Per cambiare nome va modificato in **entrambi** i posti.

### Come l'app si collega

- `application.yml` (`spring.rabbitmq.ssl.*`): abilita TLS sulla porta 5671, indica `client.p12` (la nostra identità) e `truststore.p12` (di chi ci fidiamo) e attiva `verify-hostname` (il nome nel certificato server deve corrispondere all'host).
- `RabbitConfig.java`: imposta `SaslConfig.EXTERNAL`, cioè dice al client AMQP di non inviare username/password ma di usare l'identità del certificato.

### Cosa viene bloccato

| Tentativo | Esito |
|---|---|
| Connessione senza certificato client | `handshake failure` |
| Certificato client firmato da un'altra CA | handshake fallito |
| Certificato valido ma con CN diverso da `demo-client` | handshake ok, ma login rifiutato (l'utente non esiste) |
| Connessione in chiaro su 5672 | porta chiusa |
| Login con `guest/guest` o altre password | impossibile: PLAIN disabilitato |
| Man-in-the-middle: server con certificato non firmato dalla CA | l'app rifiuta (truststore) |

Verifica a mano che senza certificato non si entra:

```bash
# senza certificato client -> handshake failure
echo | openssl s_client -connect localhost:5671 -CAfile certs/ca.pem -tls1_2

# con certificato client -> connessione TLS stabilita
echo | openssl s_client -connect localhost:5671 -CAfile certs/ca.pem \
  -cert certs/client.pem -key certs/client.key
```

### Gestione dei certificati

- **Generare / ruotare**: `./certs/gen-certs.sh`, poi riavvia RabbitMQ (`docker compose restart`) e l'app. Validità: 825 giorni.
- **Un altro client** (es. un secondo servizio): serve un certificato con un altro CN firmato dalla stessa CA e un utente con quel nome in `rabbitmq/definitions.json`. Attenzione: `gen-certs.sh` rigenera **tutto**, CA compresa, quindi invalida i certificati esistenti; per aggiungere un client senza rigenerare la CA firma un nuovo certificato con `ca.pem`/`ca.key` esistenti.
- **Cosa non committare**: le chiavi private (`*.key`, `*.p12`) e `.env` sono in `.gitignore`. `ca.key` è la più sensibile: chi la possiede può emettere certificati accettati da RabbitMQ.
- **Produzione**: questa CA è per sviluppo. In produzione usa una CA aziendale, certificati con scadenze brevi e rinnovo automatico, e non esporre la `ca.key` sulle macchine.
- L'API REST dell'app (`POST /messages`) **non è protetta**: la sicurezza riguarda solo il canale app ⇄ RabbitMQ.

## Configurazione

`demo/src/main/resources/application.yml`:

| Chiave | Default | Descrizione |
|---|---|---|
| `spring.rabbitmq.host` | `localhost` | host RabbitMQ |
| `spring.rabbitmq.port` | `5671` | porta AMQPS |
| `spring.rabbitmq.ssl.*` | keystore/truststore in `certs/` | mTLS |
| `app.queue` | `demo.queue` | nome della coda |

## Componenti

- `DemoApplication` — entry point, dichiara la coda durable.
- `RabbitConfig` — abilita l'autenticazione EXTERNAL (certificato client) e registra il `Jackson2JsonMessageConverter` che converte i record da/verso JSON.
- `Notification` — record con 2 `String` e un `long`: è il payload del messaggio.
- `MessageController` — `POST /messages`, riceve il JSON come `Notification` e lo invia in coda con `RabbitTemplate`.
- `MessageListener` — `@RabbitListener` sulla coda, riceve la `Notification` già deserializzata e la logga.

## Stop

```bash
docker compose down
```
