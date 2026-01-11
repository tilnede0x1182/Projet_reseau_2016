class Message {
	private Utile utile;

	private String idm = "";
	private String message = "";

	public Message () {
		idm = "M"+utile.genere_identifiant(7);
	}

	public Message (String message) {
		idm = utile.genere_identifiant(8);
		this.message = message;
	}

	public Message (String message, String idm) {
		this.idm = idm;
		this.message = message;
	}

	public String toString () {
		String res="";
		res = "Message : \nId : "+idm+"\nMessage : "+message;
		return res;
	}

	public String getIdm() {
		return idm;
	}

	public String getMessage() {
		return message;
	}
}
