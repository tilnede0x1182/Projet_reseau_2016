# Projet : Système d'Entités Communicantes en Anneau

## Concept et objectif

Implémentation d’un réseau en anneau en Java.

L’architecture repose sur une topologie circulaire logique : chaque nœud maintient uniquement la référence de son successeur. Les messages sont relayés séquentiellement de proche en proche jusqu’à revenir à l’émetteur, ce qui garantit la continuité du cycle. Les échanges applicatifs utilisent UDP pour la transmission ; l’intégration d’un nouveau nœud repose sur TCP afin d’assurer une phase d’insertion fiable et contrôlée.

Le protocole applicatif est explicitement défini et typé :
- APPL : transport de données applicatives.
- WHOS : requête d'identification des participants.
- MEMB : réponse à WHOS avec identifiant et adresse réseau.
- TEST : vérification de la cohérence de l'anneau.
- GBYE : notification de retrait volontaire.
- EYBG : accusé de réception du retrait.
- DOWN : arrêt global du réseau via multidiffusion.

Fonctionnalités assurées :
- insertion dynamique sans interruption globale du cycle,
- retrait propre avec mise à jour des voisins directs,
- arrêt coordonné du réseau par message de multidiffusion.

Le projet met en œuvre la programmation réseau bas niveau (sockets UDP, TCP, multidiffusion IP), la gestion concurrente par threads dédiés (écoute, supervision, insertion) et la conception d’un protocole distribué structuré avec identifiants uniques.

Objectif : maîtriser la mise en œuvre concrète d’une communication distribuée et la gestion de cohérence dans une architecture en boucle sans autorité centrale.

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
- La multidiffusion permet d'arreter l'anneau (DOWN)

### Cas d'usage theoriques

| Cas | Description |
|-----|-------------|
| Communication distribuee | Plusieurs machines echangent des donnees |
| Arret coordonne | La multidiffusion permet d'arreter tout le reseau |
| Base pour microservices | Chaque entite = un service independant |

### Etat du projet

| Composant | Statut |
|-----------|--------|
| Moteur reseau | Fonctionnel |
| Interface web | Fonctionnelle (visualisation + controles) |
| CLI | Fonctionnelle (Main.java) |
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
make compile_run   # Compile et execute
```

Autres commandes :
```bash
make compile       # Compile dans build/
make run           # Execute (necessite compilation prealable)
make javadoc       # Genere la documentation
make clean         # Supprime build/ et javadoc/
```

---

## Interface Web

Une interface web permet de visualiser et controler l'anneau en temps reel.

### Lancement

```bash
make compile_run
```

Puis ouvrir : http://localhost:6111

### Fonctionnalites

| Fonction | Description |
|----------|-------------|
| Visualisation | Graphe circulaire des entites connectees |
| Messages | Log temps reel des messages circulants |
| Controles | Ajouter/retirer entites, envoyer WHOS/TEST/DOWN |

### Architecture

```
Navigateur <-- WebSocket --> Serveur Java (port 6111)
                                  |
                             Anneau UDP/TCP
```

### Configuration (dans le fichier `Entite.java`)

- **Variable `boolean debug`**
  Permet d'afficher les messages relatifs aux connexions réseau.

- **Variable `boolean affiche_messages`**
  Permet d'afficher les messages transmis via UDP sur l'anneau.
