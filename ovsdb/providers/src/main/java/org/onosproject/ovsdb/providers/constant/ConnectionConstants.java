package org.onosproject.ovsdb.providers.constant;

public enum ConnectionConstants {
    ADDRESS("address"), PORT("port"), PROTOCOL("protocol"),
    USERNAME("username"), PASSWORD("password");

    private String name;

    private ConnectionConstants(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
