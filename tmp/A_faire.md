# À faire

## 1. Compréhension rapide
- `Entite.java` modélise chaque nœud de l’anneau : génération d’ID, ports UDP/TCP, incrustation d’une doublure, insertion simple/double et pilotage des threads de service.
- `Message.java` définit le format des messages (WHOS, MEMB, GBYE, etc.), leur sérialisation et la validation de forme.
- `Service_messages`, `Service_messages_multidiffusion` et `Service_dinsertion` sont des threads dédiés à la réception UDP, à la multidiffusion et au dialogue d’insertion respectivement.
- `Service_test` simule des tests (pannes, temps d’attente) pour vérifier la résilience du réseau ; `Service_fichiers` gère la persistance éventuelle.
- `Utile.java` regroupe les helpers : validation d’IP, génération d’adresses multicast, tirage aléatoire d’identifiants, temporisations (`sleep`).
- `Main.java` assemble une topologie d’entités pour démontrer l’insertion et les échanges (arrêt propre, messages debug, etc.).

## 2. Bugs à corriger
- `Service_messages.run()` crée un `DatagramSocket dso = new DatagramSocket(port)` mais ne le ferme jamais (même en cas d’exception). Après un premier échec, le port reste occupé et tout redémarrage échoue. Envelopper la boucle `while(true)` dans un `try-with-resources` ou fermer explicitement le socket dans un bloc `finally`.
- `messages_recus` et `applications_installees` sont des `HashSet` partagés par plusieurs threads sans synchronisation. Les méthodes `Service_messages` et `Service_messages_multidiffusion` itèrent dessus pendant que d’autres threads écrivent, ce qui déclenche des `ConcurrentModificationException`. Protéger ces structures (via `Collections.synchronizedSet` ou `ConcurrentHashMap`) est nécessaire.
- `Entite.presentation_entite` utilise `InetAddress.getLocalHost()` à chaque création, mais n’attrape que `UnknownHostException`. Sur une machine sans DNS local, l’application s’arrête brutalement. Prévoir un fallback (127.0.0.1) évite ces arrêts.

## 3. DRY en priorité
- Les classes `Service_messages` et `Service_messages_multidiffusion` dupliquent toute la plomberie de log (`aff_message`, `aff_debug`, etc.). Un parent abstrait `Service_reseau` réduirait la duplication.
- Les vérifications d’IP/port (`verifie_ip_v4`, `verifie_port`) sont recopiées dans plusieurs méthodes d’`Entite`. Appeler directement `Utile` ou encapsuler ces validations dans un objet « Adresse » simplifierait l’API.

## 4. Sécurité
- Les messages reçus via UDP sont directement passés à `entite.renvoie_message` sans contrôle d’origine. Ajouter une vérification (liste blanche d’IP, signature simple) empêcherait un intrus d’envoyer `DOWN` et de faire tomber l’anneau.
- Les ports UDP/TCP sont générés aléatoirement entre 0 et 9999. Sans vérification des plages réservées (<1024) ou déjà utilisées par le système, l’application peut tenter d’écouter sur un port protégé et échouer. Limiter la génération à une plage dédiée (ex. 50000-60000) et valider `verifie_port_creation` en conséquence.
