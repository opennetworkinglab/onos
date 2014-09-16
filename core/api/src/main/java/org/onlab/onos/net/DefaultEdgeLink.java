package org.onlab.onos.net;

import org.onlab.onos.net.provider.ProviderId;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default edge link model implementation.
 */
public class DefaultEdgeLink extends DefaultLink implements EdgeLink {

    private final HostId hostId;
    private final HostLocation hostLocation;

    /**
     * Creates an edge link using the supplied information.
     *
     * @param providerId   provider identity
     * @param hostPoint    host-side connection point
     * @param hostLocation location where host attaches to the network
     * @param isIngress    true to indicated host-to-network direction; false
     *                     for network-to-host direction
     */
    public DefaultEdgeLink(ProviderId providerId, ConnectPoint hostPoint,
                           HostLocation hostLocation, boolean isIngress) {
        super(providerId, isIngress ? hostPoint : hostLocation,
              isIngress ? hostLocation : hostPoint, Type.EDGE);
        checkArgument(hostPoint.elementId() instanceof HostId,
                      "Host point does not refer to a host ID");
        this.hostId = (HostId) hostPoint.elementId();
        this.hostLocation = hostLocation;
    }

    @Override
    public HostId hostId() {
        return hostId;
    }

    @Override
    public HostLocation hostLocation() {
        return hostLocation;
    }
}
