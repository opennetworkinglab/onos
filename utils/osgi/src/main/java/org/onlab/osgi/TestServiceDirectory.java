package org.onlab.osgi;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * Service directory implementation suitable for testing.
 */
@SuppressWarnings("unchecked")
public class TestServiceDirectory implements ServiceDirectory {

    private ClassToInstanceMap<Object> services = MutableClassToInstanceMap.create();

    @Override
    public <T> T get(Class<T> serviceClass) {
        return services.getInstance(serviceClass);
    }

    /**
     * Adds a new service to the directory.
     *
     * @param serviceClass service class
     * @param service service instance
     * @return self
     */
    public TestServiceDirectory add(Class serviceClass, Object service) {
        services.putInstance(serviceClass, service);
        return this;
    }

}
