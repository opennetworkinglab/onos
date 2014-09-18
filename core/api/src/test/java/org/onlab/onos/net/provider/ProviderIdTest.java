package org.onlab.onos.net.provider;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test of the provider identifier.
 */
public class ProviderIdTest {

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(new ProviderId("of", "foo"), new ProviderId("of", "foo"))
                .addEqualityGroup(new ProviderId("snmp", "foo"), new ProviderId("snmp", "foo"))
                .addEqualityGroup(new ProviderId("of", "bar"))
                .testEquals();
    }

}
