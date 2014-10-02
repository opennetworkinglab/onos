package org.onlab.onos.net;

import org.onlab.onos.net.provider.ProviderId;

import java.util.Map;

/**
 * Base implementation of a network model entity.
 */
public class AbstractModel extends AbstractAnnotated implements Provided {

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
    @SafeVarargs
    protected AbstractModel(ProviderId providerId,
                            Map<String, String>... annotations) {
        super(annotations);
        this.providerId = providerId;
    }

    @Override
    public ProviderId providerId() {
        return providerId;
    }

}
