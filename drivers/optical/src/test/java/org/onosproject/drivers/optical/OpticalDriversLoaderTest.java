package org.onosproject.drivers.optical;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * Optical drivers loader test.
 */
public class OpticalDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new OpticalDriversLoader();
    }
}
