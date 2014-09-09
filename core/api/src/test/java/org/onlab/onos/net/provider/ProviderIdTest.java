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
                .addEqualityGroup(new ProviderId("foo"), new ProviderId("foo"))
                .addEqualityGroup(new ProviderId("bar"))
                .testEquals();
    }

}
