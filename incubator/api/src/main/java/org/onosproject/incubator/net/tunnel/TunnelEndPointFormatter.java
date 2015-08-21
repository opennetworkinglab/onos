package org.onosproject.incubator.net.tunnel;


import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.cell.AbstractCellFormatter;

/**
 * Formats a optical tunnel endpoint as "(type)/(element-id)/(port)".
 * Formats a ip tunnel endpoint as "ip".
 */
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
