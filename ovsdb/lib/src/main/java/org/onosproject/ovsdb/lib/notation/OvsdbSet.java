/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.notation;

import java.util.Set;

import org.onosproject.ovsdb.lib.notation.json.Converter;
import org.onosproject.ovsdb.lib.notation.json.OvsdbSetSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;

@JsonDeserialize(converter = Converter.SetConverter.class)
@JsonSerialize(using = OvsdbSetSerializer.class)
public class OvsdbSet<T> extends ForwardingSet<T> {

    Set<T> target = null;

    public OvsdbSet() {
        this(Sets.<T>newHashSet());
    }

    public OvsdbSet(Set<T> backing) {
        this.target = backing;
    }

    @Override
    public Set<T> delegate() {
        return target;
    }

    public static <D> OvsdbSet<D> fromSet(Set<D> value) {
        return new OvsdbSet<>(value);
    }
}
