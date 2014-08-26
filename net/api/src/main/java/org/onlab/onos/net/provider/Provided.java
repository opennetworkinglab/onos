package org.onlab.onos.net.provider;

/**
 * Abstraction of an entity supplied by a provider.
 */
public interface Provided {

    /**
     * Returns the identifier of the provider which supplied the entity.
     *
     * @return provider identification
     */
    ProviderId providerId();

}
