package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * Test of the network element identifier.
 */
public class ElementIdTest {

    private static class FooId extends ElementId {
        public FooId(URI uri) {
            super(uri);
        }
    }

    public static URI uri(String str) {
        return URI.create(str);
    }

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(new FooId(uri("of:foo")),
                                  new FooId(uri("of:foo")))
                .addEqualityGroup(new FooId(uri("of:bar")))
                .testEquals();
        assertEquals("wrong uri", uri("ofcfg:foo"),
                     new FooId(uri("ofcfg:foo")).uri());
    }

}
