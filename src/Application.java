class Application {
	Utile utile;

	private String id_app;

	public Application () {
		id_app = utile.genere_identifiant(8);
	}

	public String getId_app () {
		return id_app;
	}
}
