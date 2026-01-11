import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serveur WebSocket natif Java (sans dependances externes).
 * Permet au navigateur web de se connecter et recevoir l'etat de l'anneau.
 */
class WebSocketServer implements Runnable {

    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int DEFAULT_PORT = 6111;

    private int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private List<WebSocketClient> clients;
    private AnneauController controller;

    /**
     * Constructeur avec port par defaut (8080).
     */
    public WebSocketServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Constructeur avec port specifique.
     * @param port Port d'ecoute du serveur WebSocket
     */
    public WebSocketServer(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
        this.controller = null;
    }

    /**
     * Definit le controleur d'anneau.
     * @param controller Controleur pour gerer les commandes
     */
    public void setController(AnneauController controller) {
        this.controller = controller;
    }

    /**
     * Demarre le serveur WebSocket.
     */
    public void start() {
        if (running) {
            System.out.println("WebSocketServer : deja en cours d'execution");
            return;
        }
        Thread serverThread = new Thread(this);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Arrete le serveur WebSocket.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("WebSocketServer.stop : " + e.getMessage());
        }
        for (WebSocketClient client : clients) {
            client.close();
        }
        clients.clear();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("WebSocketServer : demarre sur le port " + port);
            System.out.println("WebSocketServer : ouvrez http://localhost:" + port + " dans votre navigateur");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewConnection(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        System.out.println("WebSocketServer.run : " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("WebSocketServer.run : impossible de demarrer sur le port " + port + " : " + e.getMessage());
        }
    }

    /**
     * Gere une nouvelle connexion entrante.
     * @param socket Socket du client
     */
    private void handleNewConnection(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream output = socket.getOutputStream();

            String line = reader.readLine();
            if (line == null) {
                socket.close();
                return;
            }

            Map<String, String> headers = new HashMap<>();
            String requestPath = "/";

            if (line.startsWith("GET")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    requestPath = parts[1];
                }
            }

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    headers.put(key.toLowerCase(), value);
                }
            }

            if (headers.containsKey("upgrade") && headers.get("upgrade").equalsIgnoreCase("websocket")) {
                handleWebSocketUpgrade(socket, output, headers);
            } else {
                handleHttpRequest(socket, output, requestPath);
            }

        } catch (IOException e) {
            System.out.println("WebSocketServer.handleNewConnection : " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Gere une requete HTTP normale (sert les fichiers statiques).
     * @param socket Socket client
     * @param output Flux de sortie
     * @param path Chemin demande
     */
    private void handleHttpRequest(Socket socket, OutputStream output, String path) throws IOException {
        String filePath = "WEB/";

        if (path.equals("/")) {
            filePath += "index.html";
        } else {
            filePath += path.substring(1);
        }

        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            String contentType = getContentType(filePath);
            byte[] content = readFile(file);

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + content.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            output.write(response.getBytes());
            output.write(content);
        } else {
            String notFound = "<!DOCTYPE html><html><body><h1>404 Not Found</h1></body></html>";
            String response = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + notFound.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" + notFound;
            output.write(response.getBytes());
        }

        output.flush();
        socket.close();
    }

    /**
     * Lit le contenu d'un fichier.
     * @param file Fichier a lire
     * @return Contenu en bytes
     */
    private byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    /**
     * Determine le type MIME d'un fichier.
     * @param path Chemin du fichier
     * @return Type MIME
     */
    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    /**
     * Gere l'upgrade WebSocket.
     * @param socket Socket client
     * @param output Flux de sortie
     * @param headers Headers HTTP
     */
    private void handleWebSocketUpgrade(Socket socket, OutputStream output, Map<String, String> headers) throws IOException {
        String webSocketKey = headers.get("sec-websocket-key");

        if (webSocketKey == null) {
            socket.close();
            return;
        }

        String acceptKey = generateAcceptKey(webSocketKey);

        String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                "\r\n";

        output.write(response.getBytes());
        output.flush();

        WebSocketClient client = new WebSocketClient(socket, this);
        clients.add(client);

        Thread clientThread = new Thread(client);
        clientThread.setDaemon(true);
        clientThread.start();

        System.out.println("WebSocketServer : nouveau client connecte (" + clients.size() + " clients)");

        if (controller != null) {
            controller.onClientConnected(client);
        }
    }

    /**
     * Genere la cle d'acceptation WebSocket.
     * @param key Cle du client
     * @return Cle d'acceptation
     */
    private String generateAcceptKey(String key) {
        try {
            String combined = key + WEBSOCKET_GUID;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            System.out.println("WebSocketServer.generateAcceptKey : " + e.getMessage());
            return "";
        }
    }

    /**
     * Envoie un message a tous les clients connectes.
     * @param message Message a envoyer
     */
    public void broadcast(String message) {
        for (WebSocketClient client : clients) {
            client.send(message);
        }
    }

    /**
     * Retire un client de la liste.
     * @param client Client a retirer
     */
    void removeClient(WebSocketClient client) {
        clients.remove(client);
        System.out.println("WebSocketServer : client deconnecte (" + clients.size() + " clients)");
    }

    /**
     * Traite un message recu d'un client.
     * @param client Client emetteur
     * @param message Message recu
     */
    void onMessage(WebSocketClient client, String message) {
        System.out.println("WebSocketServer : message recu : " + message);
        if (controller != null) {
            controller.onMessage(client, message);
        }
    }

    /**
     * Retourne le nombre de clients connectes.
     * @return Nombre de clients
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Verifie si le serveur est en cours d'execution.
     * @return true si en cours d'execution
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Retourne le port d'ecoute.
     * @return Port
     */
    public int getPort() {
        return port;
    }
}

/**
 * Represente un client WebSocket connecte.
 */
class WebSocketClient implements Runnable {

    private Socket socket;
    private WebSocketServer server;
    private InputStream input;
    private OutputStream output;
    private boolean connected = true;

    /**
     * Constructeur.
     * @param socket Socket du client
     * @param server Serveur parent
     */
    WebSocketClient(Socket socket, WebSocketServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
    }

    @Override
    public void run() {
        try {
            while (connected && !socket.isClosed()) {
                String message = readMessage();
                if (message != null) {
                    server.onMessage(this, message);
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("WebSocketClient.run : " + e.getMessage());
            }
        } finally {
            close();
            server.removeClient(this);
        }
    }

    /**
     * Lit un message WebSocket.
     * @return Message decode ou null
     */
    private String readMessage() throws IOException {
        int firstByte = input.read();
        if (firstByte == -1) {
            connected = false;
            return null;
        }

        int opcode = firstByte & 0x0F;

        if (opcode == 0x08) {
            connected = false;
            return null;
        }

        int secondByte = input.read();
        if (secondByte == -1) {
            connected = false;
            return null;
        }

        boolean masked = (secondByte & 0x80) != 0;
        int payloadLength = secondByte & 0x7F;

        if (payloadLength == 126) {
            payloadLength = (input.read() << 8) | input.read();
        } else if (payloadLength == 127) {
            payloadLength = 0;
            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | input.read();
            }
        }

        byte[] maskKey = new byte[4];
        if (masked) {
            input.read(maskKey);
        }

        byte[] payload = new byte[payloadLength];
        int bytesRead = 0;
        while (bytesRead < payloadLength) {
            int read = input.read(payload, bytesRead, payloadLength - bytesRead);
            if (read == -1) {
                connected = false;
                return null;
            }
            bytesRead += read;
        }

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ maskKey[i % 4]);
            }
        }

        return new String(payload, "UTF-8");
    }

    /**
     * Envoie un message au client.
     * @param message Message a envoyer
     */
    public synchronized void send(String message) {
        if (!connected || socket.isClosed()) {
            return;
        }

        try {
            byte[] payload = message.getBytes("UTF-8");

            output.write(0x81);

            if (payload.length < 126) {
                output.write(payload.length);
            } else if (payload.length < 65536) {
                output.write(126);
                output.write((payload.length >> 8) & 0xFF);
                output.write(payload.length & 0xFF);
            } else {
                output.write(127);
                for (int i = 7; i >= 0; i--) {
                    output.write((payload.length >> (8 * i)) & 0xFF);
                }
            }

            output.write(payload);
            output.flush();

        } catch (IOException e) {
            System.out.println("WebSocketClient.send : " + e.getMessage());
            close();
        }
    }

    /**
     * Ferme la connexion.
     */
    public void close() {
        connected = false;
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("WebSocketClient.close : " + e.getMessage());
        }
    }

    /**
     * Verifie si le client est connecte.
     * @return true si connecte
     */
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }
}
