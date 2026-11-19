# Naval Strategy Lab

A browser-based naval tactics project built with Java 21, Spring Boot and vanilla JavaScript. It models fleet placement, turn-based combat and a shared campaign, with live updates between browser views.

## Try it locally

Install JDK 21 or newer, then open a terminal in this directory. The Maven Wrapper downloads Maven and dependencies on the first run.

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, run `sh ./mvnw spring-boot:run`. Open [localhost:8080](http://localhost:8080). The interface is in Spanish.

1. Open an existing battle or select **Iniciar nueva batalla**.
2. Configure and place a fleet, then fire at the opponent's board.
3. Open the campaign in a second tab to see its live updates.

Game state is held in memory and resets when the server restarts. This is a local academic portfolio project.

## Engineering highlights

- The domain model validates fleet placement, board coordinates and battle state transitions. The combat flow checks the current round before applying a move.
- Opponent tactics use a common strategy interface. The project includes sequential, spiral and hunt-and-target implementations.
- Spring MVC controllers handle user actions. Presenters convert domain state into browser commands, and Server-Sent Events (SSE) send updates to connected views.
- The browser is served by Spring Boot. No external database, account or API key is required.

## Tests

```powershell
.\mvnw.cmd test
node --test src/test/js/juego.test.mjs
```

On macOS or Linux, use `sh ./mvnw test`. The Java tests cover domain rules, tactics and local HTTP handlers. The Node.js suite covers browser-side state and event handling without installing packages.
