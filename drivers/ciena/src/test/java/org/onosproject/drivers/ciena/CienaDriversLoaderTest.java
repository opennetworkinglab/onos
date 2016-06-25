package org.onosproject.drivers.ciena;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * Ciena drivers loader test.
 */
public class CienaDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new CienaDriversLoader();
    }
}
