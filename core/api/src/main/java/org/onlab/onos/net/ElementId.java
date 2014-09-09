package org.onlab.onos.net;

import java.net.URI;
import java.util.Objects;

/**
 * Immutable representation of a network element identity.
 */
public abstract class ElementId {

    private final URI uri;

    /**
     * Creates an element identifier using the supplied URI.
     *
     * @param uri backing URI
     */
    protected ElementId(URI uri) {
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
        if (obj instanceof ElementId) {
            final ElementId that = (ElementId) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.uri, that.uri);
        }
        return false;
    }

    @Override
    public String toString() {
        return uri.toString();
    }

}
