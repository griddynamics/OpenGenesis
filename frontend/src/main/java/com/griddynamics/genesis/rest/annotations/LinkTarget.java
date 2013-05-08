package com.griddynamics.genesis.rest.annotations;

public enum LinkTarget {
    SELF, COLLECTION, ACTION, LOGOUT, DOWNLOAD;

    public String toRel() {
        return toString().toLowerCase();
    }
}
