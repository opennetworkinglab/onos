package org.onlab.onos.net;

import java.net.URI;
import java.util.Objects;

/**
 * Immutable representation of a network element identity.
 */
public abstract class ElementId {

    private final URI uri;
    private final String str;

    // Default constructor for serialization
    protected ElementId() {
        this.uri = null;
        this.str = null;
    }

    /**
     * Creates an element identifier using the supplied URI.
     *
     * @param uri backing URI
     */
    protected ElementId(URI uri) {
        this.uri = uri;
        this.str = uri.toString();
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
        if (obj instanceof ElementId) {
            final ElementId that = (ElementId) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.uri, that.uri);
        }
        return false;
    }

    @Override
    public String toString() {
        return str;
    }

}
