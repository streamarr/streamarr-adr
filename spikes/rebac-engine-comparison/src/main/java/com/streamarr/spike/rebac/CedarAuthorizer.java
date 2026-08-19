package com.streamarr.spike.rebac;

import com.cedarpolicy.AuthorizationEngine;
import com.cedarpolicy.BasicAuthorizationEngine;
import com.cedarpolicy.model.AuthorizationRequest;
import com.cedarpolicy.model.Context;
import com.cedarpolicy.model.ValidationRequest;
import com.cedarpolicy.model.entity.Entities;
import com.cedarpolicy.model.entity.Entity;
import com.cedarpolicy.model.policy.PolicySet;
import com.cedarpolicy.model.schema.Schema;
import com.cedarpolicy.value.CedarList;
import com.cedarpolicy.value.EntityUID;
import com.cedarpolicy.value.PrimBool;
import com.cedarpolicy.value.PrimString;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class CedarAuthorizer {

  private static final EntityUID ACCOUNT = uid("Account", "alice");
  private static final EntityUID HOUSEHOLD = uid("Household", "home");
  private static final EntityUID PROFILE = uid("Profile", "kai");

  private final AuthorizationEngine engine = new BasicAuthorizationEngine();
  private final PolicySet policies;
  private final Schema schema;

  CedarAuthorizer() throws Exception {
    policies = PolicySet.parsePolicies(readResource("streamarr.cedar"));
    schema = Schema.parse(Schema.JsonOrCedar.Cedar, readResource("streamarr.cedarschema"));
    var validation = engine.validate(new ValidationRequest(schema, policies));
    if (!validation.validationPassed()) {
      throw new IllegalStateException("Invalid Cedar prototype policy: " + validation);
    }
  }

  AuthorizationDecisions decide(PrototypeState state) throws Exception {
    return new AuthorizationDecisions(
        isAllowed(state, ProfileAction.EDIT_PROFILE),
        isAllowed(state, ProfileAction.VIEW_PROFILE));
  }

  boolean isAllowed(PrototypeState state, ProfileAction action) throws Exception {
    var account =
        new Entity(
            ACCOUNT,
            Map.of(
                "serverAdmin",
                new PrimBool(state.serverAdmin()),
                "homeHousehold",
                HOUSEHOLD,
                "householdRole",
                new PrimString(state.householdRole().name())),
            Set.of());
    var sharedHouseholds =
        state.profileSharedWithHousehold()
            ? new CedarList(List.of(HOUSEHOLD))
            : new CedarList();
    var directManagers =
        state.directProfileManager() ? new CedarList(List.of(ACCOUNT)) : new CedarList();
    var profile =
        new Entity(
            PROFILE,
            Map.of(
                "sharedHouseholds", sharedHouseholds,
                "directManagers", directManagers),
            Set.of());
    var entities = new Entities(Set.of(account, profile));

    return isAllowed(account, action.cedarName(), profile, entities);
  }

  private boolean isAllowed(
      Entity principal, String actionName, Entity resource, Entities entities) throws Exception {
    var action = new Entity(uid("Action", actionName));
    var request =
        new AuthorizationRequest(
            principal, action, resource, new Context(), Optional.of(schema), true);
    var response = engine.isAuthorized(request, policies, entities);
    return response.success.orElseThrow().isAllowed();
  }

  private static EntityUID uid(String type, String id) {
    return EntityUID.parse(type + "::\"" + id + "\"").orElseThrow();
  }

  private static String readResource(String name) throws IOException {
    try (var input = CedarAuthorizer.class.getClassLoader().getResourceAsStream(name)) {
      if (input == null) {
        throw new IOException("Missing prototype resource: " + name);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
