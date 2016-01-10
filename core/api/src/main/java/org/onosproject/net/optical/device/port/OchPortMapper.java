/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.optical.device.port;

import java.util.Optional;

import org.onosproject.net.Port;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.net.optical.impl.DefaultOchPort;

import com.google.common.annotations.Beta;

/**
 * {@link PortMapper} to handler {@link OchPort} translation.
 */
@Beta
public class OchPortMapper extends AbstractPortMapper<OchPort> {

    @Override
    public boolean is(Port port) {
        return port != null &&
               port.type() == Port.Type.OCH &&
               super.is(port);
    }

    @Override
    public Optional<OchPort> as(Port port) {
        if (port instanceof OchPort) {
            return Optional.of((OchPort) port);
        }
        return super.as(port);
    }

    @Override
    protected Optional<OchPort> mapPort(Port port) {
        if (port instanceof OchPort) {
            return Optional.of((OchPort) port);
        } else if (port instanceof org.onosproject.net.OchPort) {
            // TODO remove after deprecation of old OchPort is complete

            // translate to new OchPort
            org.onosproject.net.OchPort old = (org.onosproject.net.OchPort) port;
            return Optional.of(new DefaultOchPort(old,
                                                  old.signalType(),
                                                  old.isTunable(),
                                                  old.lambda()));
        }

        return OchPortHelper.asOchPort(port);
    }
}
