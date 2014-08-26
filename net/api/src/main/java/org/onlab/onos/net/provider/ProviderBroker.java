package org.onlab.onos.net.provider;

/**
 * Broker used for registering/unregistering information providers with the core.
 *
 * @param <T> type of the information provider
 * @param <S> type of the provider service
 */
public interface ProviderBroker<T extends Provider, S extends ProviderService> {

    /**
     * Registers the supplied provider with the core.
     *
     * @param provider provider to be registered
     * @return provider service for injecting information into core
     */
    S register(T provider);

    /**
     * Unregisters the supplied provider. As a result the previously issued
     * provider service will be invalidated.
     *
     * @param provider provider to be unregistered
     */
    void unregister(T provider);

}
