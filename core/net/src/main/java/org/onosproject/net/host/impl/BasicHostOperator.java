/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.host.impl;

import org.onlab.packet.IpAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.HostLocation;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigOperator;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;

import java.util.Set;

/**
 * Implementations of merge policies for various sources of host configuration
 * information. This includes applications, providers, and network configurations.
 */
public final class BasicHostOperator implements ConfigOperator {

    private BasicHostOperator() {
    }

    /**
     * Generates a HostDescription containing fields from a HostDescription and
     * a HostConfig.
     *
     * @param cfg   the host config entity from network config
     * @param descr a HostDescription
     * @return HostDescription based on both sources
     */
    public static HostDescription combine(BasicHostConfig cfg, HostDescription descr) {
        if (cfg == null) {
            return descr;
        }

        HostLocation location = descr.location();
        ConnectPoint cfgLocation = cfg.location();
        if (cfgLocation != null) {
            location = new HostLocation(cfgLocation, System.currentTimeMillis());
        }

        Set<IpAddress> ipAddresses = descr.ipAddress();
        Set<IpAddress> cfgIpAddresses = cfg.ipAddresses();
        if (cfgIpAddresses != null) {
            ipAddresses = cfgIpAddresses;
        }

        SparseAnnotations sa = combine(cfg, descr.annotations());
        return new DefaultHostDescription(descr.hwAddress(), descr.vlan(),
                                          location, ipAddresses, sa);
    }

    /**
     * Generates an annotation from an existing annotation and HostConfig.
     *
     * @param cfg the device config entity from network config
     * @param an  the annotation
     * @return annotation combining both sources
     */
    public static SparseAnnotations combine(BasicHostConfig cfg, SparseAnnotations an) {
        DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();
        if (cfg.name() != null) {
            newBuilder.set(AnnotationKeys.NAME, cfg.name());
        }
        if (cfg.geoCoordsSet()) {
            newBuilder.set(AnnotationKeys.LATITUDE, Double.toString(cfg.latitude()));
            newBuilder.set(AnnotationKeys.LONGITUDE, Double.toString(cfg.longitude()));
        }
        if (cfg.rackAddress() != null) {
            newBuilder.set(AnnotationKeys.RACK_ADDRESS, cfg.rackAddress());
        }
        if (cfg.owner() != null) {
            newBuilder.set(AnnotationKeys.OWNER, cfg.owner());
        }
        return DefaultAnnotations.union(an, newBuilder.build());
    }
}
