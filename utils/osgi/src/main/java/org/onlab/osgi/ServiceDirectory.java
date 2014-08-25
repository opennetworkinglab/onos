package org.onlab.osgi;

/**
 * Simple abstraction of a service directory where service implementations can
 * be found by the class name of the interfaces they provide.
 */
public interface ServiceDirectory {

    /**
     * Returns implementation of the specified service class.
     * @param serviceClass service class
     * @param <T> type of service
     * @return implementation class
     * @throws ServiceNotFoundException if no implementation found
     */
    <T> T get(Class<T> serviceClass);

}
