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
     * @param isIngress    true to indicate host-to-network direction; false
     *                     for network-to-host direction
     * @param annotations  optional key/value annotations
     */
    public DefaultEdgeLink(ProviderId providerId, ConnectPoint hostPoint,
                           HostLocation hostLocation, boolean isIngress,
                           Annotations... annotations) {
        super(providerId, isIngress ? hostPoint : hostLocation,
              isIngress ? hostLocation : hostPoint, Type.EDGE, annotations);
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

    /**
     * Creates a phantom edge link, to an unspecified end-station. This link
     * does not represent any actually discovered link stored in the system.
     *
     * @param edgePort  network edge port
     * @param isIngress true to indicate host-to-network direction; false
     *                  for network-to-host direction
     * @return new phantom edge link
     */
    public static DefaultEdgeLink createEdgeLink(HostLocation edgePort,
                                                 boolean isIngress) {
        return new DefaultEdgeLink(ProviderId.NONE,
                                   new ConnectPoint(HostId.NONE, PortNumber.P0),
                                   edgePort, isIngress);
    }
}
