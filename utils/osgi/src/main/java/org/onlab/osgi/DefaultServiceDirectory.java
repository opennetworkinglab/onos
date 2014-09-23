package org.onlab.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Default implementation of the service directory using OSGi framework utilities.
 */
public class DefaultServiceDirectory implements ServiceDirectory {

    /**
     * Returns the reference to the implementation of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     */
    public static <T> T getService(Class<T> serviceClass) {
        BundleContext bc = FrameworkUtil.getBundle(serviceClass).getBundleContext();
        T impl = bc.getService(bc.getServiceReference(serviceClass));
        if (impl == null) {
            throw new ServiceNotFoundException("Service " + serviceClass.getName() + " not found");
        }
        return impl;
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        return getService(serviceClass);
    }

}
