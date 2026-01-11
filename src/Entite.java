import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class Entite {
	Utile utile;
	static Utile utile_static;
	static final int PORT_MIN = 1025;
	static final int PORT_MAX = 65535;
	boolean debug = true;
	/**
		N'affiche que les messages relatifs à l'envoie et la réception de messages
	**/
	boolean affiche_messages = false;
	private boolean test = false;
	private int nombre_dessais = 1;
	/** en secondes/100 (centièmes de secondes) **/
	private int temps_avant_nouvel_essai = 2;
	private int taille_des_id_entite = 2;

	String identifiant;
	String adresse_ip_reception_UDP;
	int port_reception_UDP;
	int port_TCP;
	String ip_machine_suivante;
	int port_d_ecoute_UDP_machine_suivante;
	String adresse_panne_reseau_ipv4_multidifusion;
	int port_de_multidifusion;
	boolean entite_connectee = false;
	boolean entite_double = false;

	Thread insertion_simple;
	Thread service_messages;
	Thread service_messages_multidiffusion;

	/**
		Cette entité est instanciée avec le contructeur approprié lorsque l'entité se dédouble.
	**/
	Entite entite_doublure = null;

	BufferedReader br;

	Set<String> messages_recus;
	Set<String> applications_installees;

	Thread service_test = null;

	/** Controleur pour interface web **/
	AnneauController anneauController = null;

	/**
	 * Definit le controleur d'anneau pour les notifications web.
	 * @param controller Controleur
	 */
	public void setAnneauController(AnneauController controller) {
		this.anneauController = controller;
	}

	/**
	 * Notifie le controleur d'un message.
	 * @param type Type de message
	 * @param content Contenu
	 */
	private void notifyController(String type, String content) {
		if (anneauController != null) {
			anneauController.notifyMessage(this, type, content);
		}
	}

	public Entite (int port_TCP, String ip_machine_suivante) {
		utile_constructeur(port_TCP, ip_machine_suivante);
	}

	public Entite (int port_TCP, String ip_machine_suivante, int port_d_ecoute_UDP_machine_suivante) {
		this(port_TCP, ip_machine_suivante);
		this.port_d_ecoute_UDP_machine_suivante = port_d_ecoute_UDP_machine_suivante;
		utile.sleep(1);
		insertion(ip_machine_suivante, port_d_ecoute_UDP_machine_suivante);
	}

	public Entite (int taille_id_entites, int port_TCP, String ip_machine_suivante) {
		this.taille_des_id_entite = taille_id_entites;
		utile_constructeur(port_TCP, utile_static.complete_ip(ip_machine_suivante));
	}

	public Entite (String id, int port_TCP, String ip_machine_suivante) {
		utile_constructeur(port_TCP, utile_static.complete_ip(ip_machine_suivante), id);
	}

	public Entite (int port_TCP, String ip_machine_suivante, 
	int port_d_ecoute_UDP_machine_suivante, String adresse_panne_reseau_ipv4_multidifusion) {
		this(port_TCP, utile_static.complete_ip(ip_machine_suivante), port_d_ecoute_UDP_machine_suivante);
		this.adresse_panne_reseau_ipv4_multidifusion = utile_static.complete_ip(adresse_panne_reseau_ipv4_multidifusion);
	}

	/**
		Constructeur pour une doublure (est instancié par l'entité en train de se doubler).
	**/
	public Entite (String ip_diff, int port_diff, Thread insertion_simple) {
		entite_double = true;
		utile_constructeur_id_port_deja_choisis("");
		this.port_reception_UDP = utile.genere_numero_de_port (PORT_MIN, PORT_MAX, utile.getPorts_deja_choisis());
		aff_debug(this+" : port_reception_UDP : "+port_reception_UDP);
		this.port_d_ecoute_UDP_machine_suivante = port_reception_UDP;
		this.port_TCP = port_TCP;
		this.ip_machine_suivante = utile_static.complete_ip(ip_machine_suivante);
		adresse_panne_reseau_ipv4_multidifusion = utile_static.complete_ip(ip_diff);
		port_de_multidifusion = port_diff;
		this.insertion_simple = insertion_simple;
		service_messages = new Thread (new Service_messages(this));
		service_messages.start();
		service_messages_multidiffusion = new Thread (new Service_messages_multidiffusion(this));
		service_messages_multidiffusion.start();
		if (affiche_messages)
			presentation_entite(true);
	}

	public void utile_constructeur_id_port_deja_choisis (String id) {
		if (utile.getPorts_deja_choisis()==null) {
			aff_debug("Réinitialisation de ports_deja_choisis.");
			utile.setPorts_deja_choisis(new HashSet<Integer>());
		}
		messages_recus = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		applications_installees = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		boolean ok_id = false;
		if (id!=null)
			if (!id.isEmpty())
				ok_id = true;
		if (ok_id) this.identifiant = id;
		else {
			identifiant = "E"+utile.genere_identifiant(taille_des_id_entite-1);
			aff_debug("Création de l'entité "+identifiant);
		}
		try {
			adresse_ip_reception_UDP = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e) {
			adresse_ip_reception_UDP = "127.0.0.1";
			aff_debug("Impossible de résoudre l'adresse locale, utilisation de 127.0.0.1");
		}
	}

	public boolean origineAutorisee(String ip) {
		if (ip==null) return false;
		String normalisee = utile_static.complete_ip(ip);
		if (normalisee.equals(adresse_ip_reception_UDP)) return true;
		if (ip_machine_suivante!=null && normalisee.equals(utile_static.complete_ip(ip_machine_suivante))) return true;
		if (adresse_panne_reseau_ipv4_multidifusion!=null && normalisee.equals(utile_static.complete_ip(adresse_panne_reseau_ipv4_multidifusion))) return true;
		return false;
	}

	public void utile_constructeur (int port_TCP, String ip_machine_suivante) {
		utile_constructeur(port_TCP, ip_machine_suivante, "");
	}

	public void utile_constructeur (int port_TCP, String ip_machine_suivante, String id) {
		utile_constructeur_id_port_deja_choisis(id);
		this.port_reception_UDP = utile.genere_numero_de_port (PORT_MIN, PORT_MAX, utile.getPorts_deja_choisis());
		aff_debug(this+" : port_reception_UDP : "+port_reception_UDP);
		this.port_d_ecoute_UDP_machine_suivante = port_reception_UDP;
		this.port_TCP = port_TCP;
		this.ip_machine_suivante = utile_static.complete_ip(ip_machine_suivante);
		adresse_panne_reseau_ipv4_multidifusion = utile.genere_adresse_ipv4_multidiffusion();
		port_de_multidifusion = utile.genere_numero_de_port (PORT_MIN, PORT_MAX, utile.getPorts_deja_choisis());

		insertion_simple = new Thread (new Service_dinsertion(this));
		insertion_simple.start();
		service_messages = new Thread (new Service_messages(this));
		service_messages.start();
		service_messages_multidiffusion = new Thread (new Service_messages_multidiffusion(this));
		service_messages_multidiffusion.start();
		if (affiche_messages)
			presentation_entite(false);
	}

// ################################# Insertions ################################## //
// ####*************************** Insertion simple **************************#### //

	/**
		Analyse le String renvoyé par l'entité contactée lors de 
		l'insertion.
	**/
	public boolean analyse_info_entite_contactee (String infos_entite_contactee) {
		boolean res = true;
		String [] infos = infos_entite_contactee.split(" ");
		if (infos==null) return false;
		if (infos.length<5) return false;
		if (!utile.isInteger(infos[2]) || !utile.isInteger(infos[4])) return false;

		String adresse_suivant = infos[1];
		if (utile.verifie_ip_v4(adresse_suivant))
			ip_machine_suivante = adresse_suivant;
		else res = false;
		int port_suivant = Integer.parseInt(infos[2]);
		if (utile.verifie_port(port_suivant))
			port_d_ecoute_UDP_machine_suivante = port_suivant;
		else res = false;
		String adresse_mutlidiffusion = infos[3];
		if (utile.verifie_ip_multidiffusion(adresse_mutlidiffusion))
			adresse_panne_reseau_ipv4_multidifusion = adresse_mutlidiffusion;
		else res = false;
		int port_multidiffusion = Integer.parseInt(infos[4]);
		if (utile.verifie_port(port_multidiffusion))
			port_de_multidifusion = port_multidiffusion;
		else res = false;
		return res;
	}

	/**
		Tente de s'insérer (de manière simple) entre deux entités qu'on nommera ici 1 et 2.
		Prend en argument le port TCP et l'adresse ip de l'entité 1.
	**/
	public boolean insertion (String ip, int port) {
		int i=1;
		boolean res = true;
		boolean ok = true;
		do {
			res = true;
			try{
				aff_debug(this+" : Connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+".");
				Socket socket = new Socket(ip, port);
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				String infos_entite_contactee=null;
				String s0 = br.readLine();
				aff_debug(""+this+" tentant de s'insérer :\nMessage reçu : "+s0);
				if (s0!=null) {
					infos_entite_contactee = s0;
				}
				if (infos_entite_contactee==null) return false;
				if (!analyse_info_entite_contactee(infos_entite_contactee))
					res = false;
				pw.println("NEWC "+adresse_ip_reception_UDP+" "+port_reception_UDP);
				pw.flush();
				s0 = br.readLine();
				aff_debug(this+" tentant de s'insérer :\nMessage reçu : "+s0);
				//aff_debug(this+" : Entité tentant de s'insérer :\nDéconnection");
				pw.close();
				br.close();
				socket.close();
				//aff_debug(this+" : Entité tentant de s'insérer :\nDéconnectée");
				return res;
			}
			catch (Exception e){
				System.out.println(e);
				e.printStackTrace();
				ok = false;
			}
			aff_debug(""+this+" : connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+" : "+((ok)?"réussite":"échec")+".");
			utile.sleep(temps_avant_nouvel_essai);
			i++;
		} while (!ok && i<=nombre_dessais);
		return ok;
	}

// ####*************************** Insertion double **************************#### //

	/**
		Tente de s'insérer en double entre deux entités qu'on nommera ici 1 et 2.
		Prend en argument le port TCP et l'adresse ip de l'entité 1.
	**/
	public boolean insertion_double (String ip, int port) {
		int i=1;
		boolean res = true;
		boolean ok = true;
		do {
			res = true;
			try{
				aff_debug(this+" : Connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+".");
				Socket socket = new Socket(ip, port);
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				String infos_entite_contactee=null;
				String s0 = br.readLine();
				aff_debug(""+this+" tentant de s'insérer :\nMessage reçu : "+s0);
				if (s0!=null) {
					infos_entite_contactee = s0;
				}
				if (infos_entite_contactee==null) return false;
				if (!analyse_info_entite_contactee(infos_entite_contactee))
					res = false;
				pw.println("DUPL "+adresse_ip_reception_UDP+" "+port_reception_UDP+" "+adresse_panne_reseau_ipv4_multidifusion+" "+port_de_multidifusion);
				pw.flush();
				s0 = br.readLine();
				aff_debug(""+this+" tentant de s'insérer :\nMessage reçu : "+s0);
				//aff_debug(this+" : Entité tentant de s'insérer :\nDéconnection");
				pw.close();
				br.close();
				socket.close();
				//aff_debug(this+" : Entité tentant de s'insérer :\nDéconnectée");
				return res;
			}
			catch (Exception e){
				System.out.println(e);
				e.printStackTrace();
				ok = false;
			}
			aff_debug(""+this+" : connection à l'adresse ip "+ip+" et au port "+port+", essai n°"+i+" : "+((ok)?"réussite":"échec")+".");
			utile.sleep(temps_avant_nouvel_essai);
			i++;
		} while (!ok && i<=nombre_dessais);
		return ok;
	}

// ########################### Gestion des messages ############################## //

	public boolean envoie_message_UDP (String ip, int port, String message_tmp, String idm) {
		Message message = new Message(message_tmp, idm);
		try {
			if (!utile.verifie_message_correct(message.getMessage())) {
				aff(""+this+" : Imossible d'envoyer le message car sa taille est 512 octets");
				return false;
			}
			aff_message(""+this+" : Envoie_d'un message à la machine suivante sur l'anneau (ip : "+ip+", port : "+port+")");
			aff_debug(""+this+" : \n"+message);
			aff_message(""+this+" : \n"+message.getMessage());
			aff_debug(""+this+" : Création de la datagramSocket");
			DatagramSocket dso = new DatagramSocket();
			byte [] data;
			data = message.getMessage().getBytes();
			aff_debug(""+this+" : Connection au port");
			InetSocketAddress ia = new InetSocketAddress(ip, port);
			DatagramPacket paquet = new DatagramPacket(data,data.length,ia);
			aff_debug(""+this+" : Envoi du packet");
			dso.send(paquet);
			aff_debug(""+this+" : Packet envoyé.");
			notifyController("SEND", message.getMessage());
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean envoie_message_multidiffusion (String message_tmp, String idm) {
		return envoie_message_UDP(adresse_panne_reseau_ipv4_multidifusion, port_de_multidifusion, message_tmp, idm);
	}

	public boolean envoie_message_UDP_machine_suivante (String message_tmp, String idm) {
		return envoie_message_UDP(ip_machine_suivante, port_d_ecoute_UDP_machine_suivante, message_tmp, idm);
	}

	public String formate_message (String type, String message_tmp, String id_app) {
		Message message = new Message(message_tmp);
		String idm = message.getIdm();

		if (type.equals("APPL")) {
			return "APPL"+" "+idm+" "+id_app+" "+message_tmp;
		}

		if (type.equals("WHOS")) {
			return "WHOS"+" "+idm;
		}
		if (type.equals("MEMB")) {
			return "MEMB"+" "+idm+" "+identifiant+" "+adresse_ip_reception_UDP+" "+port_reception_UDP;
		}
		if (type.equals("GBYE")) {
			return "GBYE"+" "+idm+" "+adresse_ip_reception_UDP+" "+port_reception_UDP+" "+ip_machine_suivante+" "
			+port_d_ecoute_UDP_machine_suivante;
		}
		if (type.equals("EYBG")) {
			return "EYBG"+" "+idm;
		}
		if (type.equals("TEST")) {
			test = true;
			service_test = new Thread (new Service_test(this));
			service_test.start();
			return "TEST"+" "+idm+" "+adresse_panne_reseau_ipv4_multidifusion+" "+port_de_multidifusion;
		}
		if (type.equals("DOWN")) {
			return "DOWN";
		}
		return "";
	}

	public boolean renvoie_message (String message, boolean multidiffusion) {
		boolean res = true;
		String [] message_tab = null;
		String ip="";
		String port="";
		int portInt=0;
		if (message==null) return false;
		if (message.isEmpty()) return false;
		if (message.length()<4) {
			aff(""+this+" : Longueur d'un message < 4 : ne respecte pas le format.");
			return false;
		}
		String type = message.substring(0, 4);
		if (type.equals("DOWN")) {
			aff_message(this+" : Réception du message DOWN");
			if (entite_double) {
				aff_message(this+" : Réception du message DOWN : l'entité est double, pas de réaction.");
			}
			else {
				aff_message(this+" : Réception du message DOWN : fin de l'entité.");
				System.exit(0);
			}
		}
		else {
			if (multidiffusion) return res;
			if (message.length()<13) {
				aff(""+this+" : Longueur d'un message autre que DOWN < 13 : ne respecte pas le format.");
				return false;
			}
			String idm = message.substring(5, 5+8);
			if (!messages_recus.contains(idm)) {
				res = envoie_message_UDP_machine_suivante(message, idm);
				messages_recus.add(idm);
			}
			else {
				messages_recus.remove(idm);
			}
			if (type.equals("WHOS")) {
				envoie_message("MEMB");
			}
			if (type.equals("GBYE")) {
				if (utile.compte_occurences(message, ' ')!=5) {
					aff(""+this+" :\nFonction : renvoie_message\nMessage : Message GBYE mal formaté");
				}
				message_tab = message.split(" ");
				if (message_tab.length!=6) {
					aff(""+this+" :\nFonction : renvoie_message\nMessage : Message GBYE mal formaté");
				}
				ip = message_tab[2];
				port = message_tab[3];
				if (ip.equals(ip_machine_suivante) && port.equals(""+port_d_ecoute_UDP_machine_suivante)) {
					ip = message_tab[4];
					if (utile.isInteger(message_tab[5])) portInt = Integer.parseInt(port);
					else {
						aff(""+this+" :\nFonction : renvoie_message\nMessage : GBYE adresse port donné incorrect");
						return false;
					}
					if (!utile.verifie_ip_v4(ip)) {
						aff(""+this+" :\nFonction : renvoie_message\nMessage : GBYE adresse ip donnée incorrecte");
						return false;
					}
					
					if (!utile.verifie_port(portInt)) {
						aff(""+this+" :\nFonction : renvoie_message\nMessage : GBYE adresse ip donnée incorrecte");
						return false;
					}
					ip_machine_suivante = ip;
					port_d_ecoute_UDP_machine_suivante = portInt;
					envoie_message("EYBG");
				}
			}
			if (type.equals("TEST")) {
				test = false;
			}
		}
		return res;
	}

	public boolean envoie_message_brut (String message) {
		boolean res = true;
		//aff("Message à envoyer : "+message);
		if (message==null) return false;
		if (message.isEmpty()) return false;
		if (message.length()<4) {
			aff(""+this+" : Longueur d'un message < 4 : ne respecte pas le format.");
			return false;
		}
		String type = message.substring(0, 4);
		if (type.equals("DOWN")) {
			res = envoie_message_multidiffusion("DOWN", "");
		}
		else {
			if (message.length()<13) {
				aff(""+this+" : Longueur d'un message autre que DOWN < 13 : ne respecte pas le format.");
				return false;
			}
			String idm = message.substring(5, 5+8);
			if (!messages_recus.contains(idm)) {
				res = envoie_message_UDP_machine_suivante(message, idm);
				messages_recus.add(idm);
			}
			else {
				messages_recus.remove(idm);
			}
		}
		return res;
	}

	public boolean envoie_message (String message, String id_app) {
		return envoie_message_brut(formate_message("APPL", message, id_app));
	}

	public boolean envoie_message (String type) {
		return envoie_message_brut(formate_message(type, "", ""));
	}

// ############################## Applications ################################### //

	public boolean installer_application (Application app) {
		return applications_installees.add(app.getId_app());
	}

	public boolean desinstaller_application (Application app) {
		return applications_installees.remove(app.getId_app());
	}

	public boolean supporte_lapplication(String id_app) {
		return applications_installees.contains(id_app);
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

	public String toString () {
		String res = "";
		//res="Entite "+identifiant;
		res=""+identifiant;
		return res;
	}

	public void presentation_entite (boolean entite_double) {
		String entite_double_string = "simple";
		if (entite_double) entite_double_string = "double"; 
		aff("Entité "+entite_double_string+" "+identifiant+" : ");
		aff("\tadresse_ip_reception_UDP : "+adresse_ip_reception_UDP);
		aff("\tport_reception_UDP : "+port_reception_UDP);
		aff("\tip_machine_suivante : "+ ip_machine_suivante);
		aff("\tip_machine_suivante : "+ip_machine_suivante);
		aff("\tport_d_ecoute_UDP_machine_suivante : "+port_d_ecoute_UDP_machine_suivante);
		aff("");
		aff("\tadresse_panne_reseau_ipv4_multidifusion : "+adresse_panne_reseau_ipv4_multidifusion);
		aff("\tport_de_multidifusion : "+port_de_multidifusion);
		aff("\n");
	}

// ########################### Getteurs et Setteurs ############################## //

	public boolean getTest () {
		return test;
	}

	public int getTemps_avant_nouvel_essai () {
		return temps_avant_nouvel_essai;
	}

	public int getNombre_dessais () {
		return this.nombre_dessais;
	}
}
