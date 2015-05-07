package org.onosproject.net.tunnel;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;

import org.onosproject.core.IdGenerator;
import org.onosproject.net.AbstractModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.BandwidthResource;

/**
 * Default tunnel model implementation.
 */
public final class DefaultTunnel extends AbstractModel implements Tunnel {
    private final TunnelId  id;
    private final Label src;
    private final Label dst;
    private final Type type;
    private final State state;
    private final boolean isDurable;
    private final boolean isBidirectional;
    private final BandwidthResource bandwidth;

    /**
     * Constructs an tunnel using the builder pattern.
     *
     * @param providerId  provider identity, can be null if comes from the NB
     * @param builder     tunnelBuilder
     * @param annotations optional key/value annotations
     * @return
     */
    private DefaultTunnel(ProviderId providerId, TunnelBuilder builder, Annotations... annotations) {
        super(providerId, annotations);
        this.id = builder.id;
        this.src = builder.src;
        this.dst = builder.dst;
        this.type = builder.type;
        this.state = builder.state;
        this.isDurable = builder.isDurable;
        this.isBidirectional = builder.isBidirectional;
        this.bandwidth = builder.bandwidth;
    }

    @Override
    public TunnelId id() {
        return id;
    }

    @Override
    public Label src() {
        return src;
    }

    @Override
    public Label dst() {
        return dst;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public boolean isDurable() {
        return isDurable;
    }

    @Override
    public boolean isBidirectional() {
        return isBidirectional;
    }

    @Override
    public BandwidthResource bandwidth() {
        return bandwidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * {@inheritDoc}
     * Note that only TunnelId is considered on equality check.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTunnel) {
            final DefaultTunnel other = (DefaultTunnel) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("tunnelId", id)
                .add("src", src)
                .add("dst", dst)
                .add("type", type)
                .add("state", state)
                .add("durable", isDurable)
                .add("isBidirectional", isBidirectional)
                .add("bandwidth", bandwidth)
                .toString();
    }

    public static class TunnelBuilder {
        private TunnelId id = null;
        private Label src = null;
        private Label dst = null;
        private Type type = null;
        private State state = null;
        private boolean isDurable = false;
        private boolean isBidirectional = false;
        private BandwidthResource bandwidth = null;

        private static IdGenerator idGenerator;

        public TunnelBuilder labelSrcDst(Label src, Label dst) {
            this.src = src;
            this.dst = dst;
            return this;
        }

        public TunnelBuilder state(State state) {
            this.state = state;
            return this;
        }

        public TunnelBuilder isDurable(boolean isDurable) {
            this.isDurable = isDurable;
            return this;
        }

        public TunnelBuilder isBidirectional(boolean isBidirectional) {
            this.isBidirectional = isBidirectional;
            return this;
        }

        public TunnelBuilder bandwidth(BandwidthResource bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        public DefaultTunnel build(ProviderId providerId, Annotations... annotations) {
            checkState(idGenerator != null, "Id generator is not bound.");
            this.id = TunnelId.valueOf(idGenerator.getNewId());
            return new DefaultTunnel(providerId, this, annotations);
        }

    }

}
