# Sama Wallet — Flutter

Application Flutter Android et web reliée aux deux services Spring Boot du dossier voisin.

Voir [README-DEMO.md](../README-DEMO.md) pour lancer et déployer le projet.

```bash
flutter pub get
flutter analyze
flutter test
flutter build web --release
```

Pour actualiser les fichiers statiques du déploiement Render : `bash ../scripts/prepare-render-web.sh` depuis la racine du projet. Sur émulateur Android, les API locales utilisent `10.0.2.2:8091` et `10.0.2.2:8092` ; les adresses sont configurables sur l'écran de connexion.
