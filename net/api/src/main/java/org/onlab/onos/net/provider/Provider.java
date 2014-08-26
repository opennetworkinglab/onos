package org.onlab.onos.net.provider;

/**
 * Abstraction of a provider of information about network environment.
 */
public interface Provider {

    /**
     * Returns the provider identifier.
     *
     * @return provider identification
     */
    ProviderId id();

}
