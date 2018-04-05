/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.core.Version;

import static org.junit.Assert.*;
import static org.onosproject.core.Version.version;

/**
 * Tests of the version descriptor.
 */
public class VersionTest {

    @Test
    public void fromParts() {
        Version v = version(1, 2, "3", "4321");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 2, v.minor());
        assertEquals("wrong patch", "3", v.patch());
        assertEquals("wrong build", "4321", v.build());
    }

    @Test
    public void fromString() {
        Version v = version("1.2.3.4321");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 2, v.minor());
        assertEquals("wrong patch", "3", v.patch());
        assertEquals("wrong build", "4321", v.build());
    }

    @Test
    public void snapshot() {
        Version v = version("1.2.3-SNAPSHOT");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 2, v.minor());
        assertEquals("wrong patch", "3", v.patch());
        assertEquals("wrong build", "SNAPSHOT", v.build());
    }

    @Test
    public void shortNumber() {
        Version v = version("1.2.3");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 2, v.minor());
        assertEquals("wrong patch", "3", v.patch());
        assertEquals("wrong build", null, v.build());
    }

    @Test
    public void minimal() {
        Version v = version("1.4");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 4, v.minor());
        assertEquals("wrong patch", null, v.patch());
        assertEquals("wrong build", null, v.build());
    }

    @Test
    public void testToFromInt() {
        Version version1;
        Version version2;

        version1 = version("1.2");
        version2 = Version.fromInt(version1.toInt());
        assertEquals(version2, version(1, 2, "0", "0"));

        version1 = version("1.2.foo.bar");
        version2 = Version.fromInt(version1.toInt());
        assertEquals(version2, version(1, 2, "0", "0"));

        version1 = version("1.2.3");
        version2 = Version.fromInt(version1.toInt());
        assertEquals(version2, version(1, 2, "3", "0"));

        version1 = version("1.2.3-SNAPSHOT");
        version2 = Version.fromInt(version1.toInt());
        assertEquals(version2, version(1, 2, "3", "0"));

        version1 = version("255.254.253.252");
        version2 = Version.fromInt(version1.toInt());
        assertEquals(version2, version(255, 254, "253", "252"));

        assertTrue(version("0.0.2").toInt() > version("0.0.1").toInt());
        assertTrue(version("0.1.0").toInt() > version("0.0.1").toInt());
        assertTrue(version("1.0.0").toInt() > version("0.1.0").toInt());
        assertTrue(version("1.1.0").toInt() > version("1.0.1").toInt());
        assertTrue(version("2.1.1").toInt() > version("1.10.10").toInt());
        assertTrue(version("0.1.0-rc2").toInt() > version("0.1.0-rc1").toInt());
    }

    @Test
    public void testOrder() {
        List<Version> versions = Arrays.asList(
            version("0.1.0"),
            version("1.0.1"),
            version("0.0.1"),
            version("1.0.0"),
            version("1.1.1"));
        Collections.sort(versions);
        assertEquals(versions, Arrays.asList(
            version("0.0.1"),
            version("0.1.0"),
            version("1.0.0"),
            version("1.0.1"),
            version("1.1.1")));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(version("1.2.3.4321"), version(1, 2, "3", "4321"))
                .addEqualityGroup(version("1.9.3.4321"), version(1, 9, "3", "4321"))
                .addEqualityGroup(version("1.2.8.4321"), version(1, 2, "8", "4321"))
                .addEqualityGroup(version("1.2.3.x"), version(1, 2, "3", "x"))
                .testEquals();
    }
}
