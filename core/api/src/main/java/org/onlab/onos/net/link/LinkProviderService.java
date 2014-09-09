package org.onlab.onos.net.link;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.provider.ProviderService;

/**
 * Means for injecting link information into the core.
 */
public interface LinkProviderService extends ProviderService<LinkProvider> {

    /**
     * Signals that an infrastructure link has been detected.
     *
     * @param linkDescription link information
     */
    void linkDetected(LinkDescription linkDescription);

    /**
     * Signals that an infrastructure link has disappeared.
     *
     * @param linkDescription link information
     */
    void linkVanished(LinkDescription linkDescription);

    /**
     * Signals that infrastructure links associated with the specified
     * connect point have vanished.
     *
     * @param connectPoint connect point
     */
    void linksVanished(ConnectPoint connectPoint);

    /**
     * Signals that infrastructure links associated with the specified
     * device have vanished.
     *
     * @param deviceId device identifier
     */
    void linksVanished(DeviceId deviceId);

}
