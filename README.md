# Smoker Problem – Client/Server

Java implementation of the classic cigarette smokers problem, a synchronization problem from operating systems, as a client/server application.

Individual project in the course Computer Networks & Operating Systems at HAW Hamburg.

## How it works

- A central **table server** (`TischServer`) manages the shared table.
- Clients connect over the network and register with a role: a smoker with one ingredient, or the agent who places two ingredients on the table.
- The server handles each client in its own **worker thread** and synchronizes access to the table.
- Communication uses a simple text-based protocol with status responses (e.g. `400 BAD REQUEST` for invalid input).

## Tech

Java, threads and synchronization, network sockets

## Running

1. Start `TischServer`
2. Start one `AgentClient` and the `RaucherClient`s (one per ingredient)

`SmokerSimulation` runs a local version of the problem without the network.
