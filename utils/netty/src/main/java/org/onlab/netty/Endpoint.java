package org.onlab.netty;

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
        return "Endpoint [port=" + port + ", host=" + host + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        return result;
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
        Endpoint other = (Endpoint) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        return true;
    }
}