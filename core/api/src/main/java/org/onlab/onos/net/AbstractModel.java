package org.onlab.onos.net;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.onos.net.provider.ProviderId;

import java.util.Map;
import java.util.Set;

/**
 * Base implementation of a network model entity.
 */
public class AbstractModel implements Provided, Annotated {

    private final ProviderId providerId;

    // FIXME: figure out whether to make this concurrent or immutable
    private final Map<String, String> annotations = Maps.newHashMap();

    // For serialization
    public AbstractModel() {
        providerId = null;
    }

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

    @Override
    public Set<String> annotationKeys() {
        return ImmutableSet.copyOf(annotations.keySet());
    }

    @Override
    public String annotation(String key) {
        return annotations.get(key);
    }
}
