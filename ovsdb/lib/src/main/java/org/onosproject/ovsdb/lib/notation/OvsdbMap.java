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

import java.util.Map;

import org.onosproject.ovsdb.lib.notation.json.Converter;
import org.onosproject.ovsdb.lib.notation.json.OvsdbMapSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

@JsonDeserialize(converter = Converter.MapConverter.class)
@JsonSerialize(using = OvsdbMapSerializer.class)
public class OvsdbMap<K, V> extends ForwardingMap<K, V> {

    Map<K, V> target = Maps.newHashMap();

    public OvsdbMap() {
        this(Maps.<K, V>newHashMap());
    }

    public OvsdbMap(Map<K, V> value) {
        this.target = value;
    }

    @Override
    public Map<K, V> delegate() {
        return target;
    }

    public static <K, V> OvsdbMap<K, V> fromMap(Map<K, V> value) {
        return new OvsdbMap<K, V>(value);
    }
}
