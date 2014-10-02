package org.onlab.onos.net;

import org.onlab.onos.net.provider.ProviderId;

/**
 * Base implementation of a network model entity.
 */
public abstract class AbstractModel extends AbstractAnnotated implements Provided {

    private final ProviderId providerId;

    // For serialization
    public AbstractModel() {
        providerId = null;
    }

    /**
     * Creates a model entity attributed to the specified provider and
     * optionally annotated.
     *
     * @param providerId  identity of the provider
     * @param annotations optional key/value annotations
     */
    protected AbstractModel(ProviderId providerId, Annotations... annotations) {
        super(annotations);
        this.providerId = providerId;
    }

    @Override
    public ProviderId providerId() {
        return providerId;
    }

}
