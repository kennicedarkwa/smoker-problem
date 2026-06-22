import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class TischServer {

    /* Semaphore begrenzt die Anzahl parallel laufender Worker-Threads */
    public Semaphore workerThreadsSem;

    /* Portnummer */
    public final int serverPort;

    /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
    public boolean serviceRequested = true;

    // Zustand des Smoker-Monitors
    public enum TischStatus {LEER, ZUTATEN_DA, RAUCHT}

    private TischStatus aktuellerZustand = TischStatus.LEER;
    private final Set<Zutaten> zutatenAufTisch = new HashSet<>();

    /*
     * Konstruktor mit Parametern: Server-Port, Maximale Anzahl paralleler
     * Worker-Threads
     */
    public TischServer(int serverPort, int maxThreads) {
        this.serverPort = serverPort;
        this.workerThreadsSem = new Semaphore(maxThreads);
    }

    public void startServer() {
        ServerSocket welcomeSocket; // TCP-Server-Socketklasse
        Socket connectionSocket; // TCP-Standard-Socketklasse

        int nextThreadNumber = 0;

        try {
            /* Server-Socket erzeugen */
            System.err.println("Creating new Tisch TCP Server Socket Port " + serverPort);
            welcomeSocket = new ServerSocket(serverPort);

            while (serviceRequested) {
                workerThreadsSem.acquire(); // Blockieren, wenn max. Anzahl Worker-Threads erreicht

                System.err.println("Tisch Server is waiting for connection on port " + serverPort);
                /*
                 * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
                 * Standard-Socket erzeugen und an connectionSocket zuweisen
                 */
                connectionSocket = welcomeSocket.accept();

                /*
                 * Neuen Arbeits-Thread erzeugen und die Nummer, den Socket sowie das
                 * Serverobjekt uebergeben
                 */
                (new TischWorkerThread(nextThreadNumber++, connectionSocket, this)).start();
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }


    }

    // Synchronisierte Monitor-Methoden (Kritischer Abschnitt)

    public synchronized void zutatenAblegen(Zutaten z1, Zutaten z2) throws Exception {
        while (aktuellerZustand != TischStatus.LEER) {
            // Falls der Tisch voll ist oder geraucht wird, wartet der Agent im Monitor
            wait();
        }
        zutatenAufTisch.add(z1);
        zutatenAufTisch.add(z2);
        aktuellerZustand = TischStatus.ZUTATEN_DA;
        System.err.println("[MONITOR] Zutaten abgelegt: " + zutatenAufTisch);
        notifyAll(); // Alle blockierten Worker-Threads (Raucher) aufwecken
    }

    public synchronized void zutatenAbholen(Zutaten besitztZutat) throws Exception {
        // Solange der Tisch nicht bereit ist oder die Zutaten die eigene enthalten (dann braucht er sie nicht)
        while (aktuellerZustand != TischStatus.ZUTATEN_DA || zutatenAufTisch.contains(besitztZutat)) {
            wait(); // Raucher-Worker wartet hier blockierend im Monitor!
        }
        // Zutaten passen
        zutatenAufTisch.clear();
        aktuellerZustand = TischStatus.RAUCHT;
        System.err.println("[MONITOR] Zutaten weggenommen. Zustand: RAUCHT");
        notifyAll();
    }

    public synchronized void rauchenBeendet() {
        aktuellerZustand = TischStatus.LEER;
        System.err.println("[MONITOR] Rauchen beendet. Zustand: LEER");
        notifyAll(); // Agents aufwecken, damit sie neu legen können
    }

    public static void main(String[] args) {
        // Port 7777, maximal 10 parallele Verbindungen erlaubt
        TischServer myServer = new TischServer(7777, 10);
        myServer.startServer();
    }
}

// ----------------------------------------------------------------------------

class TischWorkerThread extends Thread {
    /*
     * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
     * erhaelt
     */
    /* Protokoll-Codierung des Zeilenendes: CRLF */
    private final String CRLF = "\r\n";

    private int name;
    private Socket socket;
    private TischServer server;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    boolean workerServiceRequested = true; // Arbeitsthread beenden?

    // Client-Spezifischer Zustand innerhalb dieser TCP-Verbindung
    private String clientRolle = null;
    private Zutaten raucherZutat = null;

    public TischWorkerThread(int num, Socket sock, TischServer server) {
        /* Konstruktor */
        this.name = num;
        this.socket = sock;
        this.server = server;
    }

    public void run() {
        System.err.println("Tisch Worker Thread " + name + " started.");

        try {
            /* Socket-Basisstreams durch spezielle Streams filtern */
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new DataOutputStream(socket.getOutputStream());

            while (workerServiceRequested) {
                String rawRequest = readFromClient();
                if (rawRequest == null) {
                    break; // Client hat die Verbindung unangekündigt geschlossen
                }

                String[] tokens = rawRequest.trim().split("\\s+");
                if (tokens.length == 0 || tokens[0].isEmpty()) {
                    writeToClient("400 BAD REQUEST");
                    continue;
                }

                String command = tokens[0].toUpperCase();

                // Protokoll-Zustandsmaschine abarbeiten
                try {
                    switch (command) {
                        case "REGISTER":
                            if (tokens.length < 3) {
                                writeToClient("400 BAD REQUEST Missing Parameters");
                                break;
                            }
                            String rolle = tokens[1].toUpperCase();
                            if (rolle.equals("AGENT")) {
                                this.clientRolle = "AGENT";
                                writeToClient("200 OK");
                            } else if (rolle.equals("SMOKER")) {
                                try {
                                    // fängt Tippfehler ab
                                    this.raucherZutat = Zutaten.valueOf(tokens[2].toUpperCase());
                                    this.clientRolle = "SMOKER";
                                    writeToClient("200 OK");
                                } catch (IllegalArgumentException e) {
                                    writeToClient("400 BAD REQUEST Invalid Ingredient Token");
                                }
                            } else {
                                writeToClient("400 BAD REQUEST Unknown Role");
                            }
                            break;

                        case "PUT":
                            if (!"AGENT".equals(clientRolle)) {
                                writeToClient("403 FORBIDDEN");
                                break;
                            }
                            if (tokens.length < 3) {
                                writeToClient("400 BAD REQUEST Missing Ingredients");
                                break;
                            }
                            try {
                                // Strings in Enum-Werte mappen
                                Zutaten z1 = Zutaten.valueOf(tokens[1].toUpperCase());
                                Zutaten z2 = Zutaten.valueOf(tokens[2].toUpperCase());

                                server.zutatenAblegen(z1, z2);
                                writeToClient("200 OK");
                            } catch (IllegalArgumentException e) {
                                writeToClient("400 BAD REQUEST Invalid Ingredient Token");
                            }
                            break;

                        case "FETCH":
                            if (!"SMOKER".equals(clientRolle)) {
                                writeToClient("403 FORBIDDEN");
                                break;
                            }
                            // HIER PASSIERT DIE SYNCHRONISATION:
                            // Dieser Aufruf blockiert den gesamten Worker-Thread im Monitor (.wait()),
                            // bis die Zutaten passen. Erst DANN wird die Antwort über Sockets gesendet!
                            server.zutatenAbholen(this.raucherZutat);
                            writeToClient("201 MATCH");
                            break;

                        case "DONE":
                            if (!"SMOKER".equals(clientRolle)) {
                                writeToClient("403 FORBIDDEN");
                                break;
                            }
                            server.rauchenBeendet();
                            writeToClient("200 OK");
                            break;

                        case "QUIT":
                            writeToClient("200 OK BYE");
                            workerServiceRequested = false;
                            break;

                        default:
                            writeToClient("400 BAD REQUEST Unknown Command");
                    }
                } catch (Exception e) {
                    writeToClient("500 INTERNAL SERVER ERROR " + e.getMessage());
                }
            }

            socket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by client in Thread " + name);
        } finally {
            System.err.println("TCP Worker Thread " + name + " stopped!");
            server.workerThreadsSem.release(); // Platz freigeben
        }
    }

    private String readFromClient() throws IOException {
        /*
         * Lies die naechste Anfrage-Zeile (request) vom Client
         * ALLE Anfragen vom Client müssen über diese Methode empfangen werden
         * ("Sub-Layer")
         */
        String request = inFromClient.readLine();
        System.err.println("Tisch Worker Thread " + name + " detected job: " + request);

        return request;
    }

    private void writeToClient(String line) throws IOException {
        /*
         * Sende eine Antwortzeile zum Client
         * ALLE Antworten an den Client müssen über diese Methode gesendet werden
         * ("Sub-Layer")
         */
        outToClient.write((line + CRLF).getBytes());
        System.err.println("TCP Worker Thread " + name + " has written the message: " + line);
    }
}
