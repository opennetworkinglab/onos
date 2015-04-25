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
 * This exception is thrown when a result does not meet any of the known formats
 * in RFC7047.
 */
public class UnexpectedResultException extends RuntimeException {
    public UnexpectedResultException(String message) {
        super(message);
    }

    public UnexpectedResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
