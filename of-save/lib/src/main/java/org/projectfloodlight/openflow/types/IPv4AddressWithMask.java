package org.projectfloodlight.openflow.types;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;


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

    /**
     * Returns an {@code IPv4AddressWithMask} object that represents the given
     * raw IP address masked by the given raw IP address mask.
     *
     * @param rawValue  the raw IP address to be masked
     * @param rawMask   the raw IP address mask
     * @return          an {@code IPv4AddressWithMask} object that represents
     *                  the given raw IP address masked by the given raw IP
     *                  address mask
     * @deprecated      replaced by {@link IPv4Address#of(int)} and
     *                  {@link IPv4Address#withMask(IPv4Address), e.g. <code>
     *                  IPv4Address.of(int).withMask(IPv4Address.of(int))
     *                  </code>
     */
    @Nonnull
    @Deprecated
    public static IPv4AddressWithMask of(final int rawValue, final int rawMask) {
        return new IPv4AddressWithMask(rawValue, rawMask);
    }

    /**
     * Returns an {@code IPv4AddressWithMask} object that represents the given
     * IP address masked by the given IP address mask. Both arguments are given
     * as {@code IPv4Address} objects.
     *
     * @param value  the IP address to be masked
     * @param mask   the IP address mask
     * @return       an {@code IPv4AddressWithMask} object that represents
     *               the given IP address masked by the given IP address mask
     * @throws NullPointerException  if any of the given {@code IPv4Address}
     *                               objects were {@code null}
     */
    @Nonnull
    public static IPv4AddressWithMask of(
            @Nonnull final IPv4Address value,
            @Nonnull final IPv4Address mask) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(mask, "mask must not be null");

        return new IPv4AddressWithMask(value, mask);
    }

    /**
     * Returns an {@code IPv4AddressWithMask} object that corresponds to
     * the given string in CIDR notation or other acceptable notations.
     * <p>
     * The following notations are accepted.
     * <table><tr>
     * <th>Notation</th><th>Example</th><th>Notes</th>
     * </tr><tr>
     * <td>IPv4 address only</td><td>{@code 1.2.3.4}</td><td>The subnet mask of
     * prefix length 32 (i.e. {@code 255.255.255.255}) is assumed.</td>
     * </tr><tr>
     * <td>IPv4 address/mask</td><td>{@code 1.2.3.4/255.255.255.0}</td>
     * </tr><tr>
     * <td>CIDR notation</td><td>{@code 1.2.3.4/24}</td>
     * </tr></table>
     *
     * @param string  the string in acceptable notations
     * @return        an {@code IPv4AddressWithMask} object that corresponds to
     *                the given string in acceptable notations
     * @throws NullPointerException      if the given string was {@code null}
     * @throws IllegalArgumentException  if the given string was malformed
     */
    @Nonnull
    public static IPv4AddressWithMask of(@Nonnull final String string) {
        Preconditions.checkNotNull(string, "string must not be null");

        int slashPos;
        String ip = string;
        int cidrMaskLength = 32;
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
                    cidrMaskLength = Integer.parseInt(suffix);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("IP Address not well formed: " + string);
            }
        }

        // Read IP
        IPv4Address ipv4 = IPv4Address.of(ip);

        if (maskAddress != null) {
            // Full address mask
            return IPv4AddressWithMask.of(ipv4, maskAddress);
        } else {
            return IPv4AddressWithMask.of(
                    ipv4, IPv4Address.ofCidrMaskLength(cidrMaskLength));
        }
    }

    @Override
    public boolean contains(IPAddress<?> ip) {
        Preconditions.checkNotNull(ip, "ip must not be null");

        if(ip.getIpVersion() == IPVersion.IPv4) {
            IPv4Address ipv4 = (IPv4Address) ip;
            return this.matches(ipv4);
        }

        return false;
    }
}
