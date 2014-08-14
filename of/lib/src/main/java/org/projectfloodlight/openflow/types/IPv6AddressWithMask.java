package org.projectfloodlight.openflow.types;

import java.math.BigInteger;
import java.util.Arrays;

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
        if (value == null) {
            throw new NullPointerException("Value must not be null");
        }
        if (mask == null) {
            throw new NullPointerException("Mask must not be null");
        }
        return new IPv6AddressWithMask(value, mask);
    }


    public static IPv6AddressWithMask of(final String string) {
        if (string == null) {
            throw new NullPointerException("String must not be null");
        }
        int slashPos;
        String ip = string;
        int maskBits = 128;
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
                    maskBits = Integer.parseInt(suffix);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("IPv6 Address not well formed: " + string);
            }
            if (maskBits < 0 || maskBits > 128) {
                throw new IllegalArgumentException("IPv6 Address not well formed: " + string);
            }
        }

        // Read IP
        IPv6Address ipv6 = IPv6Address.of(ip);

        if (maskAddress != null) {
            // Full address mask
            return IPv6AddressWithMask.of(ipv6, maskAddress);
        } else if (maskBits == 128) {
            // No mask
            return IPv6AddressWithMask.of(ipv6, IPv6Address.NO_MASK);
        } else if (maskBits == 0) {
            // Entirely masked out
            return IPv6AddressWithMask.of(ipv6, IPv6Address.FULL_MASK);
        }else {
            // With mask
            BigInteger mask = BigInteger.ONE.negate().shiftLeft(128 - maskBits);
            byte[] maskBytesTemp = mask.toByteArray();
            byte[] maskBytes;
            if (maskBytesTemp.length < 16) {
                maskBytes = new byte[16];
                System.arraycopy(maskBytesTemp, 0, maskBytes, 16 - maskBytesTemp.length, maskBytesTemp.length);
                Arrays.fill(maskBytes, 0, 16 - maskBytesTemp.length, (byte)(0xFF));
            } else if (maskBytesTemp.length > 16) {
                maskBytes = new byte[16];
                System.arraycopy(maskBytesTemp, 0, maskBytes, 0, maskBytes.length);
            } else {
                maskBytes = maskBytesTemp;
            }
            return IPv6AddressWithMask.of(ipv6, IPv6Address.of(maskBytes));
        }
    }


}
