import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

class Service_messages implements Runnable {
	Entite entite;
	Utile utile;
	boolean debug;
	boolean affiche_messages;

	public Service_messages (Entite entite) {
		this.entite = entite;
		this.debug = entite.debug;
		this.affiche_messages = entite.affiche_messages;
		this.utile = entite.utile;
	}

	public void run () {
		String ip = entite.adresse_ip_reception_UDP;
		int port = entite.port_reception_UDP;
		String message="";
		boolean ok = true;
		int i=1;
		do {
			try (DatagramSocket dso = new DatagramSocket(port)) {
				aff_debug(""+this+" : connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+".");
				byte [] data = new byte[1024];
				DatagramPacket paquet = new DatagramPacket(data,data.length);
				while (true) {
					dso.receive(paquet);
					message = new String(paquet.getData(), 0, paquet.getLength());
					InetSocketAddress ia = (InetSocketAddress)paquet.getSocketAddress();
					String sourceIp = ia.getAddress().getHostAddress();
					if (!entite.origineAutorisee(sourceIp)) {
						aff_debug(""+this+" : message ignoré, origine non autorisée "+sourceIp);
						continue;
					}
					aff_message(""+this+" : \nMessage reçu : "+message);
					affnn_message(""+this+" : Reçu de la machine "+ia.getHostName());
					aff_message(", depuis le port "+ia.getPort()+".");
					entite.renvoie_message(message, false);
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
		res = entite+" : Service des messages";
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
