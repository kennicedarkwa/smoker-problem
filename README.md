# Smoker Problem

Java implementation of the classic cigarette smokers problem, a synchronization problem from operating systems. Three smokers each have one ingredient (tobacco, paper or matches). An agent puts two ingredients on the table, and only the smoker with the missing third ingredient may take them.

Team project in the course Computer Networks & Operating Systems at HAW Hamburg.

## Two versions

**Local (threads + monitor):** `SmokerSimulation` starts three smoker threads and two agent threads that share a `Tisch` (table) monitor. Synchronization uses `synchronized`, `wait()` and `notifyAll()`. After 5 seconds all threads are stopped with `interrupt()`.

**Client/server (TCP):** `TischServer` runs the table as a TCP server on port 7777. Each client gets its own worker thread, and a semaphore limits the server to 10 parallel connections. Agents and smokers are separate client processes that talk to the server over a text protocol:

| Command | Who | Meaning |
|---|---|---|
| `REGISTER AGENT` / `REGISTER SMOKER <ingredient>` | all | register with a role |
| `PUT <ingredient> <ingredient>` | agent | place two ingredients |
| `FETCH` | smoker | wait until the matching ingredients are on the table |
| `DONE` | smoker | finished smoking, table is free again |
| `QUIT` | all | close the connection |

The server answers with status codes such as `200 OK`, `201 MATCH`, `400 BAD REQUEST` and `403 FORBIDDEN`.

## Tech

Java, threads, monitors, semaphores, TCP sockets

## Running

    javac -d out src/*.java

    # local version
    java -cp out SmokerSimulation

    # client/server version (each in its own terminal)
    java -cp out TischServer
    java -cp out AgentClient
    java -cp out RaucherClient TABAK
    java -cp out RaucherClient PAPIER
    java -cp out RaucherClient STREICHHOELZER
