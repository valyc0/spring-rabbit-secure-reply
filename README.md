# RabbitMQ + Spring Boot demo

Un endpoint REST invia un messaggio su una coda RabbitMQ (`demo.queue`) e un service con `@RabbitListener` lo consuma e lo scrive nel log.

## Requisiti

- Docker + Docker Compose
- Java 17+
- Maven 3.9+

## Struttura

```
.
├── docker-compose.yml   # RabbitMQ con management UI
└── demo/                # app Spring Boot 3
```

## Avvio

### 1. Avviare RabbitMQ

```bash
docker compose up -d
```

- AMQP: `localhost:5672`
- Management UI: http://localhost:15672 (utente `guest`, password `guest`)

### 2. Avviare l'app

```bash
cd demo
mvn spring-boot:run
```

L'app parte su `http://localhost:8080` e dichiara la coda `demo.queue` all'avvio.

### 3. Inviare un messaggio

```bash
curl -X POST http://localhost:8080/messages \
  -H 'Content-Type: text/plain' \
  -d 'ciao rabbit'
```

Risposta:

```
Inviato: ciao rabbit
```

Nel log dell'app comparirà:

```
Messaggio ricevuto: ciao rabbit
```

Per mandarne più di uno:

```bash
for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8080/messages -H 'Content-Type: text/plain' -d "messaggio $i"
  echo
done
```

## Verificare la coda

Dalla management UI, tab **Queues**, coda `demo.queue`: il contatore *Ready* resta a 0 perché il listener consuma subito i messaggi.

Per vedere i messaggi accumularsi, avvia l'app una volta (così crea la coda), fermala, pubblica messaggi dalla UI (*Queues → demo.queue → Publish message*), poi riavvia l'app: li consumerà tutti.

Da CLI:

```bash
docker exec rabbitmq rabbitmqctl list_queues name messages
```

## Configurazione

`demo/src/main/resources/application.yml`:

| Chiave | Default | Descrizione |
|---|---|---|
| `spring.rabbitmq.host` | `localhost` | host RabbitMQ |
| `spring.rabbitmq.port` | `5672` | porta AMQP |
| `spring.rabbitmq.username` / `password` | `guest` / `guest` | credenziali |
| `app.queue` | `demo.queue` | nome della coda |

## Componenti

- `DemoApplication` — entry point, dichiara la coda durable.
- `MessageController` — `POST /messages`, usa `RabbitTemplate` per inviare il body in coda.
- `MessageListener` — `@RabbitListener` sulla coda, logga ogni messaggio ricevuto.

## Stop

```bash
docker compose down
```
