package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * Test of the provider identifier.
 */
public class ElementIdTest {

    public static URI uri(String str) {
        return URI.create(str);
    }

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(new ElementId(uri("of:foo")),
                                  new ElementId(uri("of:foo")))
                .addEqualityGroup(new ElementId(uri("of:bar")))
                .testEquals();
        assertEquals("wrong uri", uri("ofcfg:foo"),
                     new ElementId(uri("ofcfg:foo")).uri());
    }

}
