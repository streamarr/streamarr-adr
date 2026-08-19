package com.streamarr.spike.rebac;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.ClientCheckRequest;
import dev.openfga.sdk.api.client.model.ClientTupleKey;
import dev.openfga.sdk.api.client.model.ClientTupleKeyWithoutCondition;
import dev.openfga.sdk.api.client.model.ClientWriteRequest;
import dev.openfga.sdk.api.configuration.ClientConfiguration;
import dev.openfga.sdk.api.model.CreateStoreRequest;
import dev.openfga.sdk.api.model.WriteAuthorizationModelRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

final class OpenFgaAuthorizer implements AutoCloseable {

  private static final String USER = "user:alice";
  private static final String HOUSEHOLD = "household:home";
  private static final String PROFILE = "profile:kai";
  private static final String SERVER = "server:streamarr";

  private final GenericContainer<?> container;
  private final OpenFgaClient persistedClient;
  private final OpenFgaClient contextualClient;
  private Set<Tuple> persistedTuples = Set.of();
  private PrototypeState synchronizedState;

  OpenFgaAuthorizer(PrototypeState initialState) throws Exception {
    container =
        new GenericContainer<>(DockerImageName.parse("openfga/openfga:v1.18.3"))
            .withExposedPorts(8080)
            .withCommand("run", "--datastore-engine=memory", "--playground-enabled=false");
    container.start();

    var apiUrl = "http://" + container.getHost() + ":" + container.getMappedPort(8080);
    var model = readModel();
    persistedClient = createConfiguredClient(apiUrl, "Streamarr persisted tuples", model);
    contextualClient = createConfiguredClient(apiUrl, "Streamarr contextual tuples", model);
    synchronize(initialState);
  }

  AuthorizationDecisions decidePersisted() throws Exception {
    return decide(persistedClient, List.of());
  }

  AuthorizationDecisions decideContextual(PrototypeState state) throws Exception {
    return decide(contextualClient, clientTuples(facts(state)));
  }

  void synchronize(PrototypeState state) throws Exception {
    var desired = facts(state);
    var writes = difference(desired, persistedTuples).stream().map(Tuple::asWrite).toList();
    var deletes = difference(persistedTuples, desired).stream().map(Tuple::asDelete).toList();

    if (!writes.isEmpty() || !deletes.isEmpty()) {
      persistedClient
          .write(new ClientWriteRequest().writes(writes).deletes(deletes))
          .get();
    }
    persistedTuples = Set.copyOf(desired);
    synchronizedState = state;
  }

  PrototypeState synchronizedState() {
    return synchronizedState;
  }

  private AuthorizationDecisions decide(OpenFgaClient client, List<ClientTupleKey> contextualTuples)
      throws Exception {
    return new AuthorizationDecisions(
        isAllowed(client, "can_manage", contextualTuples),
        isAllowed(client, "can_view", contextualTuples));
  }

  private boolean isAllowed(
      OpenFgaClient client, String relation, List<ClientTupleKey> contextualTuples)
      throws Exception {
    var request =
        new ClientCheckRequest().user(USER).relation(relation)._object(PROFILE);
    if (!contextualTuples.isEmpty()) {
      request.contextualTuples(contextualTuples);
    }
    return Boolean.TRUE.equals(client.check(request).get().getAllowed());
  }

  private static OpenFgaClient createConfiguredClient(
      String apiUrl, String storeName, WriteAuthorizationModelRequest model) throws Exception {
    var client = new OpenFgaClient(new ClientConfiguration().apiUrl(apiUrl));
    var store = client.createStore(new CreateStoreRequest().name(storeName)).get();
    client.setStoreId(store.getId());
    var writtenModel = client.writeAuthorizationModel(model).get();
    client.setAuthorizationModelId(writtenModel.getAuthorizationModelId());
    return client;
  }

  private static WriteAuthorizationModelRequest readModel() throws IOException {
    try (var input =
        OpenFgaAuthorizer.class.getClassLoader().getResourceAsStream("streamarr-openfga.json")) {
      if (input == null) {
        throw new IOException("Missing prototype resource: streamarr-openfga.json");
      }
      return new ObjectMapper().readValue(input, WriteAuthorizationModelRequest.class);
    }
  }

  private static Set<Tuple> facts(PrototypeState state) {
    var facts = new HashSet<Tuple>();
    facts.add(new Tuple(SERVER, "server", PROFILE));
    facts.add(new Tuple(USER, state.householdRole().openFgaRelation(), HOUSEHOLD));
    if (state.profileSharedWithHousehold()) {
      facts.add(new Tuple(HOUSEHOLD, "shared_with", PROFILE));
    }
    if (state.directProfileManager()) {
      facts.add(new Tuple(USER, "direct_manager", PROFILE));
    }
    if (state.serverAdmin()) {
      facts.add(new Tuple(USER, "administrator", SERVER));
    }
    return Set.copyOf(facts);
  }

  private static Set<Tuple> difference(Set<Tuple> left, Set<Tuple> right) {
    var difference = new HashSet<>(left);
    difference.removeAll(right);
    return difference;
  }

  private static List<ClientTupleKey> clientTuples(Set<Tuple> tuples) {
    var result = new ArrayList<ClientTupleKey>();
    tuples.stream().map(Tuple::asWrite).forEach(result::add);
    return List.copyOf(result);
  }

  @Override
  public void close() {
    container.stop();
  }

  private record Tuple(String user, String relation, String object) {

    ClientTupleKey asWrite() {
      return new ClientTupleKey().user(user).relation(relation)._object(object);
    }

    ClientTupleKeyWithoutCondition asDelete() {
      return new ClientTupleKeyWithoutCondition().user(user).relation(relation)._object(object);
    }
  }
}
