package com.tertech.tkenlightment.membership.notification.rest.dtos;

/** Audience for an admin announcement. */
public enum RecipientGroup {
    /** Only members whose status is ACTIVE. */
    ACTIVE_MEMBERS,
    /** Every member on the roster, regardless of status. */
    ALL_MEMBERS
}
