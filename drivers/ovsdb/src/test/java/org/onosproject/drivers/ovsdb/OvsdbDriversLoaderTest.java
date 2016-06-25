package org.onosproject.drivers.ovsdb;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * OVSDB drivers loader test.
 */
public class OvsdbDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new OvsdbDriversLoader();
    }
}
