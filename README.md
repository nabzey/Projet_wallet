# Projet Wallet

Deux microservices Spring Boot indépendants + une application Flutter :

- **wallet-service** (port 8091) — comptes, authentification OTP + PIN + JWT, dépôt/retrait/paiement
- **service-app** (port 8092) — demandes de prestation, ressources, tâches
- **wallet_mobile** — application Flutter (web + Android) consommant les deux API

## Compte de démonstration

Pour tester l'app sans repasser par le flux OTP à chaque fois (tant que les volumes Docker Postgres ne sont pas supprimés) :

| Champ | Valeur |
|---|---|
| Téléphone | `+221770000000` |
| PIN | `1234` |
| Numéro de compte | `CPT-C508AC97-B` |
| Solde | 85 000 FCFA (dépôt de 100 000 puis une prestation payée de 15 000) |

Se connecter directement avec `POST /api/auth/login` (ou l'écran "Connexion" dans l'app) — pas besoin de refaire l'OTP pour ce compte.

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
- **Vérification OTP en 2 temps** : `verify-otp` ne crée plus directement le PIN — il retourne un `verificationToken` (JWT de courte durée, 5 min) qui doit être fourni à `create-pin`. Empêche de définir un PIN sans avoir prouvé la possession du code OTP dans la même session.
- **`POST /api/auth/login`** : connexion directe par téléphone + PIN pour un utilisateur déjà inscrit, sans repasser par l'OTP.
- **Throttling OTP** : au-delà de `app.otp.max-tentatives`, `verify-otp` répond `429 Too Many Requests` plutôt qu'une simple erreur 400.
- **`app.kafka.enabled`** (défaut `true`) : désactive `OutboxPublisher`/`PaiementListener` (`@ConditionalOnProperty`) et bascule `PaiementWrapper`/`PrestationWrapper` sur un traitement synchrone — utile pour un déploiement sans broker Kafka (ex. hébergement gratuit).

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

# 3. Récupère un verificationToken (preuve que l'OTP a été vérifié)
curl -X POST http://localhost:8091/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567", "code": "1234"}'
# { "verificationToken": "..." }

# 4. Créer le PIN avec ce token -> récupère le JWT
curl -X POST http://localhost:8091/api/auth/create-pin \
  -H "Content-Type: application/json" \
  -d '{"telephone": "+221771234567", "pin": "1234", "verificationToken": "<coller ici>"}'
# { "token": "...", "numeroCompte": "CPT-..." }

TOKEN="<coller le token ici>"
# Pour une connexion ultérieure (compte déjà créé) :
# curl -X POST http://localhost:8091/api/auth/login -H "Content-Type: application/json" -d '{"telephone": "+221771234567", "pin": "1234"}'

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

## Frontend Flutter (`wallet_mobile`)

```bash
cd wallet_mobile
flutter pub get
flutter run -d chrome --dart-define=WALLET_URL=http://localhost:8091 --dart-define=SERVICE_URL=http://localhost:8092
```

- Thème bleu fintech défini en haut de `lib/main.dart` (constantes `ink`, `blue`, `accent`).
- `lib/api.dart` gère la connexion : web (`kIsWeb`, config via `/config.json` ou `--dart-define`) et Android émulateur (`10.0.2.2`).
- Sans proxy configuré, les appels directs vers `localhost:8091`/`8092` fonctionnent en dev car `APP_CORS_ORIGINS` autorise `http://localhost:8080` par défaut — ajuster cette variable côté backend si `flutter run` choisit un autre port.

## Vérification de bout en bout

Le script `smoke_test.py` (36 requêtes HTTP réelles : OTP, verificationToken, login, throttling 429, dépôt/retrait, paiement confirmé par Kafka, idempotence, ressources/tâches) valide que les deux services fonctionnent ensemble :

```bash
WALLET_LOG=/tmp/wallet-service.log python3 scripts/smoke_test.py
```

## Simplifications assumées

- 1 seul broker Kafka (pas de cluster à 3 nœuds).
- Secret JWT partagé entre les deux services (HMAC) plutôt qu'un IdP externe (Keycloak).
- Idempotence via `demandeId` unique sur `Transaction`, dérivé de l'id de la prestation (`PRESTATION-{id}`).
