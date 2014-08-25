package org.onlab.rest;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

/**
 * Base abstraction of a JAX-RS resource.
 */
public abstract class BaseResource {

    private static ServiceDirectory services = new DefaultServiceDirectory();

    /**
     * Sets alternate service directory to be used for lookups.
     * <p>
     * Intended to ease unit testing and not intended for use in production.
     * </p>
     *
     * @param serviceDirectory alternate service directory
     */
    public static void setServiceDirectory(ServiceDirectory serviceDirectory) {
        services = serviceDirectory;
    }

    /**
     * Returns reference to the specified service implementation.
     *
     * @param service service class
     * @param <T>     type of service
     * @return service implementation
     */
    protected static <T> T get(Class<T> service) {
        return services.get(service);
    }

}
