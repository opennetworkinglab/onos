/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.onosproject.ovsdb.lib.schema.typed;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.onosproject.ovsdb.lib.notation.Version;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TypedColumn {
    public String name();

    public MethodType method();

    public String fromVersion() default Version.NULL_VERSION_STRING;

    public String untilVersion() default Version.NULL_VERSION_STRING;
}
