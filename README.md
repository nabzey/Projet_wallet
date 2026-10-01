# Projet Wallet

Plateforme composée de 2 microservices Spring Boot indépendants :

- **wallet-service** (port 8091) — comptes, authentification OTP + PIN + JWT, dépôt/retrait/paiement
- **service-app** (port 8092) — demandes de prestation, ressources, tâches ; déclenche le paiement chez `wallet-service`

## Architecture

Chaque service suit le même découpage en couches :

```
controller/   REST, ultra fin — délègue au wrapper
wrapper/      orchestrateur du cas d'usage (préconditions + calcul + persistance + notification)
helper/       calcul métier pur, sans accès DB
service/      persistance + @Transactional (débit atomique du solde ici)
mapper/       conversion Entity ↔ DTO
exception/    exceptions métier dédiées + GlobalExceptionHandler
securite/     JWT, PIN (BCrypt), OTP (simulé)
```

Communication entre les deux services :
- **REST synchrone** : `service-app` appelle `wallet-service` (`POST /api/transactions/paiement`) en transmettant le JWT de l'utilisateur.
- **Kafka asynchrone (Outbox Pattern)** : `wallet-service` publie `paiement-effectue` (topic configurable) après le débit ; `service-app` l'écoute pour confirmer ou compenser la prestation (saga chorégraphiée).

Le point critique corrigé par rapport à un projet Spring Boot classique : le débit du solde se fait via une requête SQL atomique (`UPDATE ... WHERE solde >= montant`) et non par lecture puis réécriture en mémoire — élimine tout risque de double dépense en cas de paiements concurrents.

## Prérequis

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

## Démarrage

```bash
# 1. Infrastructure (Postgres x2 + Kafka)
docker compose up -d

# 2. wallet-service
cd wallet-service
mvn spring-boot:run

# 3. service-app (dans un autre terminal)
cd service-app
mvn spring-boot:run
```

Swagger :
- wallet-service : http://localhost:8091/swagger-ui.html
- service-app : http://localhost:8092/swagger-ui.html

Pour tester les endpoints protégés dans Swagger :
1. Dérouler `Authentification` → `request-otp` → `verify-otp` → `create-pin` (le code OTP s'affiche dans les logs de wallet-service, pas besoin de Postman).
2. Copier le `token` retourné par `create-pin`.
3. Cliquer sur le bouton **Authorize** (cadenas, en haut à droite de la page) et coller le token seul (sans le préfixe `Bearer`, Swagger l'ajoute automatiquement).
4. Tous les autres endpoints (`Compte`, `Transactions`, `Prestations`...) deviennent utilisables directement depuis la page, sans ressaisir le token.

## Flux de test complet

```bash
# 1. Demander un OTP
curl -X POST http://localhost:8091/api/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567"}'
# → le code s'affiche dans les logs de wallet-service (OTP simulé)

# 2. Vérifier l'OTP
curl -X POST http://localhost:8091/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567", "code": "1234"}'

# 3. Créer le PIN → récupère le JWT
curl -X POST http://localhost:8091/api/auth/create-pin \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567", "pin": "1234"}'
# → { "token": "...", "numeroCompte": "CPT-..." }

TOKEN="<coller le token ici>"

# 4. Déposer de l'argent sur le compte
curl -X POST http://localhost:8091/api/transactions/depot \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"montant": 50000}'

# 5. Créer une prestation
curl -X POST http://localhost:8092/api/prestations \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"titre": "Développement site web", "description": "Site vitrine", "montant": 20000}'
# → { "id": 1, "statut": "EN_ATTENTE_PAIEMENT", ... }

# 6. Payer la prestation (déclenche le débit chez wallet-service via REST,
#    puis la confirmation PAYEE arrive de façon asynchrone via Kafka)
curl -X POST http://localhost:8092/api/prestations/1/payer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"pin": "1234"}'

# 7. Vérifier le statut (attendre 1-2s le temps que l'event Kafka soit consommé)
curl http://localhost:8092/api/prestations/1
```

## Simplifications assumées (à documenter dans le rapport de mémoire)

- **1 seul broker Kafka** (pas de cluster à 3 nœuds) — suffisant pour démontrer le pattern Saga/Outbox, la réplication étant une question de passage à l'échelle en production.
- **Secret JWT partagé** entre les deux services (HMAC) plutôt qu'un IdP externe (Keycloak) — cohérent avec le flux d'authentification maison (OTP + PIN) demandé par le cahier des charges.
- **Idempotence** via un champ `demandeId` unique sur `Transaction` côté wallet-service, dérivé de l'id de la prestation côté service-app (`PRESTATION-{id}`).

## Problèmes rencontrés au démarrage et corrections

Spring Boot 4.0.5 est une version très récente qui a cassé plusieurs comportements habituels. Si le projet est repris plus tard ou réexpliqué en soutenance, voici ce qui a coincé et pourquoi.

### 1. Conflit de ports avec d'autres projets sur la machine

**Symptôme** : `curl localhost:8081/...` répondait, mais avec un résultat incohérent (page nginx au lieu de l'API).
**Cause** : le port 8081 était déjà utilisé par un autre projet (conteneur Docker tournant en permanence), et les ports Postgres 5432/5433 par un Postgres local et un autre projet.
**Correction** : `wallet-service` → port **8091**, `service-app` → port **8092**, Postgres → **5440/5441** (voir `docker-compose.yml` et les `application.yaml`). Avant de choisir un port sur cette machine, vérifier avec `docker ps` et `ss -tln` qu'il est libre.

### 2. `ObjectMapper` introuvable (Jackson 3 vs Jackson 2)

**Symptôme** : `APPLICATION FAILED TO START — Parameter 1 of constructor ... required a bean of type 'com.fasterxml.jackson.databind.ObjectMapper' that could not be found`.
**Cause** : Spring Boot 4 embarque une nouvelle lignée **Jackson 3** sous le groupId `tools.jackson.*`, différente du Jackson 2 classique (`com.fasterxml.jackson.*`). Le bean auto-configuré par Spring est donc de type `tools.jackson.databind.ObjectMapper`, qui ne correspond pas aux imports classiques utilisés dans le code (ni à ceux de `jjwt-jackson`, qui reste en Jackson 2).
**Correction** : dans `OutboxService` (wallet-service) et `PaiementListener` (service-app), un `ObjectMapper` classique est instancié directement (`private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();`) plutôt qu'injecté — `jjwt-jackson` garantit déjà la présence de Jackson 2 sur le classpath.

### 3. `KafkaTemplate`/`ConsumerFactory` introuvables

**Symptôme** : `No qualifying bean of type 'org.springframework.kafka.core.KafkaTemplate<java.lang.String, java.lang.String>' available`.
**Cause** : les beans Kafka auto-configurés par Spring Boot portent des types génériques effacés (`<Object, Object>`), qui ne correspondent pas exactement aux points d'injection typés `<String, String>` du code.
**Correction** : déclaration explicite des beans `ProducerFactory<String, String>`/`KafkaTemplate<String, String>` (wallet-service, `KafkaProducerConfig`) et `ConsumerFactory<String, String>` (service-app, `KafkaConsumerConfig`), construits à partir des propriétés `spring.kafka.*`.

### 4. Swagger UI en erreur 403 / 500

**Symptôme** : `/v3/api-docs` renvoyait 403, puis une fois corrigé, l'erreur réelle apparaissait : `NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'`.
**Cause** : `springdoc-openapi-starter-webmvc-ui` en version `2.5.0` cible Spring Framework 6 (Spring Boot 3) et n'est pas compatible avec Spring Framework 7 (Spring Boot 4).
**Correction** : montée de version vers `springdoc-openapi-starter-webmvc-ui` **3.1.1** dans les deux `pom.xml`.

**Leçon générale** : Spring Boot 4.0.5 étant une version de pointe, chaque dépendance tierce (springdoc, jjwt...) doit être vérifiée individuellement pour sa compatibilité — ne pas supposer qu'une version qui fonctionne sur Spring Boot 3 fonctionne telle quelle sur Spring Boot 4.
