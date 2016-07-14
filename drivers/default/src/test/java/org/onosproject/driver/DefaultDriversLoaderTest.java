package org.onosproject.driver;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;

/**
 * Default drivers loader test.
 */
public class DefaultDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new DefaultDriversLoader();
    }
}
