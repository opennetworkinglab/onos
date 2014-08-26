package org.onlab.onos.net.provider;

/**
 * Abstraction of a service through which providers can inject information
 * about the network environment into the core.
 *
 * @param <P> type of the information provider
 */
public interface ProviderService<P extends Provider> {

    /**
     * Returns the provider to which this service has been issued.
     *
     * @return provider to which this service has been assigned
     */
    P provider();

}
