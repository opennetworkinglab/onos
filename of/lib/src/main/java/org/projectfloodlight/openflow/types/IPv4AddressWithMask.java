package org.projectfloodlight.openflow.types;


public class IPv4AddressWithMask extends IPAddressWithMask<IPv4Address> {
    public final static IPv4AddressWithMask NONE = of(IPv4Address.NONE, IPv4Address.NONE);

    private IPv4AddressWithMask(int rawValue, int rawMask) {
        super(IPv4Address.of(rawValue), IPv4Address.of(rawMask));
    }

    private IPv4AddressWithMask(IPv4Address value, IPv4Address mask) {
        super(value, mask);
    }

    @Override
    public IPVersion getIpVersion() {
        return IPVersion.IPv4;
    }

    public static IPv4AddressWithMask of(int rawValue, int rawMask) {
        return new IPv4AddressWithMask(rawValue, rawMask);
    }

    public static IPv4AddressWithMask of(IPv4Address value, IPv4Address mask) {
        if (value == null) {
            throw new NullPointerException("Value must not be null");
        }
        if (mask == null) {
            throw new NullPointerException("Mask must not be null");
        }
        return new IPv4AddressWithMask(value, mask);
    }

    public static IPv4AddressWithMask of(final String string) {
        if (string == null) {
            throw new NullPointerException("String must not be null");
        }
        int slashPos;
        String ip = string;
        int maskBits = 32;
        IPv4Address maskAddress = null;

        // Read mask suffix
        if ((slashPos = string.indexOf('/')) != -1) {
            ip = string.substring(0, slashPos);
            try {
                String suffix = string.substring(slashPos + 1);
                if (suffix.length() == 0)
                    throw new IllegalArgumentException("IP Address not well formed: " + string);
                if (suffix.indexOf('.') != -1) {
                    // Full mask
                    maskAddress = IPv4Address.of(suffix);
                } else {
                    // CIDR Suffix
                    maskBits = Integer.parseInt(suffix);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("IP Address not well formed: " + string);
            }
            if (maskBits < 0 || maskBits > 32) {
                throw new IllegalArgumentException("IP Address not well formed: " + string);
            }
        }

        // Read IP
        IPv4Address ipv4 = IPv4Address.of(ip);

        if (maskAddress != null) {
            // Full address mask
            return IPv4AddressWithMask.of(ipv4, maskAddress);
        } else if (maskBits == 32) {
            // No mask
            return IPv4AddressWithMask.of(ipv4, IPv4Address.NO_MASK);
        } else if (maskBits == 0) {
            // No mask
            return IPv4AddressWithMask.of(ipv4, IPv4Address.FULL_MASK);
        } else {
            // With mask
            int mask = (-1) << (32 - maskBits);
            return IPv4AddressWithMask.of(ipv4, IPv4Address.of(mask));
        }
    }

}
