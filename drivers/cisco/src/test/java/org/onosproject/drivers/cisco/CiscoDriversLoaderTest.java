package org.onosproject.drivers.cisco;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;


/**
 * Cisco drivers loader test.
 */
public class CiscoDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new CiscoDriversLoader();
    }
}
