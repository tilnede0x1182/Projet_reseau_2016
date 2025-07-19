class Main {
	static Utile utile;
	public static void main (String [] args) {
		Entite e1 = new Entite (5550, "127.0.0.1");
		Entite e2 = new Entite ("E3", 5551, "127.0.0.1");
		Entite e3 = new Entite ("E4", 5552, "127.0.0.1");
		Entite e4 = new Entite ("E5", 5553, "127.0.0.1");
		Entite e5 = new Entite ("E2", 5554, "127.0.0.1");
		e1.aff_debug("\n\nFin de la création des entités.\n\n");
		//e1.insertion("127.0.0.1", 5550);
		e5.insertion("127.0.0.1", 5550);
		//utile.sleep(10);
		//e2.insertion_double("127.0.0.1", 5550);
		//e3.insertion_double("127.0.0.1", 5550);
		//e4.insertion("127.0.0.1", 5550);

		e1.aff_debug("\n\nFin des insertions\n\n");

		//e5.envoie_message("GBYE");
		//e1.envoie_message("Bonjour tout le monde :-)", new Application().getId_app());
		//e1.envoie_message("WHOS");
		//e1.envoie_message("TEST");
		//e1.envoie_message("DOWN");
	}
}
