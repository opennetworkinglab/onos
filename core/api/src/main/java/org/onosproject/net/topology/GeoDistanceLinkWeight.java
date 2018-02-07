/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.topology;

import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onlab.util.GeoLocation;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

import static java.lang.Double.MAX_VALUE;

/**
 * Link weight for measuring link cost using the geo distance between link
 * vertices as determined by the element longitude/latitude annotation.
 */
public class GeoDistanceLinkWeight implements LinkWeigher {

    private static final double MAX_KM = 40_075 / 2.0;

    private final DeviceService deviceService;

    /**
     * Creates a new link-weight with access to the specified device service.
     *
     * @param deviceService device service reference
     */
    public GeoDistanceLinkWeight(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public Weight getInitialWeight() {
        return ScalarWeight.toWeight(0.0);
    }

    @Override
    public Weight getNonViableWeight() {
        return ScalarWeight.NON_VIABLE_WEIGHT;
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        GeoLocation src = getLocation(edge.link().src().deviceId());
        GeoLocation dst = getLocation(edge.link().dst().deviceId());
        return ScalarWeight.toWeight(src != null && dst != null ? src.kilometersTo(dst) : MAX_KM);
    }

    private GeoLocation getLocation(DeviceId deviceId) {
        Device d = deviceService.getDevice(deviceId);
        Annotations a = d != null ? d.annotations() : null;
        double latitude = getDouble(a, AnnotationKeys.LATITUDE);
        double longitude = getDouble(a, AnnotationKeys.LONGITUDE);
        return latitude == MAX_VALUE || longitude == MAX_VALUE ? null :
                new GeoLocation(latitude, longitude);
    }

    private double getDouble(Annotations a, String key) {
        String value = a != null ? a.value(key) : null;
        try {
            return value != null ? Double.parseDouble(value) : MAX_VALUE;
        } catch (NumberFormatException e) {
            return MAX_VALUE;
        }
    }
}

