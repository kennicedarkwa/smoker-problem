public class Tisch {

    private boolean hatTabak = false;
    private  boolean hatPapier = false;
    private boolean hatStreichhoelzer = false;

    private boolean tischIstBelegt = false;

    //Kritischer Abschnitt. Kein raucher darf gerade zugreifen während ein Agent gerade drauflegt
    // synchronized sorgt für wechselseitigen ausschluss
    public synchronized void platziereZutaten(Zutaten z1, Zutaten z2) throws InterruptedException{
            /*
             * Pufferzugriff sperren (bzw. ggf. auf Zugriff warten): geschieht
             * automatisch durch Monitor-Eintritt ("synchronized" entspricht
             * synchronized(this){...})
             */

            /* Solange Puffer voll ==> warten! */
            while (tischIstBelegt) {
                this.wait(); // --> Warten in der Wait-Queue und Monitor des
                // Puffers freigeben
            }
            /* Item auf Tisch packen */
            setzeZutatenAufTisch(z1, true);
            setzeZutatenAufTisch(z2, true);
            tischIstBelegt = true;

            System.out
                    .println("          ENTER: "
                            + Thread.currentThread().getName()
                            + " plaziert "
                            + z1 + " und " + z2);

            /*
             * Wartenden Raucher wecken --> es muessen ALLE Threads geweckt werden
             * (evtl. auch andere Producer), da es nur eine Wait-Queue gibt!
             */
            this.notifyAll();


        }

        // Kritischer Abschnitt -> wird von rauchern aufgerufen
        public synchronized void probiereRauchen(Zutaten eigenes) throws InterruptedException{

            //* Raucher blockiert wenn Tisch leer und seine zwei auf dem tisch sind
            while (!tischIstBelegt || !sindMeinefehlendenZutatenDa(eigenes)) {
                this.wait(); // --> Warten in der Wait-Queue und Monitor des

            }

            /* Zustand verändern */
            hatTabak = false;
            hatPapier = false;
            hatStreichhoelzer = false;
            tischIstBelegt = false;

            System.out
                    .println("          REMOVE: "
                            + Thread.currentThread().getName()
                            + " nimmt die Zutaten vom Tisch.");

            /*
             * Wartenden Producer wecken --> es muessen ALLE Threads geweckt werden
             * (evtl. auch andere Consumer), da es nur eine Wait-Queue gibt!
             */
            this.notifyAll();


        }

        private void setzeZutatenAufTisch(Zutaten z, boolean status) {
            switch (z) {
                case Zutaten.TABAK -> hatTabak = status;
                case Zutaten.PAPIER -> hatPapier = status;
                case Zutaten.STREICHHOELZER -> hatStreichhoelzer = status;
            }
        }

        private boolean sindMeinefehlendenZutatenDa(Zutaten eigenes) {
            return switch (eigenes) {
                case Zutaten.TABAK -> hatPapier && hatStreichhoelzer;
                case Zutaten.PAPIER -> hatTabak && hatStreichhoelzer;
                case Zutaten.STREICHHOELZER -> hatTabak && hatPapier;
            };
        }

    }
