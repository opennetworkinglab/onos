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

public class Commit extends Operation {

    public static final String COMMIT = "commit";
    Boolean durable;

    public Commit(Boolean durable) {
        super(null, COMMIT);
        this.durable = durable;
    }

    public Boolean isDurable() {
        return durable;
    }
}
