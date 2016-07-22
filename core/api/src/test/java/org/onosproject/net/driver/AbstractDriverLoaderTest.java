package org.onosproject.net.driver;

import org.junit.Test;

import java.util.Set;

/**
 * Base test class for driver loading.
 */
public abstract class AbstractDriverLoaderTest {

    private class DriverAdminServiceAdapter extends DriverServiceAdapter implements DriverAdminService {
        @Override
        public Set<DriverProvider> getProviders() {
            return null;
        }

        @Override
        public void registerProvider(DriverProvider provider) {
        }

        @Override
        public void unregisterProvider(DriverProvider provider) {
        }

        @Override
        public Class<? extends Behaviour> getBehaviourClass(String className) {
            return null;
        }
    }

    protected AbstractDriverLoader loader;

    @Test
    public void testLoader() {
        loader.driverAdminService = new DriverAdminServiceAdapter();
        loader.activate();
        loader.deactivate();
    }
}
