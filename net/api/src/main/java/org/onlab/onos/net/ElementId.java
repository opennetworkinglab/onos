package org.onlab.onos.net;

import java.net.URI;
import java.util.Objects;

import static com.google.common.base.Objects.toStringHelper;

/**
 * Immutable representation of a network element identity.
 */
public class ElementId {

    private final URI uri;

    /**
     * Creates an element identifier using the supplied URI.
     *
     * @param uri backing URI
     */
    public ElementId(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the backing URI.
     *
     * @return backing URI
     */
    public URI uri() {
        return uri;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ElementId other = (ElementId) obj;
        return Objects.equals(this.uri, other.uri);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("uri", uri).toString();
    }

}
