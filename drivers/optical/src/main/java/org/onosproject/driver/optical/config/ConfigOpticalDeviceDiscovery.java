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
package org.onosproject.driver.optical.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.onlab.packet.ChassisId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.inject.DeviceInjectionConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.config.OpticalPortConfig;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
/*
 * Example:
 * CHECKSTYLE:OFF
{
    "ports" : {
      "inject:10.49.54.31/1" : {

# see OpticalPortConfig
        "optical" : {
          "type" : "OCH"
        },
# Annotations required by projection mechanism. see OchPortHelper, etc.
        "annotations" : {
          "entries" : {
            "comment" : "following is required annotations for OCH port",
            "tunable" : "true",
            "lambda" : "{ \"gridType\": \"FLEX\", \"channelSpacing\": \"CHL_6P25GHZ\", \"spacingMultiplier\": 42, \"slotGranularity\": 1 }",
            "signalType" : "ODUC4"
          }
        },
# see ConfigLambdaQuery
        "lambdas" : {
          "gridType" : "DWDM",
          "dwdmSpacing" : "CHL_50GHZ",
          "slotStart" : 1,
          "slotSttep" : 1,
          "slotEnd" : 5
        }
      },

      "inject:10.49.54.31/2" : {
        "optical" : {
          "type" : "OMS"
        },
        "annotations" : {
          "entries" : {
            "comment" : "following is required annotations for OMS port",
            "minFrequency" : "178981000000000",
            "maxFrequency" : "237931000000000",
            "grid" : "100000000000"
          }
        },
        "lambdas" : {
          "gridType" : "FLEX",
          "slotStart" : 1,
          "slotStep" : 2,
          "slotEnd" : 3585
        }
      },

      "inject:10.49.54.31/3" : {
        "optical" : {
          "type" : "ODUCLT",
          "speed" : 100000
        },
        "annotations" : {
          "entries" : {
            "comment" : "following is required annotations for ODUCLT port",
            "signalType" : "CLT_100GBE"
          }
        }
      },

      "inject:10.49.54.31/4" : {
        "optical" : {
          "speed" : 100000
        }
      }
    },
#
# Note: "ports" must be defined before "devices" when submitting in single tx.
#
    "devices" : {
      "inject:10.49.54.31" : {
        "basic" : {
# specify driver to use ConfigOpticalDeviceDiscovery, etc.
          "driver" : "optical-config",
          "type" : "ROADM_OTN",
          "hwVersion" : "1.0",
          "swVersion" : "2.0",
          "serial" : "R-3000"
        },
        "inject" : {
# This device has 5 ports [1-5]
# details defined in /ports tree above.
          "ports" : "5"
        }
      }
    }
}
 * CHECKSTYLE:ON
 */

/**
 * DeviceDescriptionDiscovery implementation, which
 * utilizes {@link DeviceInjectionConfig}, {@link BasicDeviceConfig},
 * {@link OpticalPortConfig} to populate Device and Ports based on configuration.
 */
@Beta
public class ConfigOpticalDeviceDiscovery extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    @Override
    public DeviceDescription discoverDeviceDetails() {
        NetworkConfigService netcfg = handler().get(NetworkConfigService.class);
        DeviceId did = data().deviceId();

        String unk = "UNKNOWN";

        Optional<DeviceInjectionConfig> inject =
                Optional.ofNullable(netcfg.getConfig(did, DeviceInjectionConfig.class));

        Optional<BasicDeviceConfig> basic =
                Optional.ofNullable(netcfg.getConfig(did, BasicDeviceConfig.class));

        Device.Type type = basic.map(BasicDeviceConfig::type).orElse(Device.Type.SWITCH);
        String manufacturer = basic.map(BasicDeviceConfig::manufacturer).orElse(unk);
        String hwVersion = basic.map(BasicDeviceConfig::hwVersion).orElse(unk);
        String swVersion = basic.map(BasicDeviceConfig::swVersion).orElse(unk);
        String serialNumber = basic.map(BasicDeviceConfig::serial).orElse(unk);
        ChassisId chassis = new ChassisId();
        // if inject is not specified, return default unavailable device
        boolean defaultAvailable = inject.isPresent();
        return new DefaultDeviceDescription(did.uri(),
                                            type,
                                            manufacturer,
                                            hwVersion,
                                            swVersion,
                                            serialNumber,
                                            chassis,
                                            defaultAvailable);
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        NetworkConfigService netcfg = handler().get(NetworkConfigService.class);
        DeviceId did = data().deviceId();

        DeviceInjectionConfig cfg = netcfg.getConfig(did, DeviceInjectionConfig.class);
        if (cfg == null) {
            return ImmutableList.of();
        }
        String ports = cfg.ports();
        // TODO: parse port format like [1-3,6] in the future
        int numPorts = Integer.parseInt(ports);

        List<PortDescription> portDescs = new ArrayList<>(numPorts);
        for (int i = 1; i <= numPorts; ++i) {
            PortNumber number = PortNumber.portNumber(i);
            ConnectPoint cp = new ConnectPoint(did, number);

            Optional<OpticalPortConfig> port =
                    Optional.ofNullable(netcfg.getConfig(cp, OpticalPortConfig.class));

            boolean isEnabled = true;
            // default packet port speed on configured-optical device (in Mbps)
            int speedFallback = 10_000;
            long portSpeed = port.flatMap(OpticalPortConfig::speed).orElse(speedFallback);
            Port.Type type = port.map(OpticalPortConfig::type).orElse(Port.Type.COPPER);

            portDescs.add(DefaultPortDescription.builder()
                    .withPortNumber(number)
                    .isEnabled(isEnabled)
                    .type(type)
                    .portSpeed(portSpeed)
                    .build());
        }

        return portDescs;
    }

}
