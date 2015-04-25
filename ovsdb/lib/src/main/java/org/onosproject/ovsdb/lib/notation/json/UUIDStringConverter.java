/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.notation.json;

import org.onosproject.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.databind.util.StdConverter;

public class UUIDStringConverter extends StdConverter<String, UUID> {

    @Override
    public UUID convert(String value) {
        return new UUID(value);
    }

}
