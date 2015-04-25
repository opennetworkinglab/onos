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

/**
 * This is a generic exception thrown by the Typed Schema utilities.
 */
public class TyperException extends RuntimeException {

    public TyperException(String message) {
        super(message);
    }

    public TyperException(String message, Throwable cause) {
        super(message, cause);
    }
}
