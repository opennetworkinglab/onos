package org.onosproject.drivers.corsa;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * Corsa drivers loader test.
 */
public class CorsaDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new CorsaDriversLoader();
    }
}
