package org.onlab.onos.net.provider;

/**
 * Broker used for registering/unregistering information providers with the core.
 *
 * @param <P> type of the information provider
 * @param <S> type of the provider service
 */
public interface ProviderBroker<P extends Provider, S extends ProviderService<P>> {

    /**
     * Registers the supplied provider with the core.
     *
     * @param provider provider to be registered
     * @return provider service for injecting information into core
     */
    S register(P provider);

    /**
     * Unregisters the supplied provider. As a result the previously issued
     * provider service will be invalidated and any subsequent invocations
     * of its methods may throw {@link java.lang.IllegalStateException}.
     *
     * @param provider provider to be unregistered
     */
    void unregister(P provider);

}
