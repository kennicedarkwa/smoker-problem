import java.util.LinkedList;

public class SmokerSimulation {
  public static void  main(String[] args) {
    Tisch tisch = new Tisch();

      LinkedList<Thread> threads = new LinkedList<>();

      System.out.println(" STARTE SIMULATION ");

      Raucher r1 = new Raucher(tisch, Zutaten.TABAK,          "Raucher A (Hat Tabak)");
      Raucher r2 = new Raucher(tisch, Zutaten.PAPIER,         "Raucher B (Hat Papier)");
      Raucher r3 = new Raucher(tisch, Zutaten.STREICHHOELZER, "Raucher C (Hat Streichhölzer)");

      threads.add(r1);
      threads.add(r2);
      threads.add(r3);

      r1.start();
      r2.start();
      r3.start();

      Agent a1 = new Agent(tisch, "Agent 1");
      Agent a2 = new Agent(tisch, "Agent 2");

      threads.add(a1);
      threads.add(a2);
      a1.start();
      a2.start();

      try {
          Thread.sleep(5000);
      } catch (InterruptedException e) {
          e.printStackTrace();
      }

      System.out.println("ZEIT ABGELAUFEN, BEENDE THREADS");

      for (Thread t : threads) {
          t.interrupt();
      }
     }
}
