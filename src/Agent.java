import java.util.Date;

public class Agent extends Thread {
    private Tisch tisch;

    public Agent(Tisch tisch, String name) {
        this.tisch = tisch;
        this.setName(name);
    }

    @Override
    public void run() {
        /*
         * Erzeuge Date-Objekte und lege sie in den Puffer. Halte nach jeder
         * Ablage fuer eine Zufallszeit an.
         */
        try {
            while (!isInterrupted()) {
                String zutat1 = "";
                String zutat2 = "";

                int zufall = (int) (Math.random() * 3);
                Zutaten z1, z2;
                switch (zufall) {
                    case 0 -> { z1 = Zutaten.TABAK;          z2 = Zutaten.PAPIER; }
                    case 1 -> { z1 = Zutaten.TABAK;          z2 = Zutaten.STREICHHOELZER; }
                    default -> { z1 = Zutaten.PAPIER;        z2 = Zutaten.STREICHHOELZER; }
                }
                tisch.platziereZutaten(z1, z2);

                // Kurze Pause, bevor der Agent den nächsten Zyklus startet
                Thread.sleep((int) (Math.random() * 500));
            }
        } catch (InterruptedException ex) {
            // Interrupt aufgetreten --> fertig
            System.err.println(this.getName() + " wurde erfolgreich interrupted!");
        }
    }

}
