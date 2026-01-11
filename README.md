# Projet : Système d'Entités Communicantes en Anneau

## Concept et objectif

Ce projet est un **projet academique sur les systemes distribues** - specifiquement un **reseau en anneau** (Token Ring).

**Ce n'est PAS une messagerie utilisateur.** C'est une infrastructure de communication distribuee.

### Principe de l'anneau

```
    E1 ──UDP──> E2
    ^           │
    │           v
    E4 <──UDP── E3
```

- Les entites forment une boucle : E1 → E2 → E3 → E4 → E1
- Un message fait le tour complet de l'anneau (chaque entite le relaie)
- Une nouvelle entite peut s'inserer entre deux existantes
- Si un noeud tombe, la multidiffusion permet de reconfigurer l'anneau

### Cas d'usage theoriques

| Cas | Description |
|-----|-------------|
| Communication distribuee | Plusieurs machines echangent des donnees |
| Tolerance aux pannes | Si un noeud tombe, l'anneau se reconfigure |
| Base pour microservices | Chaque entite = un service independant |

### Etat du projet

| Composant | Statut |
|-----------|--------|
| Moteur reseau | Fonctionnel |
| Interface utilisateur | Aucune (Main.java de test) |
| Applications | Squelette seulement |

---

## Description technique

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

### Detail du protocole de messages

| Type | Format | Description |
|------|--------|-------------|
| APPL | `APPL <idm> <id_app> <message>` | Message applicatif (donnees utilisateur) |
| WHOS | `WHOS <idm>` | Demande : "Qui est dans l'anneau ?" |
| MEMB | `MEMB <idm> <id_entite> <ip> <port>` | Reponse : "Je suis la, voici mon IP/port" |
| GBYE | `GBYE <idm> <ip_depart> <port_depart> <ip_suivant> <port_suivant>` | "Je quitte l'anneau" |
| EYBG | `EYBG <idm>` | Accuse de reception du depart |
| TEST | `TEST <idm> <ip_multicast> <port_multicast>` | Tester si l'anneau fonctionne |
| DOWN | `DOWN` | Arreter tout le reseau (multidiffusion) |

- `idm` : identifiant unique du message (8 caracteres)
- Taille max : 512 octets

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
