package org.onlab.onos.net.provider;

/**
 * Base provider implementation.
 */
public abstract class AbstractProvider implements Provider {

    private final ProviderId providerId;

    /**
     * Creates a provider with the supplier identifier.
     *
     * @param id provider id
     */
    protected AbstractProvider(ProviderId id) {
        this.providerId = id;
    }

    @Override
    public ProviderId id() {
        return providerId;
    }

}
