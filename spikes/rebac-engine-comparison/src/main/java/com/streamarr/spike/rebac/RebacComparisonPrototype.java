package com.streamarr.spike.rebac;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class RebacComparisonPrototype {

  private static final String CLEAR = "\033[2J\033[H";
  private static final String BOLD = "\033[1m";
  private static final String DIM = "\033[2m";
  private static final String RESET = "\033[0m";

  private RebacComparisonPrototype() {}

  public static void main(String[] args) throws Exception {
    var state = PrototypeState.initial();
    var cedar = new CedarAuthorizer();
    System.out.println("Starting disposable OpenFGA container...");

    try (var openFga = new OpenFgaAuthorizer(state);
        var input = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        render(state, cedar, openFga);
        var line = input.readLine();
        if (line == null || line.equalsIgnoreCase("q")) {
          return;
        }

        state =
            switch (line.toLowerCase()) {
              case "r" -> state.cycleRole();
              case "s" -> state.toggleShare();
              case "m" -> state.toggleDirectManager();
              case "a" -> state.toggleServerAdmin();
              case "y" -> {
                openFga.synchronize(state);
                yield state;
              }
              default -> state;
            };
      }
    }
  }

  private static void render(
      PrototypeState databaseState, CedarAuthorizer cedar, OpenFgaAuthorizer openFga)
      throws Exception {
    var cedarDecision = cedar.decide(databaseState);
    var contextualDecision = openFga.decideContextual(databaseState);
    var persistedDecision = openFga.decidePersisted();
    var synchronizedState = openFga.synchronizedState();

    System.out.print(CLEAR);
    System.out.println(BOLD + "PROTOTYPE — Streamarr authorization engines" + RESET);
    System.out.println(DIM + "Alice acts on profile Kai in household Home" + RESET);
    System.out.println();
    printState("Streamarr request facts", databaseState);
    printState("OpenFGA stored tuples", synchronizedState);
    System.out.println(
        BOLD
            + "Stores agree: "
            + RESET
            + (databaseState.equals(synchronizedState) ? "yes" : "NO — authorization drift"));
    System.out.println();
    System.out.println(BOLD + "Authorization decisions" + RESET);
    printDecision("Cedar (Streamarr request facts)", cedarDecision);
    printDecision("OpenFGA (contextual request facts)", contextualDecision);
    printDecision("OpenFGA (stored relationship tuples)", persistedDecision);
    System.out.println();
    System.out.println(
        DIM
            + "Direct management grants portable authority. Viewing still requires an active local share."
            + RESET);
    System.out.println();
    System.out.println(
        BOLD
            + "[r]"
            + RESET
            + " role  "
            + BOLD
            + "[s]"
            + RESET
            + " toggle PostgreSQL share  "
            + BOLD
            + "[m]"
            + RESET
            + " direct manager");
    System.out.println(
        BOLD
            + "[a]"
            + RESET
            + " ServerAdmin  "
            + BOLD
            + "[y]"
            + RESET
            + " synchronize OpenFGA  "
            + BOLD
            + "[q]"
            + RESET
            + " quit");
    System.out.print("> ");
    System.out.flush();
  }

  private static void printState(String title, PrototypeState state) {
    System.out.println(BOLD + title + RESET);
    System.out.println("  household role: " + state.householdRole());
    System.out.println("  profile actively shared: " + yesNo(state.profileSharedWithHousehold()));
    System.out.println("  direct ProfileManager: " + yesNo(state.directProfileManager()));
    System.out.println("  ServerAdmin: " + yesNo(state.serverAdmin()));
  }

  private static void printDecision(String engine, AuthorizationDecisions decision) {
    System.out.printf(
        "  %-41s edit=%-3s view=%-3s%n",
        engine, yesNo(decision.canEdit()), yesNo(decision.canView()));
  }

  private static String yesNo(boolean value) {
    return value ? "yes" : "no";
  }
}
