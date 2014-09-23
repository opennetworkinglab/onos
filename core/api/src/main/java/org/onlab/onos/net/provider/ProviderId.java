package org.onlab.onos.net.provider;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Notion of provider identity.
 */
public class ProviderId {

    private final String scheme;
    private final String id;

    /**
     * Creates a new provider identifier from the specified string.
     * The providers are expected to follow the reverse DNS convention, e.g.
     * {@code org.onlab.onos.provider.of.device}
     *
     * @param scheme device URI scheme to which this provider is bound, e.g. "of", "snmp"
     * @param id     string identifier
     */
    public ProviderId(String scheme, String id) {
        this.scheme = scheme;
        this.id = id;
    }

    /**
     * Returns the device URI scheme to which this provider is bound.
     *
     * @return device URI scheme
     */
    public String scheme() {
        return scheme;
    }

    /**
     * Returns the device URI scheme specific id portion.
     *
     * @return id
     */
    public String id() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProviderId) {
            final ProviderId other = (ProviderId) obj;
            return Objects.equals(this.scheme, other.scheme) &&
                    Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("scheme", scheme).add("id", id).toString();
    }

}
