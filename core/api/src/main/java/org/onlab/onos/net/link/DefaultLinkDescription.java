package org.onlab.onos.net.link;

import com.google.common.base.MoreObjects;
import org.onlab.onos.net.AbstractDescription;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.SparseAnnotations;

/**
 * Default implementation of immutable link description entity.
 */
public class DefaultLinkDescription extends AbstractDescription
        implements LinkDescription {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Link.Type type;

    /**
     * Creates a link description using the supplied information.
     *
     * @param src  link source
     * @param dst  link destination
     * @param type link type
     * @param annotations optional key/value annotations
     */
    public DefaultLinkDescription(ConnectPoint src, ConnectPoint dst,
                                  Link.Type type, SparseAnnotations... annotations) {
        super(annotations);
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
    public Link.Type type() {
        return type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("Link").add("src", src())
                                .add("dst", dst())
                                .add("type", type()).toString();
    }

}
