package org.onlab.onos.net.provider;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Notion of provider identity.
 */
public class ProviderId {

    private final String id;

    /**
     * Creates a new provider identifier from the specified string.
     * The providers are expected to follow the reverse DNS convention, e.g.
     * {@code org.onlab.onos.provider.of.device}
     *
     * @param id string identifier
     */
    public ProviderId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ProviderId other = (ProviderId) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).toString();
    }

}
