# Projet Wallet

Deux microservices Spring Boot indépendants :

- **wallet-service** (port 8091) — comptes, authentification OTP + PIN + JWT, dépôt/retrait/paiement
- **service-app** (port 8092) — demandes de prestation, ressources, tâches

## Stack technique

- Java 17, Spring Boot 4.0.5
- Spring Web MVC, Spring Data JPA, Spring Security (Resource Server JWT maison)
- PostgreSQL (1 base par service)
- Apache Kafka (1 broker) + Outbox Pattern pour la communication asynchrone
- JWT : `jjwt` (génération côté wallet-service, validation sur les deux services, secret HMAC partagé)
- BCrypt pour le hash du PIN
- Lombok
- springdoc-openapi 3.1.1 (Swagger UI)
- Docker Compose (Postgres x2 + Kafka)

## Structure du code (identique dans les deux services)

```
controller/   endpoints REST, délègue au wrapper
wrapper/      orchestration du cas d'usage : appelle securite/helper/service, gère la transaction
helper/       calcul métier pur, sans accès DB
service/      persistance, @Transactional, requêtes repository
mapper/       conversion Entity <-> DTO
exception/    exceptions métier + GlobalExceptionHandler
securite/     JWT, PIN (BCrypt), OTP
model/        entités JPA
repository/   Spring Data JPA
dto/          requêtes/réponses
config/       sécurité, Kafka, OpenAPI
```

## Communication entre les deux services

- **REST synchrone** : `service-app` appelle `wallet-service` (`POST /api/transactions/paiement`) en transmettant le JWT de l'utilisateur.
- **Kafka asynchrone** : `wallet-service` publie l'event `paiement-effectue` (Outbox Pattern : écriture en base + event dans la même transaction, publication par un scheduler). `service-app` consomme cet event pour passer la prestation à `PAYEE` ou `ECHEC_PAIEMENT`.

## Points d'implémentation notables

- **Débit du solde atomique** : `UPDATE compte SET solde = solde - :montant WHERE id = :id AND solde >= :montant` (requête JPA dans `CompteRepository`), pas de lecture puis réécriture en mémoire.
- **Idempotence** : `Transaction.demandeId` est unique ; un paiement déjà traité pour un `demandeId` donné est rejeté avant tout débit.
- **Outbox Pattern** : table `outbox_event`, écrite dans la même transaction que l'opération métier, publiée vers Kafka par `OutboxPublisher` (`@Scheduled`).
- **JWT partagé** : `wallet-service` signe les tokens (HMAC, secret `APP_JWT_SECRET`), `service-app` les valide avec le même secret.

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
1. `Authentification` → `request-otp` → `verify-otp` → `create-pin` (le code OTP s'affiche dans les logs de wallet-service).
2. Copier le `token` retourné par `create-pin`.
3. Bouton **Authorize** (cadenas) → coller le token seul (sans `Bearer`).
4. Les autres endpoints deviennent utilisables directement depuis la page.

## Flux de test en curl

```bash
# 1. Demander un OTP
curl -X POST http://localhost:8091/api/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567"}'
# le code s'affiche dans les logs de wallet-service

# 2. Vérifier l'OTP
curl -X POST http://localhost:8091/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567", "code": "1234"}'

# 3. Créer le PIN -> récupère le JWT
curl -X POST http://localhost:8091/api/auth/create-pin \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567", "pin": "1234"}'
# { "token": "...", "numeroCompte": "CPT-..." }

TOKEN="<coller le token ici>"

# 4. Déposer de l'argent
curl -X POST http://localhost:8091/api/transactions/depot \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"montant": 50000}'

# 5. Créer une prestation
curl -X POST http://localhost:8092/api/prestations \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"titre": "Développement site web", "description": "Site vitrine", "montant": 20000}'
# { "id": 1, "statut": "EN_ATTENTE_PAIEMENT", ... }

# 6. Payer la prestation
curl -X POST http://localhost:8092/api/prestations/1/payer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"pin": "1234"}'

# 7. Vérifier le statut (laisser 1-2s le temps que l'event Kafka soit consommé)
curl http://localhost:8092/api/prestations/1
```

## Simplifications assumées

- 1 seul broker Kafka (pas de cluster à 3 nœuds).
- Secret JWT partagé entre les deux services (HMAC) plutôt qu'un IdP externe (Keycloak).
- Idempotence via `demandeId` unique sur `Transaction`, dérivé de l'id de la prestation (`PRESTATION-{id}`).
