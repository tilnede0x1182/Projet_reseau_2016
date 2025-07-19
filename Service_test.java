import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

class Service_test implements Runnable {
	Entite entite;
	Utile utile;
	boolean debug;

	public Service_test (Entite entite) {
		this.entite = entite;
		this.debug = entite.debug;
	}

	public void run () {
		String ip = entite.adresse_ip_reception_UDP;
		int port = entite.port_reception_UDP;
		String message="";
		try {
			//utile.sleep(1); // pour le test
			utile.sleep(500);
			if (entite.getTest()) {
				entite.envoie_message("DOWN");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String toString () {
		String res="";
		res = "Service test de l'entite "+entite.identifiant;
		return res;
	}


// ################# Fonctions utilitaires - Autres fonctions #################### //

	public void aff_debug (Object s0) {
		if (debug)
			System.out.println(s0);
	}

	public void affnn_debug (Object s0) {
		if (debug)
			System.out.print(s0);
	}

	public void aff (Object s0) {
		System.out.println(s0);
	}

	public void affnn (Object s0) {
		System.out.print(s0);
	}
}
