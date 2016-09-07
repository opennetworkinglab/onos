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
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.optical.device.OduCltPortHelper;
import org.onosproject.net.optical.impl.DefaultOduCltPort;

import com.google.common.annotations.Beta;

/**
 * {@link PortMapper} to handler {@link OduCltPort} translation.
 */
@Beta
public class OduCltPortMapper extends AbstractPortMapper<OduCltPort> {

    @Override
    public boolean is(Port port) {
        return port != null &&
               port.type() == Port.Type.ODUCLT &&
               super.is(port);
    }

    @Override
    public Optional<OduCltPort> as(Port port) {
        if (port instanceof OduCltPort) {
            return Optional.of((OduCltPort) port);
        }
        return super.as(port);
    }

    @Override
    protected Optional<OduCltPort> mapPort(Port port) {
        if (port instanceof OduCltPort) {
            return Optional.of((OduCltPort) port);
        } else if (port instanceof org.onosproject.net.OduCltPort) {
            // TODO remove after deprecation of old OduCltPort is complete

            // translate to new OduCltPort
            org.onosproject.net.OduCltPort old = (org.onosproject.net.OduCltPort) port;
            return Optional.of(new DefaultOduCltPort(old,
                                                     old.signalType()));
        }

        return OduCltPortHelper.asOduCltPort(port);
    }

}
