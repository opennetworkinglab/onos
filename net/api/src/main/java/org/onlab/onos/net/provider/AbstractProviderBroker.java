package org.onlab.onos.net.provider;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of provider broker.
 *
 * @param <P> type of the information provider
 * @param <S> type of the provider service
 */
public abstract class AbstractProviderBroker<P extends Provider, S extends ProviderService>
        implements ProviderBroker<P, S> {

    private final Map<ProviderId, S> services = new HashMap<>();

    /**
     * Creates a new provider service bound to the specified provider.
     *
     * @param provider provider
     * @return provider service
     */
    protected abstract S createProviderService(P provider);

    @Override
    public synchronized S register(P provider) {
        checkNotNull(provider, "Provider cannot be null");
        checkArgument(!services.containsKey(provider), "Provider %s already registered", provider.id());
        S service = createProviderService(provider);
        services.put(provider.id(), service);
        return service;
    }

    @Override
    public synchronized void unregister(P provider) {
        checkNotNull(provider, "Provider cannot be null");
        S service = services.get(provider);
        checkArgument(service != null, "Provider %s not registered", provider.id());
        if (service instanceof AbstractProviderService) {
            ((AbstractProviderService) service).invalidate();
        }
        services.remove(provider);
    }
}
