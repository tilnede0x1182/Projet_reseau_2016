import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controleur de l'anneau pour l'interface web.
 * Gere les commandes venant du navigateur et notifie l'etat de l'anneau.
 */
class AnneauController {

    private WebSocketServer wsServer;
    private List<Entite> entites;
    private int nextPort = 5560;

    /**
     * Constructeur.
     * @param wsServer Serveur WebSocket
     */
    public AnneauController(WebSocketServer wsServer) {
        this.wsServer = wsServer;
        this.entites = new CopyOnWriteArrayList<>();
    }

    /**
     * Ajoute une entite a la liste geree.
     * @param entite Entite a ajouter
     */
    public void addEntite(Entite entite) {
        entites.add(entite);
        broadcastState();
    }

    /**
     * Retire une entite de la liste.
     * @param entite Entite a retirer
     */
    public void removeEntite(Entite entite) {
        entites.remove(entite);
        broadcastState();
    }

    /**
     * Appele quand un client WebSocket se connecte.
     * @param client Client connecte
     */
    public void onClientConnected(WebSocketClient client) {
        String state = buildStateJson();
        client.send(state);
    }

    /**
     * Appele quand un message est recu d'un client.
     * @param client Client emetteur
     * @param message Message recu (JSON)
     */
    public void onMessage(WebSocketClient client, String message) {
        try {
            String command = extractJsonValue(message, "command");

            if (command == null) {
                sendError(client, "Commande manquante");
                return;
            }

            switch (command) {
                case "getState":
                    client.send(buildStateJson());
                    break;

                case "addEntite":
                    handleAddEntite(client);
                    break;

                case "removeEntite":
                    String idToRemove = extractJsonValue(message, "id");
                    handleRemoveEntite(client, idToRemove);
                    break;

                case "sendWHOS":
                    handleSendMessage("WHOS");
                    break;

                case "sendTEST":
                    handleSendMessage("TEST");
                    break;

                case "sendDOWN":
                    handleSendMessage("DOWN");
                    break;

                case "sendAPPL":
                    String applMessage = extractJsonValue(message, "message");
                    handleSendAppl(applMessage);
                    break;

                default:
                    sendError(client, "Commande inconnue: " + command);
            }

        } catch (Exception e) {
            System.out.println("AnneauController.onMessage : " + e.getMessage());
            sendError(client, "Erreur: " + e.getMessage());
        }
    }

    /**
     * Gere l'ajout d'une nouvelle entite.
     * @param client Client demandeur
     */
    private void handleAddEntite(WebSocketClient client) {
        if (entites.isEmpty()) {
            Entite newEntite = new Entite(nextPort++, "127.0.0.1");
            newEntite.setAnneauController(this);
            entites.add(newEntite);
            broadcastLog("Entite " + newEntite.identifiant + " creee (premiere entite)");
        } else {
            Entite firstEntite = entites.get(0);
            Entite newEntite = new Entite("E" + (entites.size() + 1), nextPort++, "127.0.0.1");
            newEntite.setAnneauController(this);
            entites.add(newEntite);

            boolean inserted = newEntite.insertion("127.0.0.1", firstEntite.port_TCP);

            if (inserted) {
                broadcastLog("Entite " + newEntite.identifiant + " inseree dans l'anneau");
            } else {
                broadcastLog("Echec insertion entite " + newEntite.identifiant);
            }
        }

        broadcastState();
    }

    /**
     * Gere le retrait d'une entite.
     * @param client Client demandeur
     * @param id Identifiant de l'entite
     */
    private void handleRemoveEntite(WebSocketClient client, String id) {
        if (id == null || id.isEmpty()) {
            sendError(client, "ID entite manquant");
            return;
        }

        Entite toRemove = null;
        for (Entite entite : entites) {
            if (entite.identifiant.equals(id)) {
                toRemove = entite;
                break;
            }
        }

        if (toRemove != null) {
            toRemove.envoie_message("GBYE");
            entites.remove(toRemove);
            broadcastLog("Entite " + id + " retiree de l'anneau");
            broadcastState();
        } else {
            sendError(client, "Entite non trouvee: " + id);
        }
    }

    /**
     * Envoie un message de type specifique via la premiere entite.
     * @param type Type de message (WHOS, TEST, DOWN)
     */
    private void handleSendMessage(String type) {
        if (entites.isEmpty()) {
            broadcastLog("Impossible d'envoyer " + type + ": aucune entite");
            return;
        }

        Entite firstEntite = entites.get(0);
        firstEntite.envoie_message(type);
        broadcastLog("Message " + type + " envoye par " + firstEntite.identifiant);
    }

    /**
     * Envoie un message applicatif.
     * @param message Contenu du message
     */
    private void handleSendAppl(String message) {
        if (entites.isEmpty()) {
            broadcastLog("Impossible d'envoyer APPL: aucune entite");
            return;
        }

        if (message == null || message.isEmpty()) {
            message = "Hello from web!";
        }

        Entite firstEntite = entites.get(0);
        firstEntite.envoie_message(message, "WEB_APP");
        broadcastLog("Message APPL envoye: " + message);
    }

    /**
     * Construit le JSON de l'etat de l'anneau.
     * @return JSON de l'etat
     */
    private String buildStateJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"state\",\"entites\":[");

        for (int i = 0; i < entites.size(); i++) {
            Entite entite = entites.get(i);
            if (i > 0) sb.append(",");

            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(entite.identifiant)).append("\",");
            sb.append("\"ip\":\"").append(escapeJson(entite.adresse_ip_reception_UDP)).append("\",");
            sb.append("\"portUDP\":").append(entite.port_reception_UDP).append(",");
            sb.append("\"portTCP\":").append(entite.port_TCP).append(",");
            sb.append("\"nextIp\":\"").append(escapeJson(entite.ip_machine_suivante)).append("\",");
            sb.append("\"nextPort\":").append(entite.port_d_ecoute_UDP_machine_suivante);
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * Diffuse l'etat de l'anneau a tous les clients.
     */
    public void broadcastState() {
        String state = buildStateJson();
        wsServer.broadcast(state);
    }

    /**
     * Diffuse un message de log a tous les clients.
     * @param message Message de log
     */
    public void broadcastLog(String message) {
        String json = "{\"type\":\"log\",\"message\":\"" + escapeJson(message) + "\",\"timestamp\":" + System.currentTimeMillis() + "}";
        wsServer.broadcast(json);
    }

    /**
     * Notifie qu'un message circule dans l'anneau.
     * @param entite Entite qui a recu/envoye
     * @param messageType Type de message
     * @param content Contenu
     */
    public void notifyMessage(Entite entite, String messageType, String content) {
        String json = "{\"type\":\"message\"," +
                "\"entite\":\"" + escapeJson(entite.identifiant) + "\"," +
                "\"messageType\":\"" + escapeJson(messageType) + "\"," +
                "\"content\":\"" + escapeJson(content) + "\"," +
                "\"timestamp\":" + System.currentTimeMillis() + "}";
        wsServer.broadcast(json);
    }

    /**
     * Envoie une erreur a un client.
     * @param client Client destinataire
     * @param error Message d'erreur
     */
    private void sendError(WebSocketClient client, String error) {
        String json = "{\"type\":\"error\",\"message\":\"" + escapeJson(error) + "\"}";
        client.send(json);
    }

    /**
     * Extrait une valeur d'un JSON simple.
     * @param json Chaine JSON
     * @param key Cle recherchee
     * @return Valeur ou null
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);

        if (keyIndex == -1) {
            return null;
        }

        int valueStart = keyIndex + searchKey.length();

        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        char firstChar = json.charAt(valueStart);

        if (firstChar == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) {
                return null;
            }
            return json.substring(valueStart + 1, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char currentChar = json.charAt(valueEnd);
                if (currentChar == ',' || currentChar == '}' || currentChar == ']') {
                    break;
                }
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    /**
     * Echappe les caracteres speciaux pour JSON.
     * @param str Chaine a echapper
     * @return Chaine echappee
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Retourne la liste des entites.
     * @return Liste des entites
     */
    public List<Entite> getEntites() {
        return entites;
    }
}
