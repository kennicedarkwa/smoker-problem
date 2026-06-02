public class Raucher extends Thread {
    private Zutaten meineZutat;
    private Tisch tisch;

    public Raucher(Tisch tisch, Zutaten meineZutat, String name) {
        this.tisch = tisch;
        this.meineZutat = meineZutat;
        this.setName(name);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                // Warten, bis  passenden zwei anderen Zutaten da sind
                // Der Tisch-Monitor macht das wait(), wenn es nicht passt
                tisch.probiereRauchen(meineZutat);

                // rauchen kostet Zeit -> Thread.sleep
                System.out.println(this.getName() + " macht sich eine Zigarette und raucht...");
                Thread.sleep((int) (Math.random() * 1000));
                System.out.println(this.getName() + " ist fertig mit Rauchen.");

                // Danach geht die Schleife wieder von vorne los
            }
        } catch (InterruptedException e) {
            System.err.println(this.getName() + " wurde per Interrupt beendet.");
        }
    }
}
