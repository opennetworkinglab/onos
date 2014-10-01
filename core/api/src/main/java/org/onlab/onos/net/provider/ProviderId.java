package org.onlab.onos.net.provider;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Notion of provider identity.
 */
public class ProviderId {

    private final String scheme;
    private final String id;
    private final boolean ancillary;

    /**
     * Creates a new provider identifier from the specified string.
     * The providers are expected to follow the reverse DNS convention, e.g.
     * {@code org.onlab.onos.provider.of.device}
     *
     * @param scheme device URI scheme to which this provider is bound, e.g. "of", "snmp"
     * @param id     string identifier
     */
    public ProviderId(String scheme, String id) {
        this(scheme, id, false);
    }

    /**
     * Creates a new provider identifier from the specified string.
     * The providers are expected to follow the reverse DNS convention, e.g.
     * {@code org.onlab.onos.provider.of.device}
     *
     * @param scheme device URI scheme to which this provider is bound, e.g. "of", "snmp"
     * @param id     string identifier
     * @param ancillary ancillary provider indicator
     */
    public ProviderId(String scheme, String id, boolean ancillary) {
        this.scheme = scheme;
        this.id = id;
        this.ancillary = ancillary;
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
     * Indicates whether the provider id belongs to an ancillary provider.
     *
     * @return true for ancillary; false for primary provider
     */
    public boolean isAncillary() {
        return ancillary;
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
                    Objects.equals(this.id, other.id) &&
                    this.ancillary == other.ancillary;
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("scheme", scheme).add("id", id)
                .add("ancillary", ancillary).toString();
    }

}
