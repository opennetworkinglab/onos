/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net;

/**
 * Collection of keys for annotation.
 * <p>
 * Number of the annotation keys have been deprecated as the use of annotations
 * is being phased out and instead network configuration subsystem is being
 * phased-in for majority of model meta-data.
 */
public final class AnnotationKeys {

    private static final double DEFAULT_VALUE = 1.0;

    // Prohibit instantiation
    private AnnotationKeys() {
    }

    /**
     * Annotation key for instance name.
     */
    public static final String NAME = "name";

    /**
     * Annotation key for instance type (e.g. host type).
     *
     * @deprecated since Cardinal
     */
    @Deprecated
    public static final String TYPE = "type";

    /**
     * Annotation key for UI type (the glyph ID for rendering).
     */
    public static final String UI_TYPE = "uiType";

    /**
     * Annotation key for UI connection type (the style for host link rendering).
     */
    public static final String CONNECTION_TYPE = "connectionType";

    /**
     * Annotation key for UI location type of device/host
     * (either 'geo' or 'grid').
     */
    public static final String LOC_TYPE = "locType";

    /**
     * Annotation key for latitude (e.g. latitude of device/host
     * in a geo-layout).
     */
    public static final String LATITUDE = "latitude";

    /**
     * Annotation key for longitude (e.g. longitude of device/host
     * in a geo-layout).
     */
    public static final String LONGITUDE = "longitude";

    /**
     * Annotation key for grid-Y (e.g. y-coordinate of device/host
     * in a grid-layout).
     */
    public static final String GRID_Y = "gridY";

    /**
     * Annotation key for grid-X (e.g. x-coordinate of device/host
     * in a grid-layout).
     */
    public static final String GRID_X = "gridX";

    /**
     * Annotation key for southbound protocol.
     */
    public static final String PROTOCOL = "protocol";

    /**
     * Annotation key for the device driver name.
     */
    public static final String DRIVER = "driver";

    /**
     * Annotation key for durable links.
     */
    public static final String DURABLE = "durable";

    /**
     * Annotation key for link metric; used by
     * {@link org.onosproject.net.topology.MetricLinkWeight} function.
     */
    public static final String METRIC = "metric";

    /**
     * Annotation key for latency.
     * The value of this key is expected to be latency in nanosecond.
     */
    public static final String LATENCY = "latency";

    /**
     * Annotation key for bandwidth.
     * The value for this key is interpreted as Mbps.
     */
    public static final String BANDWIDTH = "bandwidth";

    /**
     * Annotation key for the number of optical waves.
     */
    public static final String OPTICAL_WAVES = "optical.waves";

    /**
     * Annotation key for the port name.
     */
    public static final String PORT_NAME = "portName";

    /**
     * Annotation key for the optical channel receiving/in port (RX).
     */
    public static final String PORT_IN = "portIn";

    /**
     * Annotation key for the optical channel port transmitting/out port (TX).
     */

    public static final String PORT_OUT = "portOut";
    /**
     * Annotation key for the port mac.
     */
    public static final String PORT_MAC = "portMac";

    /**
     * Annotation key for the admin state.
     * The value of this key is expected to be "enabled" or "disabled"
     *
     */
    public static final String ADMIN_STATE = "adminState";

    /**
     * Annotation key for the router ID.
     */
    public static final String ROUTER_ID = "routerId";

    /**
     * Annotation key for the static lambda.
     */
    public static final String STATIC_LAMBDA = "staticLambda";

    /**
     * Annotation key for the static port.
     */
    public static final String STATIC_PORT = "staticPort";

    /**
     * Annotation key for device location.
     */
    public static final String RACK_ADDRESS = "rackAddress";

    /**
     * Annotation key for device owner.
     */
    public static final String OWNER = "owner";

    /**
     * Annotation key for the channel id.
     */
    public static final String CHANNEL_ID = "channelId";

    /**
     * Annotation key for the management address.
     */
    public static final String MANAGEMENT_ADDRESS = "managementAddress";

    /**
     * Annotation key for the username.
     */
    public static final String USERNAME = "username";

    /**
     * Annotation key for the password.
     */
    public static final String PASSWORD = "password";

    /**
     * Link annotation key to express that a Link
     * is backed by underlying protection mechanism.
     */
    // value is undefined at the moment, only using key existence
    public static final String PROTECTED = "protected";

    /**
     * Annotation key for REST server identifier.
     */
    public static final String REST_SERVER = "restServer";

    /**
     * Annotation key for the sshkey.
     */
    public static final String SSHKEY = "sshkey";

    /**
     * Annotation key for the protocol layer.
     */
    public static final String LAYER = "layer";

    /**
     * Annotation key for jitter.
     * The value of this key is expected to be jitter in seconds
     */
    public static final String JITTER = "jitter";

    /**
     * Annotation key for delay.
     * The value of this key is expected to be delay in seconds
     */
    public static final String DELAY = "delay";

    /**
     * Annotation key for loss.
     * The value of this key is expected to be loss in percentage.
     */
    public static final String LOSS = "loss";

    /**
     * Annotation key for availability.
     * The value of this key is expected to be availability as a percentage
     */
    public static final String AVAILABILITY = "availability";

    /**
     * Annotation key for flapping.
     * The value of this key is expected to be a subjective percentage for flapping
     */
    public static final String FLAPPING = "flapping";

    /**
     * Annotation key for identifying a metered link.
     * The value of this key is expected to be a boolean for metered as true/false.
     */
    public static final String METERED = "metered";

    /**
     * Annotation key for data usage on a metered link.
     * The value of this key is expected to be a percentage of the data available within the plan.
     */
    public static final String METERED_USAGE = "meteredUsage";

    /**
     * Annotation key for identifying the tier ranking of a link. Links with a lower tier would be
     * selected in the path over links with a higher tier.
     * The value of this key is expected to be a number that represents the tier value.
     */
    public static final String TIER = "tier";

    /**
     * Annotation key for the datapath description.
     * Provides a human readable description of a given datapath. Used, for instance, when an Openflow
     * switch connects to the controller, in the response to the OFPMP_DESC request
     */
    public static final String DATAPATH_DESCRIPTION = "datapathDescription";

    /**
     * Returns the value annotated object for the specified annotation key.
     * The annotated value is expected to be String that can be parsed as double.
     * If parsing fails, the returned value will be {@value #DEFAULT_VALUE}.
     *
     * @param annotated annotated object whose annotated value is obtained
     * @param key       key of annotation
     * @return double value of annotated object for the specified key
     */
    public static double getAnnotatedValue(Annotated annotated, String key) {
        double value;
        try {
            value = Double.parseDouble(annotated.annotations().value(key));
        } catch (NumberFormatException e) {
            value = DEFAULT_VALUE;
        }
        return value;
    }
}
