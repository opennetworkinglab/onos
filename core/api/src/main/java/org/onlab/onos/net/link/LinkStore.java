package org.onlab.onos.net.link;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.Store;

import java.util.Set;

/**
 * Manages inventory of infrastructure links; not intended for direct use.
 */
public interface LinkStore extends Store<LinkEvent, LinkStoreDelegate> {

    /**
     * Returns the number of links in the store.
     *
     * @return number of links
     */
    int getLinkCount();

    /**
     * Returns an iterable collection of all links in the inventory.
     *
     * @return collection of all links
     */
    Iterable<Link> getLinks();

    /**
     * Returns all links egressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceEgressLinks(DeviceId deviceId);

    /**
     * Returns all links ingressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceIngressLinks(DeviceId deviceId);

    /**
     * Returns the link between the two end-points.
     *
     * @param src source connection point
     * @param dst destination connection point
     * @return link or null if one not found between the end-points
     */
    Link getLink(ConnectPoint src, ConnectPoint dst);

    /**
     * Returns all links egressing from the specified connection point.
     *
     * @param src source connection point
     * @return set of connection point links
     */
    Set<Link> getEgressLinks(ConnectPoint src);

    /**
     * Returns all links ingressing to the specified connection point.
     *
     * @param dst destination connection point
     * @return set of connection point links
     */
    Set<Link> getIngressLinks(ConnectPoint dst);

    /**
     * Creates a new link, or updates an existing one, based on the given
     * information.
     *
     * @param providerId      provider identity
     * @param linkDescription link description
     * @return create or update link event, or null if no change resulted
     */
    public LinkEvent createOrUpdateLink(ProviderId providerId,
                                        LinkDescription linkDescription);

    /**
     * Removes the link based on the specified information.
     *
     * @param src link source
     * @param dst link destination
     * @return remove link event, or null if no change resulted
     */
    LinkEvent removeLink(ConnectPoint src, ConnectPoint dst);

}
