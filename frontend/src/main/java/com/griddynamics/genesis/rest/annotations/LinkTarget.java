package com.griddynamics.genesis.rest.annotations;

public enum LinkTarget {
    SELF, COLLECTION, ACTION, LOGOUT;

    public String toRel() {
        return toString().toLowerCase();
    }
}
