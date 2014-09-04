package org.onlab.onos.net;

import org.onlab.onos.net.provider.ProviderId;

/**
 * Base implementation of a network model entity.
 */
public class AbstractModel implements Provided {

    private final ProviderId providerId;

    /**
     * Creates a model entity attributed to the specified provider.
     *
     * @param providerId identity of the provider
     */
    protected AbstractModel(ProviderId providerId) {
        this.providerId = providerId;
    }

    @Override
    public ProviderId providerId() {
        return providerId;
    }

}
