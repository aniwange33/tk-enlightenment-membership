package com.tertech.tkenlightment.membership.shared.domain.exceptions;

public class MemberAlreadyExistsException extends DomainException {

    public MemberAlreadyExistsException(String email) {
        super("Member with email '" + email + "' already exists");
    }
}
