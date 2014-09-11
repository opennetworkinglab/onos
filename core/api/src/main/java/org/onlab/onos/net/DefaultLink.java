package org.onlab.onos.net;

import org.onlab.onos.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default infrastructure link model implementation.
 */
public class DefaultLink extends AbstractModel implements Link {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Type type;

    /**
     * Creates an infrastructure link using the supplied information.
     *
     * @param providerId provider identity
     * @param src        link source
     * @param dst        link destination
     * @param type       link type
     */
    public DefaultLink(ProviderId providerId, ConnectPoint src, ConnectPoint dst,
                       Type type) {
        super(providerId);
        this.src = src;
        this.dst = dst;
        this.type = type;
    }

    @Override
    public ConnectPoint src() {
        return src;
    }

    @Override
    public ConnectPoint dst() {
        return dst;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefaultLink) {
            final DefaultLink other = (DefaultLink) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .add("type", type)
                .toString();
    }

}
