package org.onlab.onos.net;

import java.util.List;

/**
 * Representation of a contiguous directed path in a network. Path comprises
 * of a sequence of links, where adjacent links must share the same device,
 * meaning that destination of the source of one link must coincide with the
 * destination of the previous link.
 */
public interface Path extends Link {

    /**
     * Returns sequence of links comprising the path.
     *
     * @return list of links
     */
    List<Link> links();

}
