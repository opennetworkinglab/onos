package org.onosproject.ipran;

import java.util.Collection;

import org.onosproject.net.flowext.FlowRuleExtEntry;


public interface IpranSessionListener {
    /**
     * Receives a route update from a route provider.
     *
     * @param routeUpdates the collection with updated route information
     */
    public void update(Collection<FlowRuleExtEntry> routeUpdates);
}
