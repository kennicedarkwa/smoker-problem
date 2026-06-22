import java.io.*;
import java.net.*;

public class RaucherClient {
    private final String CRLF = "\r\n";
    private int serverPort;
    private String hostname;
    private Socket clientSocket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private boolean serviceRequested = true;

    // Spezifische Eigenschaft dieses Rauchers
    private final Zutaten meineZutat;

    public RaucherClient(String hostname, int serverPort, Zutaten zutat) {
        this.hostname = hostname;
        this.serverPort = serverPort;
        this.meineZutat = zutat;
    }

    public void startJob() {
        try {
            /* Socket erzeugen --> Verbindungsaufbau mit dem Tisch-Server */
            clientSocket = new Socket(hostname, serverPort);

            /* Streams analog zur Vorlesungsvorlage initialisieren */
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "IBM-850"));

            System.err.println("Raucher-Prozess gestartet. Besitzt unendlich: " + meineZutat);

            /* 1. Am Tisch registrieren */
            writeToServer("REGISTER SMOKER " + meineZutat.name());
            String reply = readFromServer();

            if (!reply.startsWith("200")) {
                System.err.println("Registrierung am Tisch verweigert: " + reply);
                serviceRequested = false;
            }

            /* Endlose Schleife: Versuchen zu rauchen */
            while (serviceRequested) {
                System.err.println("Fordere fehlende Zutaten an (FETCH)...");
                writeToServer("FETCH");

                /* Hier blockiert das readFromServer() so lange, bis der Tisch liefert! */
                String matchReply = readFromServer();

                if (matchReply.startsWith("201 MATCH")) {
                    // --- ZUSTANDSZEIT: Der Raucher raucht lokal im eigenen Prozess ---
                    System.err.println("!!! Passende Zutaten erhalten. Ich rauche jetzt für 2 Sekunden...");
                    Thread.sleep(2000);

                    System.err.println("Fertig mit Rauchen. Gebe Tisch frei (DONE)...");
                    writeToServer("DONE");

                    // Bestätigung abwarten
                    readFromServer();
                } else {
                    System.err.println("Unerwartete Antwort: " + matchReply);
                    Thread.sleep(1000); // Fehler-Schutzpause
                }
            }

            clientSocket.close();
        } catch (Exception e) {
            System.err.println("Verbindung vom Server abgebrochen: " + e.getMessage());
        }
        System.err.println("Raucher-Client gestoppt!");
    }

    private void writeToServer(String line) throws IOException {
        outToServer.write((line + CRLF).getBytes("IBM-850"));
        System.err.println("Raucher Client sent: " + line);
    }

    private String readFromServer() throws IOException {
        String reply = inFromServer.readLine();
        System.err.println("Raucher Client got reply: " + reply);
        return reply;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Fehler: Bitte Zutat angeben! (TABAK, PAPIER oder STREICHHOELZER)");
            return;
        }
        try {
            Zutaten gewaehlteZutat = Zutaten.valueOf(args[0].toUpperCase().trim());

            // Starten mit dem Port des TischServers (7777)
            RaucherClient client = new RaucherClient("localhost", 7777, gewaehlteZutat);
            client.startJob();

        } catch (IllegalArgumentException e) {
            System.err.println("Fehler: '" + args[0] + "' ist keine gültige Zutat. Erlaubt sind: TABAK, PAPIER, STREICHHOELZER");
        }

    }
}