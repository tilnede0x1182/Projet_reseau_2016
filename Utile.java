import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.lang.Thread;

class Utile {
	private static Pattern pattern;
	private static Matcher matcher;
	private static HashSet<Integer> ports_deja_choisis;

// ##################### Vérification des adresses ip et ports ################### //

	public static boolean verifie_ip_v4 (String ip) {
		String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."+
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		pattern = Pattern.compile(IPADDRESS_PATTERN);
		matcher = pattern.matcher(ip);
		return matcher.matches();	    	    
	}	

	public static boolean verifie_ip_multidiffusion (String ip) {
		String IPADDRESS_PATTERN = "2(?:2[4-9]|3\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d?|0)){3}";
		pattern = Pattern.compile(IPADDRESS_PATTERN);
		matcher = pattern.matcher(ip);
		return matcher.matches();
	}

	public static boolean verifie_port_creation (int port) {
		boolean ok = true;
		if (!(port>0 && port<=9999)) ok = false;
		if (!(port>=8000 && port<9000)) ok = false;
		return ok;
	}

	public static boolean verifie_port (int port) {
		boolean ok = true;
		if (!(port>0 && port<=9999)) ok = false;
		return ok;
	}

// ################# Fonctions utilitaires - Autres fonctions #################### //

	/**
		Les but est de pouvoir compléter l'adresse ip pour qu'elle réponde aux exigences, 
		morceau par morceau (ici on ne traite l'un des quatre morceaux de l'adresse ipv4).
		Valeur renvoyée en fonction de la valeur reçue :
		"" -> ""
		"1" -> "001"
		"12" -> "012"
		"123" -> "123"
	**/
	public static String complete_ip_chiffre (String nombre) {
		if (nombre.length()==0) return "";
		if (nombre.length()==1) return "00"+nombre;
		if (nombre.length()==2) return "0"+nombre;
		if (nombre.length()==3) return nombre;
		return "";
	}

	public static String complete_ip (String ip) {
		if (compte_occurences(ip, '.')!=3) {
			aff("Fonction : complete_ip\nMessage d'erreur : ipv4 incorrecte.");
			return null;
		}
		String [] ip_tab = ip.split("\\.");
		if (ip_tab.length!=4)  {
			aff("Fonction : complete_ip\nMessage d'erreur : ipv4 incorrecte.");
			return null;
		}
		String [] ip_res = new String[4];
		for (int i=0; i<4; i++) {
			ip_res[i] = complete_ip_chiffre(ip_tab[i]);
		}
		return ip_res[0]+"."+ip_res[1]+"."+ip_res[2]+"."+ip_res[3];	
	}

	public static int compte_occurences (String s0, char a_trouver) {
		int s0len = s0.length();
		int cmp = 0;
		for (int i=0; i<s0len; i++) {
			if (s0.charAt(i)==a_trouver) cmp++;
		}
		return cmp;
	}

	public static void sleep (int centiseconds) {
		try {
			Thread.sleep(centiseconds*10);
		}
		catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static int randInt (int min, int max) {
		int res = (int)(Math.random()*max)+min;
		if (res>max) res = max;
		if (res<min) res = min;

		return res;
	}

	public static boolean isInteger (String n0) {
		try {
			int n1 = Integer.parseInt(n0);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	public static void aff (Object s0) {
		System.out.println(s0);
	}

	public static void affnn (Object s0) {
		System.out.print(s0);
	}

// ######### Fonctions utilitaires - génération des port et adresses_ip ########## //

	/**
		Génère l'identifiant de l'entité.

		char symbole_tmp :
			0 : chiffre
			1 : lettre minuscule
			2 : lettre majuscule
	**/
	public static String genere_identifiant (int nombre_de_char) {
		String id="";
		if (nombre_de_char<1)
			return "0";
		char symbole_tmp = (char)(0);
		/**
			0 : chiffre
			1 : lettre minuscule
			2 : lettre majuscule
		**/
		for (int i = 0; i<nombre_de_char; i++) {
			int choix_symbole = randInt(0, 2);
			if (choix_symbole==0) {
				symbole_tmp = (char)('0'+(char)(randInt(0, 9)));
			}
			if (choix_symbole==1) {
				symbole_tmp = (char)('a'+(char)(randInt(0, 25)));
			}
			if (choix_symbole==2) {
				symbole_tmp = (char)('A'+(char)(randInt(0, 25)));
			}
			id+=symbole_tmp;
		}
		return id;
	}

	/**
		Génère un numéro de port entre min et max et en 
		évitant les valeurs du HashSet ports_deja_choisis.
	**/
	public static int genere_numero_de_port (int min, int max, HashSet<Integer> ports_deja_choisis) {
		int res = 0;
		Utile.sleep(5);
		do {
			res = randInt(min, max);
		}
		while (ports_deja_choisis.contains(res) || !verifie_port_creation(res));
		ports_deja_choisis.add(res);
		return res;
	}

	/**
		Génère une adresse ipv4 de multidiffusion.
	**/
	public static String genere_adresse_ipv4_multidiffusion () {
		String p1 = ""+randInt(224, 239);
		String p2 = complete_ip_chiffre(""+randInt(0, 255));
		String p3 = complete_ip_chiffre(""+randInt(0, 255));
		String p4 = complete_ip_chiffre(""+randInt(0, 255));
		return (p1+"."+p2+"."+p3+"."+p4);
	}

// ######## Fonctions utilitaires - vérification de la forme des messages ######## //

	public static boolean verifie_message_correct (String message) {
		return (message.length()<=512);
	}

// ############################ Getteurs et setteurs ############################# //

	public static HashSet<Integer> getPorts_deja_choisis () {
		return ports_deja_choisis;
	}

	public static void setPorts_deja_choisis (HashSet<Integer> ports_deja_choisis_tmp) {
		ports_deja_choisis = ports_deja_choisis_tmp;
	}
}
