package com.griddynamics.genesis.rest.annotations;

public enum LinkTarget {
    SELF, COLLECTION;

    public String toRel() {
        return toString().toLowerCase();
    }
}
