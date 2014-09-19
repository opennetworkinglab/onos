package org.onlab.onos.net;

import org.onlab.onos.net.provider.ProviderId;

/**
 * Base implementation of network elements, i.e. devices or hosts.
 */
public class AbstractElement extends AbstractModel implements Element {

    protected final ElementId id;

    // For serialization
    public AbstractElement() {
        id = null;
    }

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param providerId identity of the provider
     * @param id         element identifier
     */
    protected AbstractElement(ProviderId providerId, ElementId id) {
        super(providerId);
        this.id = id;
    }

    @Override
    public ElementId id() {
        return id;
    }

}
