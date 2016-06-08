package org.onosproject.drivers.fujitsu;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * Fujistu driver loader test.
 */
public class FujitsuDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new FujitsuDriversLoader();
    }
}
