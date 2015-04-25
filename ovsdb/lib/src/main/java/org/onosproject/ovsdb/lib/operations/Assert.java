/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.onosproject.ovsdb.lib.operations;

public class Assert extends Operation {

    public static final String ASSERT = "assert";
    String lock;

    public Assert(String lock) {
        super(null, ASSERT);
        this.lock = lock;
    }

    public String getLock() {
        return lock;
    }
}
