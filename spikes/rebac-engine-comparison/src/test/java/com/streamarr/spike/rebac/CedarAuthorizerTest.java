package com.streamarr.spike.rebac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("UnitTest")
class CedarAuthorizerTest {

  private final CedarAuthorizer authorizer;

  CedarAuthorizerTest() throws Exception {
    authorizer = new CedarAuthorizer();
  }

  @Test
  @DisplayName("Should allow Profile editing when authority derives from an active Household share")
  void shouldAllowProfileEditingWhenAuthorityDerivesFromActiveHouseholdShare() throws Exception {
    var householdAdmin =
        new PrototypeState(PrototypeState.HouseholdRole.ADMIN, true, false, false);

    assertTrue(authorizer.isAllowed(householdAdmin, ProfileAction.EDIT_PROFILE));
  }

  @Test
  @DisplayName("Should deny derived Profile editing when token carries the removed OWNER role")
  void shouldDenyDerivedProfileEditingWhenTokenCarriesRemovedOwnerRole() throws Exception {
    var removedOwnerRole =
        new PrototypeState(PrototypeState.HouseholdRole.OWNER, true, false, false);

    assertFalse(authorizer.isAllowed(removedOwnerRole, ProfileAction.EDIT_PROFILE));
  }

  @Test
  @DisplayName("Should allow offering a Profile when Account is a direct ProfileManager")
  void shouldAllowOfferingProfileWhenAccountIsDirectProfileManager() throws Exception {
    var directManager =
        new PrototypeState(PrototypeState.HouseholdRole.MEMBER, false, true, false);

    assertTrue(authorizer.isAllowed(directManager, ProfileAction.OFFER_PROFILE_SHARE));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("authorityCreatingActions")
  @DisplayName(
      "Should require direct authority when an action creates a portable Profile relationship")
  void shouldRequireDirectAuthorityWhenActionCreatesPortableProfileRelationship(
      ProfileAction action) throws Exception {
    var derivedHouseholdAdmin =
        new PrototypeState(PrototypeState.HouseholdRole.ADMIN, true, false, false);
    var directManager =
        new PrototypeState(PrototypeState.HouseholdRole.MEMBER, false, true, false);
    var serverAdmin =
        new PrototypeState(PrototypeState.HouseholdRole.MEMBER, false, false, true);

    assertFalse(authorizer.isAllowed(derivedHouseholdAdmin, action));
    assertTrue(authorizer.isAllowed(directManager, action));
    assertTrue(authorizer.isAllowed(serverAdmin, action));
  }

  private static Stream<ProfileAction> authorityCreatingActions() {
    return Stream.of(
        ProfileAction.OFFER_PROFILE_SHARE,
        ProfileAction.INVITE_PROFILE_MANAGER,
        ProfileAction.INITIATE_PROFILE_CLAIM,
        ProfileAction.MIGRATE_PROFILE_LIFECYCLE);
  }
}
