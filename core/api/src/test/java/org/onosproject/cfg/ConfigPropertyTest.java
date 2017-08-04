/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.cfg;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.cfg.ConfigProperty.Type;

import static org.junit.Assert.*;
import static org.onosproject.cfg.ConfigProperty.Type.*;
import static org.onosproject.cfg.ConfigProperty.defineProperty;
import static org.onosproject.cfg.ConfigProperty.resetProperty;
import static org.onosproject.cfg.ConfigProperty.setProperty;

/**
 * Set of tests of the configuration property class.
 */
public class ConfigPropertyTest {

    @Test
    public void basics() {
        ConfigProperty p = defineProperty("foo", STRING, "bar", "Foo Prop");
        validate(p, "foo", STRING, "bar", "bar");
        p = setProperty(p, "BAR");
        validate(p, "foo", STRING, "BAR", "bar");
        p = resetProperty(p);
        validate(p, "foo", STRING, "bar", "bar");
    }

    @Test
    public void equality() {
        new EqualsTester()
                .addEqualityGroup(defineProperty("foo", STRING, "bar", "Desc"),
                                  defineProperty("foo", STRING, "goo", "Desc"))
                .addEqualityGroup(defineProperty("bar", STRING, "bar", "Desc"),
                                  defineProperty("bar", STRING, "goo", "Desc"))
                .testEquals();
    }

    private void validate(ConfigProperty p, String name, Type type, String v, String dv) {
        assertEquals("incorrect name", name, p.name());
        assertEquals("incorrect type", type, p.type());
        assertEquals("incorrect value", v, p.value());
        assertEquals("incorrect default", dv, p.defaultValue());
        assertEquals("incorrect description", "Foo Prop", p.description());
    }

    @Test
    public void asInteger() {
        ConfigProperty p = defineProperty("foo", INTEGER, "123", "Foo Prop");
        validate(p, "foo", INTEGER, "123", "123");
        assertEquals("incorrect value", 123, p.asInteger());
        assertEquals("incorrect value", 123L, p.asLong());
    }

    @Test
    public void asLong() {
        ConfigProperty p = defineProperty("foo", LONG, "123", "Foo Prop");
        validate(p, "foo", LONG, "123", "123");
        assertEquals("incorrect value", 123L, p.asLong());
    }

    @Test
    public void asFloat() {
        ConfigProperty p = defineProperty("foo", FLOAT, "123.0", "Foo Prop");
        validate(p, "foo", FLOAT, "123.0", "123.0");
        assertEquals("incorrect value", 123.0, p.asFloat(), 0.01);
        assertEquals("incorrect value", 123.0, p.asDouble(), 0.01);
    }

    @Test
    public void asDouble() {
        ConfigProperty p = defineProperty("foo", DOUBLE, "123.0", "Foo Prop");
        validate(p, "foo", DOUBLE, "123.0", "123.0");
        assertEquals("incorrect value", 123.0, p.asDouble(), 0.01);
    }

    @Test
    public void asBoolean() {
        ConfigProperty p = defineProperty("foo", BOOLEAN, "true", "Foo Prop");
        validate(p, "foo", BOOLEAN, "true", "true");
        assertEquals("incorrect value", true, p.asBoolean());
    }
}