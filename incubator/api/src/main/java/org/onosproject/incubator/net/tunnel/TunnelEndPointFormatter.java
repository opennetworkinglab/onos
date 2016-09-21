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
package org.onosproject.incubator.net.tunnel;


import com.google.common.annotations.Beta;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.cell.AbstractCellFormatter;

import java.util.Optional;

/**
 * Formats an optical tunnel endpoint as "(type)/(element-id)/(port)".
 * Formats an IP tunnel endpoint as "ip".
 */
@Beta
public final class TunnelEndPointFormatter extends AbstractCellFormatter {
    //non-instantiable
    private TunnelEndPointFormatter() {
    }

    private String safeOptional(Optional<?> optional) {
        return optional.isPresent() ? optional.get().toString() : QUERY;
    }

    @Override
    protected String nonNullFormat(Object value) {

        if (value instanceof DefaultOpticalTunnelEndPoint) {
            DefaultOpticalTunnelEndPoint ep =
                    (DefaultOpticalTunnelEndPoint) value;

            String e = safeOptional(ep.elementId());
            String p = safeOptional(ep.portNumber());
            return ep.type() + SLASH + e + SLASH + p;

        } else if (value instanceof IpTunnelEndPoint) {
            IpTunnelEndPoint cp = (IpTunnelEndPoint) value;
            return cp.ip().toString();
        }
        return EMPTY;
    }

    /**
     * An instance of this class.
     */
    public static final CellFormatter INSTANCE = new TunnelEndPointFormatter();
}
