/*
 * Copyright 2015 Open Networking Laboratory
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

/**
 * Formats a optical tunnel endpoint as "(type)/(element-id)/(port)".
 * Formats a ip tunnel endpoint as "ip".
 */
@Beta
public final class TunnelEndPointFormatter extends AbstractCellFormatter {
    //non-instantiable
    private TunnelEndPointFormatter() {
    }

    @Override
    protected String nonNullFormat(Object value) {

        if (value instanceof DefaultOpticalTunnelEndPoint) {
            DefaultOpticalTunnelEndPoint cp = (DefaultOpticalTunnelEndPoint) value;
            return cp.type() + "/" + cp.elementId().get() + "/" + cp.portNumber().get();
        } else if (value instanceof IpTunnelEndPoint) {
            IpTunnelEndPoint cp = (IpTunnelEndPoint) value;
            return cp.ip().toString();
        }
        return "";
    }

    /**
     * An instance of this class.
     */
    public static final CellFormatter INSTANCE = new TunnelEndPointFormatter();
}
