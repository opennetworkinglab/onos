package org.onosproject.drivers.bti;


import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * BTI Drivers loader test.
 */
public class BtiDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new BtiDriversLoader();
    }
}
