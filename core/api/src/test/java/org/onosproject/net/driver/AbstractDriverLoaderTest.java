package org.onosproject.net.driver;

import java.util.Set;

import org.junit.Test;
import org.onosproject.net.DeviceId;

/**
 * Base test class for driver loading.
 */
public abstract class AbstractDriverLoaderTest {

    private class DriverAdminServiceAdapter implements DriverAdminService {
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
        public Set<Driver> getDrivers() {
            return null;
        }

        @Override
        public Set<Driver> getDrivers(Class<? extends Behaviour> withBehaviour) {
            return null;
        }

        @Override
        public Driver getDriver(String mfr, String hw, String sw) {
            return null;
        }

        @Override
        public Driver getDriver(DeviceId deviceId) {
            return null;
        }

        @Override
        public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
            return null;
        }

        @Override
        public Driver getDriver(String driverName) {
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
