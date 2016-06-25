package org.onosproject.drivers.netconf;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * Netconf drivers loader test.
 */
public class NetconfDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new NetconfDriversLoader();
    }
}
