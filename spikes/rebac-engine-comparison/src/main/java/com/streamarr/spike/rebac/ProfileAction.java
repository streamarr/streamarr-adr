package com.streamarr.spike.rebac;

enum ProfileAction {
  EDIT_PROFILE("editProfile"),
  VIEW_PROFILE("viewProfile"),
  OFFER_PROFILE_SHARE("offerProfileShare"),
  INVITE_PROFILE_MANAGER("inviteProfileManager"),
  INITIATE_PROFILE_CLAIM("initiateProfileClaim"),
  MIGRATE_PROFILE_LIFECYCLE("migrateProfileLifecycle");

  private final String cedarName;

  ProfileAction(String cedarName) {
    this.cedarName = cedarName;
  }

  String cedarName() {
    return cedarName;
  }
}
