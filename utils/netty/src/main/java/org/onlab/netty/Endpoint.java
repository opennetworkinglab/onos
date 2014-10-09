package org.onlab.netty;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Representation of a TCP/UDP communication end point.
 */
public class Endpoint {

    private final int port;
    private final String host;

    /**
     * Used for serialization.
     */
    @SuppressWarnings("unused")
    private Endpoint() {
        port = 0;
        host = null;
    }

    public Endpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("port", port)
                .add("host", host)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Endpoint that = (Endpoint) obj;
        return Objects.equals(this.port, that.port) &&
               Objects.equals(this.host, that.host);
    }
}
