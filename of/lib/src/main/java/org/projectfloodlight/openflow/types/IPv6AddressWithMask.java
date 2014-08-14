package org.projectfloodlight.openflow.types;

import com.google.common.base.Preconditions;

public class IPv6AddressWithMask extends IPAddressWithMask<IPv6Address> {
    public final static IPv6AddressWithMask NONE = of(IPv6Address.NONE, IPv6Address.NONE);

    private IPv6AddressWithMask(IPv6Address value, IPv6Address mask) {
        super(value, mask);
    }

    @Override
    public IPVersion getIpVersion() {
        return IPVersion.IPv6;
    }

    public static IPv6AddressWithMask of(IPv6Address value, IPv6Address mask) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(mask, "mask must not be null");
        return new IPv6AddressWithMask(value, mask);
    }

    public static IPv6AddressWithMask of(final String string) {
        Preconditions.checkNotNull(string, "string must not be null");

        int slashPos;
        String ip = string;
        int cidrMaskLength = 128;
        IPv6Address maskAddress = null;

        // Read mask suffix
        if ((slashPos = string.indexOf('/')) != -1) {
            ip = string.substring(0, slashPos);
            try {
                String suffix = string.substring(slashPos + 1);
                if (suffix.length() == 0)
                    throw new IllegalArgumentException("IPv6 Address not well formed: " + string);
                if (suffix.indexOf(':') != -1) {
                    // Full mask
                    maskAddress = IPv6Address.of(suffix);
                } else {
                    // CIDR Suffix
                    cidrMaskLength = Integer.parseInt(suffix);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("IPv6 Address not well formed: " + string);
            }
        }

        // Read IP
        IPv6Address ipv6 = IPv6Address.of(ip);

        if (maskAddress != null) {
            // Full address mask
            return IPv6AddressWithMask.of(ipv6, maskAddress);
        } else {
            return IPv6AddressWithMask.of(ipv6,
                    IPv6Address.ofCidrMaskLength(cidrMaskLength));
        }
    }

    @Override
    public boolean contains(IPAddress<?> ip) {
        Preconditions.checkNotNull(ip, "ip must not be null");

        if(ip.getIpVersion() == IPVersion.IPv6) {
            IPv6Address ipv6 = (IPv6Address) ip;
            return this.matches(ipv6);
        }

        return false;
    }
}
