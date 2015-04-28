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

import org.onosproject.ovsdb.lib.notation.OvsdbSet;
import org.onosproject.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public  class OvsdbTypesIdResolver implements TypeIdResolver {

        private JavaType baseType;

        @Override
        public void init(JavaType bt) {
            this.baseType = bt;
        }

        @Override
        public String idFromValue(Object value) {
            throw new UnsupportedOperationException("not yet done");
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            throw new UnsupportedOperationException("not yet done");
        }

        @Override
        public String idFromBaseType() {
            throw new UnsupportedOperationException("not yet done");
        }

        @Override
        public JavaType typeFromId(String id) {
            if ("set".equals(id)) {
                return TypeFactory.defaultInstance().constructCollectionType(OvsdbSet.class, Object.class);
            } else if ("uuid".equals(id) || "named-uuid".equals(id)) {
                return TypeFactory.defaultInstance().constructType(UUID.class);
            }
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            throw new UnsupportedOperationException("not yet done");
        }
    }