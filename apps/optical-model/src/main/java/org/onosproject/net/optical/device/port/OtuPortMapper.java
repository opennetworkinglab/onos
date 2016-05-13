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
import org.onosproject.net.optical.OtuPort;
import org.onosproject.net.optical.device.OtuPortHelper;
import org.onosproject.net.optical.impl.DefaultOtuPort;

import com.google.common.annotations.Beta;

/**
 * {@link PortMapper} to handler {@link OtuPort} translation.
 */
@Beta
public class OtuPortMapper extends AbstractPortMapper<OtuPort> {

    @Override
    public boolean is(Port port) {
        return port != null &&
               port.type() == Port.Type.OTU &&
               super.is(port);
    }

    @Override
    public Optional<OtuPort> as(Port port) {
        if (port instanceof OtuPort) {
            return Optional.of((OtuPort) port);
        }
        return super.as(port);
    }

    @Override
    protected Optional<OtuPort> mapPort(Port port) {
        if (port instanceof OtuPort) {
            return Optional.of((OtuPort) port);
        } else if (port instanceof org.onosproject.net.OtuPort) {
            // TODO remove after deprecation of old OtuPort is complete

            // translate to new OtuPort
            org.onosproject.net.OtuPort old = (org.onosproject.net.OtuPort) port;
            return Optional.of(new DefaultOtuPort(old,
                                                  old.signalType()));
        }

        return OtuPortHelper.asOtuPort(port);
    }


}
