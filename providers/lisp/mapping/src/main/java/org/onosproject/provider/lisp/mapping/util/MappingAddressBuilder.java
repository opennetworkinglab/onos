/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.provider.lisp.mapping.util;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.lisp.ctl.ExtensionMappingAddressInterpreter;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispAsAddress;
import org.onosproject.lisp.msg.types.LispDistinguishedNameAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;
import org.onosproject.lisp.msg.types.LispMacAddress;
import org.onosproject.lisp.msg.types.lcaf.LispLcafAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.mapping.addresses.ExtensionMappingAddressType.ExtensionMappingAddressTypes.*;

/**
 * Mapping address builder class.
 */
public final class MappingAddressBuilder {

    private static final Logger log =
            LoggerFactory.getLogger(MappingAddressBuilder.class);

    private static final int IPV4_PREFIX_LENGTH = 32;
    private static final int IPV6_PREFIX_LENGTH = 128;

    // prevent from instantiation
    private MappingAddressBuilder() {
    }

    /**
     * Converts LispAfiAddress into abstracted mapping address.
     *
     * @param deviceService device service
     * @param deviceId      device identifier
     * @param address       LispAfiAddress
     * @return abstracted mapping address
     */
    protected static MappingAddress getAddress(DeviceService deviceService,
                                               DeviceId deviceId,
                                               LispAfiAddress address) {

        if (address == null) {
            log.warn("Address is not specified.");
            return null;
        }

        switch (address.getAfi()) {
            case IP4:
                return afi2mapping(address);
            case IP6:
                return afi2mapping(address);
            case AS:
                int asNum = ((LispAsAddress) address).getASNum();
                return MappingAddresses.asMappingAddress(String.valueOf(asNum));
            case DISTINGUISHED_NAME:
                String dn = ((LispDistinguishedNameAddress)
                        address).getDistinguishedName();
                return MappingAddresses.dnMappingAddress(dn);
            case MAC:
                MacAddress macAddress = ((LispMacAddress) address).getAddress();
                return MappingAddresses.ethMappingAddress(macAddress);
            case LCAF:
                return deviceService == null ? null :
                        lcaf2extension(deviceService, deviceId, (LispLcafAddress) address);
            default:
                log.warn("Unsupported address type {}", address.getAfi());
                break;
        }

        return null;
    }

    /**
     * Converts AFI address to generalized mapping address.
     *
     * @param afiAddress IP typed AFI address
     * @return generalized mapping address
     */
    private static MappingAddress afi2mapping(LispAfiAddress afiAddress) {
        switch (afiAddress.getAfi()) {
            case IP4:
                IpAddress ipv4Address = ((LispIpv4Address) afiAddress).getAddress();
                IpPrefix ipv4Prefix = IpPrefix.valueOf(ipv4Address, IPV4_PREFIX_LENGTH);
                return MappingAddresses.ipv4MappingAddress(ipv4Prefix);
            case IP6:
                IpAddress ipv6Address = ((LispIpv6Address) afiAddress).getAddress();
                IpPrefix ipv6Prefix = IpPrefix.valueOf(ipv6Address, IPV6_PREFIX_LENGTH);
                return MappingAddresses.ipv6MappingAddress(ipv6Prefix);
            default:
                log.warn("Only support to convert IP address type");
                break;
        }
        return null;
    }

    /**
     * Converts LCAF address to extension mapping address.
     *
     * @param deviceService device service
     * @param deviceId      device identifier
     * @param lcaf          LCAF address
     * @return extension mapping address
     */
    private static MappingAddress lcaf2extension(DeviceService deviceService,
                                                 DeviceId deviceId,
                                                 LispLcafAddress lcaf) {

        Device device = deviceService.getDevice(deviceId);

        ExtensionMappingAddressInterpreter addressInterpreter;
        ExtensionMappingAddress mappingAddress = null;
        if (device.is(ExtensionMappingAddressInterpreter.class)) {
            addressInterpreter = device.as(ExtensionMappingAddressInterpreter.class);
        } else {
            addressInterpreter = null;
        }

        switch (lcaf.getType()) {
            case LIST:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(LIST_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case SEGMENT:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(SEGMENT_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case AS:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(AS_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case APPLICATION_DATA:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(APPLICATION_DATA_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case GEO_COORDINATE:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(GEO_COORDINATE_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case NAT:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(NAT_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case NONCE:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(NONCE_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case MULTICAST:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(MULTICAST_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case TRAFFIC_ENGINEERING:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(TRAFFIC_ENGINEERING_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            case SOURCE_DEST:
                if (addressInterpreter != null &&
                        addressInterpreter.supported(SOURCE_DEST_ADDRESS.type())) {
                    mappingAddress = addressInterpreter.mapLcafAddress(lcaf);
                }
                break;
            default:
                log.warn("Unsupported extension mapping address type {}", lcaf.getType());
                break;
        }

        return mappingAddress != null ?
                MappingAddresses.extensionMappingAddressWrapper(mappingAddress, deviceId) : null;
    }
}
