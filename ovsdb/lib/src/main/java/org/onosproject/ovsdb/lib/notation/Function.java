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

public enum Function {
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_THAN(">="),
    GREATER_THAN_OR_EQUALS(">="),
    INCLUDES("includes"),
    EXCLUDES("excludes");

    private Function(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}
