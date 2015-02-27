package org.onosproject.net;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.net.Link.State.ACTIVE;

import java.util.Objects;

import org.onosproject.net.provider.ProviderId;

/**
 * the default implementation of a network tunnel.
 */
public final class DefaultTunnel extends AbstractModel implements Tunnel {
    private ConnectPoint src;
    private ConnectPoint dst;
    private Type type;
    private State state;
    private boolean isDurable;

    public DefaultTunnel(ProviderId providerId, ConnectPoint src,
                         ConnectPoint dst, Type type,
                         Annotations... annotations) {
        this(providerId, src, dst, type, ACTIVE, false, annotations);
    }

    public DefaultTunnel(ProviderId providerId, ConnectPoint src,
                         ConnectPoint dst, Type type, State state,
                         boolean isDurable, Annotations... annotations) {
        super(providerId, annotations);
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.state = state;
        this.isDurable = isDurable;
    }

    @Override
    public ConnectPoint src() {
        // TODO Auto-generated method stub
        return src;
    }

    @Override
    public ConnectPoint dst() {
        // TODO Auto-generated method stub
        return dst;
    }

    @Override
    public org.onosproject.net.Link.Type type() {
        // TODO Auto-generated method stub
        return Link.Type.TUNNEL;
    }

    @Override
    public State state() {
        // TODO Auto-generated method stub
        return state;
    }

    @Override
    public boolean isDurable() {
        // TODO Auto-generated method stub
        return isDurable;
    }

    @Override
    public Type tunnelType() {
        // TODO Auto-generated method stub
        return type;
    }

    /**
     * @deprecated
     */
    @Override
    public NetworkResource resource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTunnel) {
            final DefaultTunnel other = (DefaultTunnel) obj;
            return Objects.equals(this.src, other.src)
                    && Objects.equals(this.dst, other.dst)
                    && Objects.equals(this.type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("src", src).add("dst", dst)
                .add("type", type).add("state", state)
                .add("durable", isDurable).toString();
    }
}
