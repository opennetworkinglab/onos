package org.onosproject.store.cluster.impl;

import java.util.Set;

import org.onosproject.cluster.DefaultControllerNode;

import com.google.common.collect.ImmutableSet;

/**
 * Cluster definition.
 */
public class ClusterDefinition {

    private Set<DefaultControllerNode> nodes;
    private String ipPrefix;

    /**
     * Creates a new cluster definition.
     * @param nodes cluster nodes.
     * @param ipPrefix ip prefix common to all cluster nodes.
     * @return cluster definition
     */
    public static ClusterDefinition from(Set<DefaultControllerNode> nodes, String ipPrefix) {
        ClusterDefinition definition = new ClusterDefinition();
        definition.ipPrefix = ipPrefix;
        definition.nodes = ImmutableSet.copyOf(nodes);
        return definition;
    }

    /**
     * Returns set of cluster nodes.
     * @return cluster nodes.
     */
    public Set<DefaultControllerNode> nodes() {
        return ImmutableSet.copyOf(nodes);
    }

    /**
     * Returns ipPrefix in dotted decimal notion.
     * @return ip prefix.
     */
    public String ipPrefix() {
        return ipPrefix;
    }
}