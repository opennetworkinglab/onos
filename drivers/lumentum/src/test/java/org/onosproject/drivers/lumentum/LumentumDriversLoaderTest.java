package org.onosproject.drivers.lumentum;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * Lumentum drivers loader test.
 */
public class LumentumDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new LumentumDriversLoader();
    }
}
