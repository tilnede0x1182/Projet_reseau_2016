# Projet : Système d’Entités Communicantes en Anneau

- Ce projet implémente un réseau d'entités capables de communiquer en anneau via UDP et TCP.
- Chaque entité peut envoyer, recevoir et relayer des messages dans le réseau.
- L'architecture supporte des insertions simples ou doubles d'entités dans l'anneau.
- Les entités peuvent s'auto-configurer avec des ports et adresses multicast générés dynamiquement.
- Les messages échangés respectent un format structuré incluant un identifiant unique.
- Il existe un mécanisme de multidiffusion pour certains types de messages critiques.
- Le projet inclut une gestion minimale d’applications, installées ou désinstallées par les entités.
- Des services en threads indépendants assurent la réception UDP, la gestion de la multidiffusion et l'insertion.
- Le mode debug est configurable pour faciliter le suivi des échanges réseau.
- Le code est entièrement écrit en Java, sans bibliothèque externe.

## Technologies utilisées

- Java 17 (ou compatible avec le code source fourni)
- Aucun framework externe (standard Java SE uniquement)
- Communication réseau :
  - UDP : `DatagramSocket`, `DatagramPacket`, `MulticastSocket`
  - TCP : `Socket`, `ServerSocket`
- Threads natifs Java

## Fonctionnalités principales

- **Réseau en anneau dynamique**
  - Insertion simple (une entité prend place entre deux autres)
  - Insertion double (une entité crée une doublure)
  - Mise à jour automatique des pointeurs réseau
- **Communication réseau**
  - Transmission UDP entre entités voisines
  - Réception de messages en multidiffusion
  - Relai conditionnel des messages (évite doublons)
- **Protocoles internes**
  - Format de message propre avec ID unique (`idm`)
  - Types de messages : `APPL`, `WHOS`, `MEMB`, `GBYE`, `EYBG`, `TEST`, `DOWN`
- **Applications**
  - Installation / désinstallation d'une application par ID
  - Vérification de la présence d'une application dans une entité
- **Multithreading**
  - Threads distincts pour l’insertion (`Service_dinsertion`)
  - Réception UDP standard (`Service_messages`)
  - Réception UDP en multidiffusion (`Service_messages_multidiffusion`)
  - Test automatisé de chute réseau (`Service_test`)
- **Utilitaires**
  - Génération aléatoire d’IDs, ports, adresses multicast
  - Vérification de format d’adresse IP et de ports
  - Complétion automatique des adresses IP
- **Debug**
  - Affichage des connexions et messages selon les flags `debug` et `affiche_messages`
- **Main**
  - Script de test simple créant plusieurs entités et testant leur insertion

## Compilation et exécution

```bash
javac *.java && java Main
```

### Configuration (dans le fichier `Entite.java`)

- **Variable `boolean debug`**
  Permet d'afficher les messages relatifs aux connexions réseau.

- **Variable `boolean affiche_messages`**
  Permet d'afficher les messages transmis via UDP sur l'anneau.
```
