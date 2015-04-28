/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.notation;

public enum Mutator {
    SUM("+="),
    DIFFERENCE("-="),
    PRODUCT("*="),
    QUOTIENT("/="),
    REMAINDER("%="),
    INSERT("insert"),
    DELETE("delete");

    private Mutator(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}
