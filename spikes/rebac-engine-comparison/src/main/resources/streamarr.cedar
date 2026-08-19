permit(
    principal is Account,
    action == Action::"editProfile",
    resource is Profile
) when {
    principal.serverAdmin ||
    resource.directManagers.contains(principal) ||
    (
        resource.sharedHouseholds.contains(principal.homeHousehold) &&
        principal.householdRole == "ADMIN"
    )
};

permit(
    principal is Account,
    action == Action::"viewProfile",
    resource is Profile
) when {
    resource.sharedHouseholds.contains(principal.homeHousehold)
};

permit(
    principal is Account,
    action in Action::"portableRelationshipMutation",
    resource is Profile
) when {
    principal.serverAdmin ||
    resource.directManagers.contains(principal)
};

forbid(
    principal is Account,
    action in Action::"portableRelationshipMutation",
    resource is Profile
) unless {
    principal.serverAdmin ||
    resource.directManagers.contains(principal)
};
