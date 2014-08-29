package org.onlab.onos.net.provider;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base implementation of provider registry.
 *
 * @param <P> type of the information provider
 * @param <S> type of the provider service
 */
public abstract class AbstractProviderRegistry<P extends Provider, S extends ProviderService<P>>
        implements ProviderRegistry<P, S> {

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
        checkState(!services.containsKey(provider.id()), "Provider %s already registered", provider.id());
        S service = createProviderService(provider);
        services.put(provider.id(), service);
        return service;
    }

    @Override
    public synchronized void unregister(P provider) {
        checkNotNull(provider, "Provider cannot be null");
        S service = services.get(provider.id());
        if (service != null && service instanceof AbstractProviderService) {
            ((AbstractProviderService) service).invalidate();
            services.remove(provider.id());
        }
    }

    @Override
    public synchronized Set<ProviderId> getProviders() {
        return ImmutableSet.copyOf(services.keySet());
    }

}
