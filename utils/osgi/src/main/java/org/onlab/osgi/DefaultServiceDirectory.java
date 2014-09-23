package org.onlab.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

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
        if (bc != null) {
            ServiceReference<T> reference = bc.getServiceReference(serviceClass);
            if (reference != null) {
                T impl = bc.getService(reference);
                if (impl != null) {
                    return impl;
                }
            }
        }
        throw new ServiceNotFoundException("Service " + serviceClass.getName() + " not found");
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        return getService(serviceClass);
    }

}
