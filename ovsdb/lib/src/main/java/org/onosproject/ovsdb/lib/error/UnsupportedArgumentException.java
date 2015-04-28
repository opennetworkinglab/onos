/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.onosproject.ovsdb.lib.error;

public class UnsupportedArgumentException extends RuntimeException {

    public UnsupportedArgumentException(String message) {
        super(message);
    }

    public UnsupportedArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
