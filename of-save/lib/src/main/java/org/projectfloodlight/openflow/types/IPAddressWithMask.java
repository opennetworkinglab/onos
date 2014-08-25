package org.projectfloodlight.openflow.types;

import com.google.common.base.Preconditions;


public abstract class IPAddressWithMask<F extends IPAddress<F>> extends Masked<F> {

    protected IPAddressWithMask(F value, F mask) {
        super(value, mask);
    }

    public abstract IPVersion getIpVersion();
    
    public abstract boolean contains(IPAddress<?> ip);

    public F getSubnetBroadcastAddress() {
        if (!mask.isCidrMask()) {
            throw new IllegalArgumentException("Mask Invalid " + mask +
                                               " cannot get subnet for non CIDR mask");
        }
        return value.or(mask.not());
    }

    public boolean isSubnetBroadcastAddress(F candidate) {
        return getSubnetBroadcastAddress().equals(candidate);
    }

    public static IPAddressWithMask<?> of(String ip) {
        Preconditions.checkNotNull(ip, "string ip must not be null");

        if (ip.indexOf('.') != -1)
            return IPv4AddressWithMask.of(ip);
        else if (ip.indexOf(':') != -1)
            return IPv6AddressWithMask.of(ip);
        else
            throw new IllegalArgumentException("IP Address not well formed: " + ip);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append(value.toString());

        res.append('/');
        if (mask.isCidrMask()) {
            // CIDR notation
            res.append(mask.asCidrMaskLength());
        } else {
            // Full address mask
            res.append(mask.toString());
        }

        return res.toString();
    }
}
