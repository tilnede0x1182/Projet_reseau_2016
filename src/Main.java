import java.util.Scanner;

/**
 * Point d'entree du systeme d'entites communicantes en anneau.
 * Demarre le serveur WebSocket et attend les commandes.
 */
class Main {
	static Utile utile;
	static WebSocketServer wsServer;
	static AnneauController controller;

	public static void main(String[] args) {
		System.out.println("===========================================");
		System.out.println("  SYSTEME D'ENTITES COMMUNICANTES EN ANNEAU");
		System.out.println("===========================================");
		System.out.println();

		int webPort = 6111;
		if (args.length > 0) {
			try {
				webPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Port invalide, utilisation du port par defaut: " + webPort);
			}
		}

		wsServer = new WebSocketServer(webPort);
		controller = new AnneauController(wsServer);
		wsServer.setController(controller);

		wsServer.start();

		System.out.println();
		System.out.println("Interface web disponible sur: http://localhost:" + webPort);
		System.out.println();

		Entite e1 = new Entite(5550, "127.0.0.1");
		e1.setAnneauController(controller);
		controller.addEntite(e1);

		System.out.println("Entite initiale creee: " + e1.identifiant);
		System.out.println();
		System.out.println("Commandes disponibles:");
		System.out.println("  add     - Ajouter une entite");
		System.out.println("  list    - Lister les entites");
		System.out.println("  whos    - Envoyer WHOS");
		System.out.println("  test    - Envoyer TEST");
		System.out.println("  quit    - Quitter");
		System.out.println();

		Scanner scanner = new Scanner(System.in);
		boolean running = true;

		while (running) {
			System.out.print("> ");
			String input = scanner.nextLine().trim().toLowerCase();

			switch (input) {
				case "add":
					addEntite();
					break;

				case "list":
					listEntites();
					break;

				case "whos":
					sendMessage("WHOS");
					break;

				case "test":
					sendMessage("TEST");
					break;

				case "quit":
				case "exit":
				case "q":
					running = false;
					break;

				case "":
					break;

				default:
					System.out.println("Commande inconnue: " + input);
			}
		}

		System.out.println("Arret du serveur...");
		wsServer.stop();
		scanner.close();
		System.out.println("Au revoir.");
	}

	/**
	 * Ajoute une nouvelle entite dans l'anneau.
	 */
	private static void addEntite() {
		int nextPort = 5550 + controller.getEntites().size();
		String nextId = "E" + (controller.getEntites().size() + 1);

		Entite newEntite = new Entite(nextId, nextPort, "127.0.0.1");
		newEntite.setAnneauController(controller);

		if (!controller.getEntites().isEmpty()) {
			Entite first = controller.getEntites().get(0);
			boolean inserted = newEntite.insertion("127.0.0.1", first.port_TCP);

			if (inserted) {
				controller.addEntite(newEntite);
				System.out.println("Entite " + nextId + " ajoutee et inseree dans l'anneau.");
			} else {
				System.out.println("Echec de l'insertion de " + nextId);
			}
		} else {
			controller.addEntite(newEntite);
			System.out.println("Entite " + nextId + " ajoutee (premiere entite).");
		}
	}

	/**
	 * Liste toutes les entites.
	 */
	private static void listEntites() {
		System.out.println("Entites dans l'anneau:");
		for (Entite e : controller.getEntites()) {
			System.out.println("  - " + e.identifiant + " (UDP:" + e.port_reception_UDP + ", TCP:" + e.port_TCP + ")");
		}
		System.out.println("Total: " + controller.getEntites().size() + " entite(s)");
	}

	/**
	 * Envoie un message via la premiere entite.
	 * @param type Type de message
	 */
	private static void sendMessage(String type) {
		if (controller.getEntites().isEmpty()) {
			System.out.println("Aucune entite disponible.");
			return;
		}

		Entite first = controller.getEntites().get(0);
		first.envoie_message(type);
		System.out.println("Message " + type + " envoye par " + first.identifiant);
	}
}
