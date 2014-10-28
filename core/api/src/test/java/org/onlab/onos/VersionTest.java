package org.onlab.onos;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.onos.core.Version;

import static org.junit.Assert.*;
import static org.onlab.onos.core.Version.version;

/**
 * Tests of the version descriptor.
 */
public class VersionTest {

    @Test
    public void fromParts() {
        Version v = version(1, 2, 3, "4321");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 2, v.minor());
        assertEquals("wrong patch", 3, v.patch());
        assertEquals("wrong build", "4321", v.build());
    }

    @Test
    public void fromString() {
        Version v = version("1.2.3.4321");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 2, v.minor());
        assertEquals("wrong patch", 3, v.patch());
        assertEquals("wrong build", "4321", v.build());
    }

    @Test
    public void snapshot() {
        Version v = version("1.2.3-SNAPSHOT");
        assertEquals("wrong major", 1, v.major());
        assertEquals("wrong minor", 2, v.minor());
        assertEquals("wrong patch", 3, v.patch());
        assertEquals("wrong build", "SNAPSHOT", v.build());
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(version("1.2.3.4321"), version(1, 2, 3, "4321"))
                .addEqualityGroup(version("1.9.3.4321"), version(1, 9, 3, "4321"))
                .addEqualityGroup(version("1.2.8.4321"), version(1, 2, 8, "4321"))
                .addEqualityGroup(version("1.2.3.x"), version(1, 2, 3, "x"))
                .testEquals();
    }
}