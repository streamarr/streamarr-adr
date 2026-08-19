package com.streamarr.spike.rebac;

record PrototypeState(
    HouseholdRole householdRole,
    boolean profileSharedWithHousehold,
    boolean directProfileManager,
    boolean serverAdmin) {

  static PrototypeState initial() {
    return new PrototypeState(HouseholdRole.MEMBER, true, false, false);
  }

  PrototypeState cycleRole() {
    return new PrototypeState(
        householdRole.next(),
        profileSharedWithHousehold,
        directProfileManager,
        serverAdmin);
  }

  PrototypeState toggleShare() {
    return new PrototypeState(
        householdRole, !profileSharedWithHousehold, directProfileManager, serverAdmin);
  }

  PrototypeState toggleDirectManager() {
    return new PrototypeState(
        householdRole, profileSharedWithHousehold, !directProfileManager, serverAdmin);
  }

  PrototypeState toggleServerAdmin() {
    return new PrototypeState(
        householdRole, profileSharedWithHousehold, directProfileManager, !serverAdmin);
  }

  enum HouseholdRole {
    MEMBER,
    ADMIN,
    OWNER;

    HouseholdRole next() {
      return switch (this) {
        case MEMBER -> ADMIN;
        case ADMIN -> OWNER;
        case OWNER -> MEMBER;
      };
    }

    String openFgaRelation() {
      return name().toLowerCase();
    }
  }
}
