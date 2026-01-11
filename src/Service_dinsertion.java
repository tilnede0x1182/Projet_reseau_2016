import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

class Service_dinsertion implements Runnable {
	Entite entite;
	Utile utile;
	boolean debug;
	boolean affiche_messages;
	private static ServerSocket srvSock;
	String type_dentite = "";

	public Service_dinsertion (Entite entite) {
		this.entite = entite;
		this.debug = entite.debug;
		this.affiche_messages = entite.affiche_messages;
	}

	public void run () {
		String ip = entite.adresse_ip_reception_UDP;
		int port = entite.port_TCP;
		String message="";
		boolean ok = true;
		int i = 1;
		int g=0;
		do {
			try {
				aff_debug(this+" : connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+".");
				srvSock = new ServerSocket(port);
				while (true) {
					try {
						aff_debug(this+" : boucle d'insertion, n°"+g);
						Socket comSock = srvSock.accept();
						BufferedReader comBR = new BufferedReader(new InputStreamReader(comSock.getInputStream()));
						PrintWriter comPW = new PrintWriter(new OutputStreamWriter(comSock.getOutputStream()));
						String message_welc = "WELC "+entite.ip_machine_suivante+" "+entite.port_d_ecoute_UDP_machine_suivante+" "
						+entite.adresse_panne_reseau_ipv4_multidifusion+" "+entite.port_de_multidifusion;
						aff_debug(this+" : Envoi du message "+message_welc);
						comPW.println(message_welc);
						comPW.flush();
						aff_debug(this+" : Attente d'un message pour l'insertion");
						message = comBR.readLine();
						aff_debug(this+" : \nMessage reçu : "+message);
						affnn_debug(this+" : Fin de l'insertion : ");
						boolean bool_tmp = analyse_info_entite_insertion(message);
						if (type_dentite.equals("SIMPLE")) {
							if (entite.entite_connectee) {
								aff_debug(this+" : Cette entité est déjà connectée : connection simple refusée.");
								bool_tmp = false;
							}
							if (bool_tmp) {
								entite.entite_connectee = true;
								comPW.println("ACKC");
							}
						}
						if (type_dentite.equals("DOUBLE")) {
							if (entite.entite_double) {
								aff_debug(this+" : Cette entité est déjà une entité doublée : connection en doublure refusée.");
								aff_debug(this+" : Envoi du message NOTC");
								comPW.println("NOTC");
								bool_tmp = false;
							}
							if (bool_tmp) {
								entite.entite_double = true;
								comPW.println("ACKD");
							}
						}
						comPW.flush();
						if (bool_tmp) {
							aff_debug("réussite.");
						}
						else {
							aff_debug("échec.");
						}
						//aff_debug(this+" : \nDéconnection des br, pw et socket");
						comBR.close();
						comPW.close();
						comSock.close();
						aff_debug(this+" : \nDéconnecté.\n");
					}
					catch (Exception e) {
						aff(this+" : Problème avec le client.");
					}
					g++;
				}
			}
			catch (Exception e) {
				aff(this+" : Problème lors de la création de la ServerSocket.");
				e.printStackTrace();
				ok = false;
			}
			aff_debug(this+" : connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+" : "+((ok)?"réussite":"échec")+".");
			utile.sleep(entite.getTemps_avant_nouvel_essai());
			i++;
		} while (!ok && i<=entite.getNombre_dessais());
	}

	/**
		Analyse le String renvoyé par l'entité contactée lors de 
		l'insertion.
	**/
	public boolean analyse_info_entite_insertion (String infos_entite_contactee) {
		boolean res = true;
		String [] infos = infos_entite_contactee.split(" ");
		if (infos==null) return false;
		if (infos.length<3) return false;
		if (!utile.isInteger(infos[2])) return false;

		if (infos[0].equals("NEWC")) {
			type_dentite = "SIMPLE";
			String adresse_suivant = infos[1];
			if (utile.verifie_ip_v4(adresse_suivant))
				entite.ip_machine_suivante = adresse_suivant;
			else res = false;
			int port_suivant = Integer.parseInt(infos[2]);
			if (utile.verifie_port(port_suivant))
				entite.port_d_ecoute_UDP_machine_suivante = port_suivant;
			else res = false;
			return res;
		}
		if (infos[0].equals("DUPL")) {
			type_dentite = "DOUBLE";
			if (!utile.isInteger(infos[2])) return false;
			String ip_diff = infos[3];
			int port_diff = Integer.parseInt(infos[4]);
			entite.entite_doublure = new Entite(ip_diff, port_diff, entite.insertion_simple);
		}
		return res;
	}

	public String toString () {
		String res="";
		res = entite+" : Service d'insertion";
		return res;
	}

// ################# Fonctions utilitaires - Autres fonctions #################### //

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
