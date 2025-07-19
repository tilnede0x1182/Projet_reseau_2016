import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

class Service_messages_multidiffusion implements Runnable {
	Entite entite;
	Utile utile;
	boolean debug;
	boolean affiche_messages;

	public Service_messages_multidiffusion (Entite entite) {
		this.entite = entite;
		this.debug = entite.debug;
		this.affiche_messages = entite.affiche_messages;
	}

	public void run () {
		String ip = entite.adresse_panne_reseau_ipv4_multidifusion;
		int port = entite.port_de_multidifusion;
		String message="";
		boolean ok=true;
		int i=1;
		do {
			try {
				aff_debug(""+this+" : connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+".");
				MulticastSocket mso = new MulticastSocket(port);
				mso.joinGroup(InetAddress.getByName(ip));
				byte [] data = new byte[100];
				DatagramPacket paquet = new DatagramPacket(data,data.length);
				while(true) {
					mso.receive(paquet);
					message = new String(paquet.getData(), 0, paquet.getLength());
					aff_message(""+this+" : \nMessage reçu : "+message);
					entite.renvoie_message(message, true);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				ok = false;
			}
			aff_debug(""+this+" : connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+" : "+((ok)?"réussite":"échec")+".");
			utile.sleep(entite.getTemps_avant_nouvel_essai());
			i++;
		} while (!ok && i<=entite.getNombre_dessais());
	}

	public String toString () {
		String res="";
		res = entite+" : Service des messages en multidiffusion";
		return res;
	}


// ################# Fonctions utilitaires - Autres fonctions #################### //

	public void aff_message (Object s0) {
		if (affiche_messages || debug)
			System.out.println(s0);
	}

	public void affnn_message (Object s0) {
		if (affiche_messages || debug)
			System.out.print(s0);
	}

	public void aff_debug (Object s0) {
		if (debug && !affiche_messages)
			System.out.println(s0);
	}

	public void affnn_debug (Object s0) {
		if (debug && !affiche_messages)
			System.out.print(s0);
	}

	public void aff (Object s0) {
		System.out.println(s0);
	}

	public void affnn (Object s0) {
		System.out.print(s0);
	}
}
