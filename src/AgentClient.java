import java.io.*;
import java.net.*;
import java.util.Random;

public class AgentClient {
    private final String CRLF = "\r\n";
    private int serverPort;
    private String hostname;
    private Socket clientSocket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private boolean serviceRequested = true;

    private final int agentId;
    private final Random rand = new Random();
    // ÄNDERE DIESE ZEILE in deinem AgentClient:
    private final String[] zutatenPool = {"TABAK", "PAPIER", "STREICHHOELZER"};

    public AgentClient(String hostname, int serverPort, int id) {
        this.hostname = hostname;
        this.serverPort = serverPort;
        this.agentId = id;
    }

    public void startJob() {
        try {
            clientSocket = new Socket(hostname, serverPort);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "IBM-850"));

            System.err.println("Agent-Prozess " + agentId + " gestartet.");

            /* 1. Als Agent registrieren */
            writeToServer("REGISTER AGENT " + agentId);
            String reply = readFromServer();

            if (!reply.startsWith("200")) {
                serviceRequested = false;
            }

            while (serviceRequested) {
                // Zufällig zwei *unterschiedliche* Zutaten auswählen
                int idx1 = rand.nextInt(3);
                int idx2 = (idx1 + 1 + rand.nextInt(2)) % 3;
                String z1 = zutatenPool[idx1];
                String z2 = zutatenPool[idx2];

                System.err.println("Agent " + agentId + " bereitet vor: " + z1 + " und " + z2);
                Thread.sleep(1500); // Kleine Bedenkzeit vor dem Ablegen

                // Versuchen, die Zutaten per PUT auf den Tisch zu legen
                writeToServer("PUT " + z1 + " " + z2);
                String putReply = readFromServer();

                if (putReply.startsWith("200")) {
                    System.err.println("Agent " + agentId + " hat Zutaten erfolgreich abgelegt.");
                } else if (putReply.startsWith("409")) {
                    // Robuste Fehlerbehandlung (Anforderung c): Tisch ist noch blockiert
                    System.err.println("Tisch besetzt oder es wird geraucht. Agent wartet...");
                }

                Thread.sleep(2000); // Pause bis zur nächsten Runde
            }

            clientSocket.close();
        } catch (Exception e) {
            System.err.println("Verbindung abgebrochen: " + e.getMessage());
        }
    }

    private void writeToServer(String line) throws IOException {
        outToServer.write((line + CRLF).getBytes("IBM-850"));
        System.err.println("Agent Client " + agentId + " sent: " + line);
    }

    private String readFromServer() throws IOException {
        String reply = inFromServer.readLine();
        System.err.println("Agent Client " + agentId + " got reply: " + reply);
        return reply;
    }

    public static void main(String[] args) {
        int id = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
        AgentClient client = new AgentClient("localhost", 7777, id);
        client.startJob();
    }
}