/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cfg.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onosproject.cfg.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.cfg.ConfigProperty.Type.STRING;
import static org.onosproject.cfg.ConfigProperty.defineProperty;
import static org.onosproject.cfg.impl.ConfigPropertyDefinitions.read;
import static org.onosproject.cfg.impl.ConfigPropertyDefinitions.write;

/**
 * Tests of the config property definitions utility.
 */
public class ConfigPropertyDefinitionsTest {

    @Test
    public void basics() throws IOException {
        Set<ConfigProperty> original = ImmutableSet
                .of(defineProperty("foo", STRING, "dingo", "FOO"),
                    defineProperty("bar", STRING, "bat", "BAR"));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        write(out, original);
        Set<ConfigProperty> read = read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("incorrect defs", original, read);
    }

}