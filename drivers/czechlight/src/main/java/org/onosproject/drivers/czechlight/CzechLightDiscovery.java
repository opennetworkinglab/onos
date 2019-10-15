/*
 * Copyright 2019-2020 Jan Kundrát, CESNET, <jan.kundrat@cesnet.cz> and Open Networking Foundation
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

package org.onosproject.drivers.czechlight;

import com.google.common.collect.Lists;

import org.apache.commons.configuration.HierarchicalConfiguration;

import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Device description behaviour for CzechLight SDN ROADM devices using NETCONF.
 */
public class CzechLightDiscovery
        extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

    public enum DeviceType {
        LINE_DEGREE,
        ADD_DROP_FLEX,
        COHERENT_ADD_DROP,
        INLINE_AMP,
    };
    public static final String DEVICE_TYPE_ANNOTATION = "czechlight.model";

    public static final int PORT_COMMON = 100;
    public static final int PORT_INLINE_WEST = PORT_COMMON + 1;
    public static final int PORT_INLINE_EAST = PORT_INLINE_WEST + 1;
    public static final ChannelSpacing CHANNEL_SPACING_50 = ChannelSpacing.CHL_50GHZ;
    public static final ChannelSpacing CHANNEL_SPACING_NONE = ChannelSpacing.CHL_0GHZ;
    public static final Frequency START_CENTER_FREQ_50 = Frequency.ofGHz(191_350);
    public static final Frequency END_CENTER_FREQ_50 = Frequency.ofGHz(196_100);

    private static final String MOD_ROADM_DEVICE = "czechlight-roadm-device";
    private static final String MOD_ROADM_DEVICE_DATE = "2019-09-30";
    private static final String MOD_ROADM_FEATURE_LINE_DEGREE = "hw-line-9";
    private static final String MOD_ROADM_FEATURE_FLEX_ADD_DROP = "hw-add-drop-20";
    private static final String MOD_COHERENT_A_D = "czechlight-coherent-add-drop";
    private static final String MOD_COHERENT_A_D_DATE = "2019-09-30";
    private static final String MOD_INLINE_AMP = "czechlight-inline-amp";
    private static final String MOD_INLINE_AMP_DATE = "2019-09-30";
    private static final String NS_CZECHLIGHT_PREFIX = "http://czechlight.cesnet.cz/yang/";
    public static final String NS_CZECHLIGHT_ROADM_DEVICE = NS_CZECHLIGHT_PREFIX + MOD_ROADM_DEVICE;
    public static final String NS_CZECHLIGHT_COHERENT_A_D = NS_CZECHLIGHT_PREFIX + MOD_COHERENT_A_D;
    public static final String NS_CZECHLIGHT_INLINE_AMP  = NS_CZECHLIGHT_PREFIX + MOD_INLINE_AMP;

    private static final String YANGLIB_KEY_REVISION = "data.modules-state.module.revision";
    private static final String YANGLIB_KEY_MODULE_NAME = "data.modules-state.module.name";
    private static final String YANGLIB_XMLNS = "urn:ietf:params:xml:ns:yang:ietf-yang-library";
    private static final String YANGLIB_XML_PREFIX = "yanglib";
    private static final String YANGLIB_XPATH_FILTER = "/" + YANGLIB_XML_PREFIX + ":modules-state/module[(name='"
            + MOD_ROADM_DEVICE + "') or (name='" + MOD_COHERENT_A_D + "') or (name='" + MOD_INLINE_AMP + "')]";
    private static final String YANGLIB_PATH_QUERY_FEATURES = "data.modules-state.module.feature";

    public static final String CHANNEL_DEFS_FILTER =
            "<channel-plan xmlns=\"http://czechlight.cesnet.cz/yang/czechlight-roadm-device\">" +
                    "<channel><lower-frequency/><upper-frequency/></channel>" +
                    "</channel-plan>";
    private static final String UNIDIR_CFG_SUBSTR = "<port/><attenuation/><power/>";
    public static final String XML_MC_OPEN = "<media-channels " +
            "xmlns=\"http://czechlight.cesnet.cz/yang/czechlight-roadm-device\">";
    public static final String XML_MC_CLOSE = "</media-channels>";
    public static final String MC_ROUTING_FILTER =
            XML_MC_OPEN +
                    "<add>" + UNIDIR_CFG_SUBSTR + "</add>" +
                    "<drop>" + UNIDIR_CFG_SUBSTR + "</drop>" +
                    XML_MC_CLOSE;

    public static final String LINE_EXPRESS_PREFIX = "E";

    private static final String DESC_PORT_LINE_WEST = "Line West";
    private static final String DESC_PORT_LINE_EAST = "Line East";
    private static final String DESC_PORT_LINE = "Line";
    private static final String DESC_PORT_EXPRESS = "Express";


    private static final Logger log = getLogger(CzechLightDiscovery.class);

    @Override
    public DeviceDescription discoverDeviceDetails() {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Cannot request NETCONF session for {}", data().deviceId());
            return null;
        }

        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
        final var noDevice = new DefaultDeviceDescription(handler().data().deviceId().uri(), Device.Type.OTHER,
                null, null, null, null, null, annotations.build());

        try {
            Boolean isLineDegree = false, isAddDrop = false, isCoherentAddDrop = false, isInlineAmp = false;
            var data = doGetXPath(getNetconfSession(), YANGLIB_XML_PREFIX, YANGLIB_XMLNS, YANGLIB_XPATH_FILTER);
            if (!data.containsKey(YANGLIB_KEY_REVISION)) {
                log.error("Not talking to a supported CzechLight device, is that a teapot?");
                return noDevice;
            }
            final var revision = data.getString(YANGLIB_KEY_REVISION);
            if (data.getString(YANGLIB_KEY_MODULE_NAME).equals(MOD_ROADM_DEVICE)) {
                if (!revision.equals(MOD_ROADM_DEVICE_DATE)) {
                    log.error("Revision mismatch for YANG module {}: got {}", MOD_ROADM_DEVICE, revision);
                    return noDevice;
                }
                final var features = data.getStringArray(YANGLIB_PATH_QUERY_FEATURES);
                isLineDegree = Arrays.stream(features)
                        .anyMatch(s -> s.equals(MOD_ROADM_FEATURE_LINE_DEGREE));
                isAddDrop = Arrays.stream(features)
                        .anyMatch(s -> s.equals(MOD_ROADM_FEATURE_FLEX_ADD_DROP));
                if (!isLineDegree && !isAddDrop) {
                    log.error("Device type not recognized, but {} YANG model is present. Reported YANG features: {}",
                            MOD_ROADM_DEVICE, String.join(", ", features));
                    return noDevice;
                }
            } else if (data.getString(YANGLIB_KEY_MODULE_NAME).equals(MOD_COHERENT_A_D)) {
                if (!revision.equals(MOD_COHERENT_A_D_DATE)) {
                    log.error("Revision mismatch for YANG module {}: got {}", MOD_COHERENT_A_D, revision);
                    return noDevice;
                }
                isCoherentAddDrop = true;
            } else if (data.getString(YANGLIB_KEY_MODULE_NAME).equals(MOD_INLINE_AMP)) {
                if (!revision.equals(MOD_INLINE_AMP_DATE)) {
                    log.error("Revision mismatch for YANG module {}: got {}", MOD_INLINE_AMP, revision);
                    return noDevice;
                }
                isInlineAmp = true;
            }

            if (isLineDegree) {
                log.info("Talking to a Line/Degree ROADM node");
                annotations.set(DEVICE_TYPE_ANNOTATION, DeviceType.LINE_DEGREE.toString());
            } else if (isAddDrop) {
                log.info("Talking to an Add/Drop ROADM node");
                annotations.set(DEVICE_TYPE_ANNOTATION, DeviceType.ADD_DROP_FLEX.toString());
            } else if (isCoherentAddDrop) {
                log.info("Talking to a Coherent Add/Drop ROADM node");
                annotations.set(DEVICE_TYPE_ANNOTATION, DeviceType.COHERENT_ADD_DROP.toString());
            } else if (isInlineAmp) {
                log.info("Talking to an inline ampifier, not a ROADM, but we will fake it as a ROADM for now");
                annotations.set(DEVICE_TYPE_ANNOTATION, DeviceType.INLINE_AMP.toString());
            } else {
                log.error("Device type not recognized");
                return noDevice;
            }
        } catch (NetconfException e) {
            log.error("Cannot request ietf-yang-library data", e);
            return noDevice;
        }

        // FIXME: initialize these
        String vendor       = "CzechLight";
        String hwVersion    = "n/a";
        String swVersion    = "n/a";
        String serialNumber = "n/a";
        ChassisId chassisId = null;

        return new DefaultDeviceDescription(handler().data().deviceId().uri(), Device.Type.ROADM,
                vendor, hwVersion, swVersion, serialNumber, chassisId, annotations.build());
    }

    public static String leafPortName(final DeviceType deviceType, final long number) {
        switch (deviceType) {
            case LINE_DEGREE:
                return LINE_EXPRESS_PREFIX + Long.toString(number);
            default:
                return Long.toString(number);
        }
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        DeviceId deviceId = handler().data().deviceId();
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(deviceId);
        var deviceType = DeviceType.valueOf(device.annotations().value(DEVICE_TYPE_ANNOTATION));

        List<PortDescription> portDescriptions = Lists.newArrayList();

        if (deviceType == DeviceType.INLINE_AMP) {
            DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
            annotations.set(AnnotationKeys.PORT_NAME, DESC_PORT_LINE_WEST);
            annotations.set(OdtnDeviceDescriptionDiscovery.PORT_TYPE,
                    OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.toString());
            portDescriptions.add(omsPortDescription(PortNumber.portNumber(PORT_INLINE_WEST),
                    true,
                    START_CENTER_FREQ_50,
                    END_CENTER_FREQ_50,
                    CHANNEL_SPACING_50.frequency(),
                    annotations.build()));

            annotations = DefaultAnnotations.builder();
            annotations.set(AnnotationKeys.PORT_NAME, DESC_PORT_LINE_EAST);
            annotations.set(OdtnDeviceDescriptionDiscovery.PORT_TYPE,
                    OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.toString());
            portDescriptions.add(omsPortDescription(PortNumber.portNumber(PORT_INLINE_EAST),
                    true,
                    START_CENTER_FREQ_50,
                    END_CENTER_FREQ_50,
                    CHANNEL_SPACING_50.frequency(),
                    annotations.build()));

            return portDescriptions;
        }

        DefaultAnnotations.Builder annotationsForCommon = DefaultAnnotations.builder();
        switch (deviceType) {
            case LINE_DEGREE:
                annotationsForCommon.set(AnnotationKeys.PORT_NAME, DESC_PORT_LINE);
                annotationsForCommon.set(OdtnDeviceDescriptionDiscovery.PORT_TYPE,
                        OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.toString());
                break;
            case ADD_DROP_FLEX:
            case COHERENT_ADD_DROP:
                annotationsForCommon.set(AnnotationKeys.PORT_NAME, DESC_PORT_EXPRESS);
                break;
            case INLINE_AMP:
                assert false : "this cannot happen because it's handled above, but I have to type this here anyway";
            default:
                assert false : "unhandled device type";
        }
        portDescriptions.add(omsPortDescription(PortNumber.portNumber(PORT_COMMON),
                true,
                START_CENTER_FREQ_50,
                END_CENTER_FREQ_50,
                CHANNEL_SPACING_50.frequency(),
                annotationsForCommon.build()));

        final int leafPortCount;
        switch (deviceType) {
            case LINE_DEGREE:
                leafPortCount = 9;
                break;
            case ADD_DROP_FLEX:
                leafPortCount = 20;
                break;
            case COHERENT_ADD_DROP:
                leafPortCount = 8;
                break;
            default:
                log.error("Unsupported CzechLight device type");
                return null;
        }

        for (var i = 1; i <= leafPortCount; ++i) {
            DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
            final Frequency channelSpacing;
            annotations.set(AnnotationKeys.PORT_NAME, leafPortName(deviceType, i));
            switch (deviceType) {
                case LINE_DEGREE:
                    channelSpacing = CHANNEL_SPACING_50.frequency();
                    break;
                case ADD_DROP_FLEX:
                    annotations.set(OdtnDeviceDescriptionDiscovery.PORT_TYPE,
                            OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.toString());
                    channelSpacing = CHANNEL_SPACING_50.frequency();
                    break;
                case COHERENT_ADD_DROP:
                    annotations.set(OdtnDeviceDescriptionDiscovery.PORT_TYPE,
                            OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.toString());
                    channelSpacing = CHANNEL_SPACING_NONE.frequency();
                    break;
                default:
                    log.error("Unsupported CzechLight device type");
                    return null;
            }
            portDescriptions.add(omsPortDescription(PortNumber.portNumber(i),
                    true,
                    START_CENTER_FREQ_50,
                    END_CENTER_FREQ_50,
                    channelSpacing,
                    annotations.build()));
        }
        return portDescriptions;
    }

    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(data().deviceId()).getSession();
    }

    public static double dbmToMilliwatts(final double dbm) {
        return java.lang.Math.pow(10, dbm / 10);
    }

    public static double milliwattsToDbm(final double mw) {
        return 10 * java.lang.Math.log10(mw);
    }

    /** Run a <get> NETCONF command with an XPath filter.
     *
     * @param session well, a NETCONF session
     * @param prefix Name of a XML element prefix to use. Can be meaningless, such as "M"
     * @param namespace Full URI of the XML namespace to use. This is the real meat.
     * @param xpathFilter String with a relative XPath filter. This *MUST* start with "/" followed by 'prefix' and ":".
     * @return Result of the <get/> operation via NETCONF as a XmlHierarchicalConfiguration
     * @throws NetconfException exactly as session.doWrappedRpc() would do.
     * */
    public static HierarchicalConfiguration doGetXPath(final NetconfSession session, final String prefix,
                                                       final String namespace, final String xpathFilter)
            throws NetconfException {
        final var reply = session.doWrappedRpc("<get xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">"
                + "<filter type=\"xpath\" xmlns:" + prefix + "=\"" + namespace + "\""
                + " select=\"" + xpathFilter.replace("\"", "&quot;") + "\"/>"
                + "</get>");
        log.debug("GET RPC w/XPath {}", reply);
        var data = XmlConfigParser.loadXmlString(reply);
        if (!data.containsKey("data[@xmlns]")) {
            log.error("NETCONF <get> w/XPath returned error: {}", reply);
            return null;
        }
        return data;
    }

    public static HierarchicalConfiguration doGetSubtree(final NetconfSession session, final String subtreeXml)
            throws NetconfException {
        final var data = XmlConfigParser.loadXmlString(session.getConfig(DatastoreId.RUNNING, subtreeXml));
        if (!data.containsKey("data[@xmlns]")) {
            log.error("NETCONF <get> w/subtree returned error");
            return null;
        }
        return data;
    }

    /** Massage an XPath fragment (without the "/module:" prefix) into a key suitable for XmlConfigParser.getString()
     *
     * This might or might not work properly for various corner cases. It will fail horribly when XML namespaces are not
     * being done in exactly the same manner as the author of this code assumed was the case.
     *
     * @param xpath XPath subset without the "/module:" prefix, such as "container/another-container/leaf"
     * @return a string to be passed to XmlConfigParser.getString(...) of the XmlHierarchicalConfiguration
     * */
    public static String xpathToXmlKey(final String xpath) {
        return "data." // prefix added by RPC handling
                // turn XPath level delimiters into XmlConfigParser's key format
                + xpath.replace('/', '.')
                // filter out XPath list keys/selectors, they are not visible in XmlConfigParser
                .replaceAll("\\[[^]]*\\]", "");
    }

}
