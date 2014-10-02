package org.onlab.onos.net;

import com.google.common.collect.ImmutableList;
import org.onlab.onos.net.provider.ProviderId;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a network path.
 */
public class DefaultPath extends DefaultLink implements Path {

    private final List<Link> links;
    private final double cost;

    /**
     * Creates a path from the specified source and destination using the
     * supplied list of links.
     *
     * @param providerId provider identity
     * @param links      contiguous links that comprise the path
     * @param cost       unit-less path cost
     * @param annotations optional key/value annotations
     */
    public DefaultPath(ProviderId providerId, List<Link> links, double cost,
                       Annotations... annotations) {
        super(providerId, source(links), destination(links), Type.INDIRECT, annotations);
        this.links = ImmutableList.copyOf(links);
        this.cost = cost;
    }

    @Override
    public List<Link> links() {
        return links;
    }

    @Override
    public double cost() {
        return cost;
    }

    // Returns the source of the first link.
    private static ConnectPoint source(List<Link> links) {
        checkNotNull(links, "List of path links cannot be null");
        checkArgument(!links.isEmpty(), "List of path links cannot be empty");
        return links.get(0).src();
    }

    // Returns the destination of the last link.
    private static ConnectPoint destination(List<Link> links) {
        checkNotNull(links, "List of path links cannot be null");
        checkArgument(!links.isEmpty(), "List of path links cannot be empty");
        return links.get(links.size() - 1).dst();
    }

    @Override
    public int hashCode() {
        return Objects.hash(links);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPath) {
            final DefaultPath other = (DefaultPath) obj;
            return Objects.equals(this.links, other.links);
        }
        return false;
    }
}
