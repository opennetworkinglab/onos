package org.onlab.packet;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Tests for class {@link Ip6Prefix}.
 */
public class Ip6PrefixTest {
    /**
     * Tests the immutability of {@link Ip6Prefix}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(Ip6Prefix.class);
    }

    /**
     * Tests default class constructor.
     */
    @Test
    public void testDefaultConstructor() {
        Ip6Prefix ip6prefix = new Ip6Prefix();
        assertThat(ip6prefix.toString(), is("::/0"));
    }

    /**
     * Tests valid class copy constructor.
     */
    @Test
    public void testCopyConstructor() {
        Ip6Prefix fromAddr = new Ip6Prefix("1100::/8");
        Ip6Prefix ip6prefix = new Ip6Prefix(fromAddr);
        assertThat(ip6prefix.toString(), is("1100::/8"));

        fromAddr = new Ip6Prefix("::/0");
        ip6prefix = new Ip6Prefix(fromAddr);
        assertThat(ip6prefix.toString(), is("::/0"));

        fromAddr =
            new Ip6Prefix("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        ip6prefix = new Ip6Prefix(fromAddr);
        assertThat(ip6prefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }

    /**
     * Tests invalid class copy constructor for a null object to copy from.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullObject() {
        Ip6Prefix fromAddr = null;
        Ip6Prefix ip6prefix = new Ip6Prefix(fromAddr);
    }

    /**
     * Tests valid class constructor for an address and prefix length.
     */
    @Test
    public void testConstructorForAddressAndPrefixLength() {
        Ip6Prefix ip6prefix =
            new Ip6Prefix(new Ip6Address("1100::"), (short) 8);
        assertThat(ip6prefix.toString(), is("1100::/8"));

        ip6prefix =
            new Ip6Prefix(new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8885"),
                        (short) 8);
        assertThat(ip6prefix.toString(), is("1100::/8"));

        ip6prefix =
            new Ip6Prefix(new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8800"),
                        (short) 120);
        assertThat(ip6prefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ip6prefix = new Ip6Prefix(new Ip6Address("::"), (short) 0);
        assertThat(ip6prefix.toString(), is("::/0"));

        ip6prefix =
            new Ip6Prefix(new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8885"),
                        (short) 128);
        assertThat(ip6prefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8885/128"));

        ip6prefix =
            new Ip6Prefix(new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"),
                        (short) 128);
        assertThat(ip6prefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));

        ip6prefix =
            new Ip6Prefix(new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8885"),
                        (short) 64);
        assertThat(ip6prefix.toString(), is("1111:2222:3333:4444::/64"));
    }

    /**
     * Tests valid class constructor for a string.
     */
    @Test
    public void testConstructorForString() {
        Ip6Prefix ip6prefix = new Ip6Prefix("1100::/8");
        assertThat(ip6prefix.toString(), is("1100::/8"));

        ip6prefix = new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/8");
        assertThat(ip6prefix.toString(), is("1100::/8"));

        ip6prefix =
            new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8800/120");
        assertThat(ip6prefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ip6prefix = new Ip6Prefix("::/0");
        assertThat(ip6prefix.toString(), is("::/0"));

        ip6prefix =
            new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/128");
        assertThat(ip6prefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8885/128"));

        ip6prefix = new Ip6Prefix("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ip6prefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));

        ip6prefix =
            new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/64");
        assertThat(ip6prefix.toString(), is("1111:2222:3333:4444::/64"));
    }

    /**
     * Tests invalid class constructor for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullString() {
        String fromString = null;
        Ip6Prefix ip6prefix = new Ip6Prefix(fromString);
    }

    /**
     * Tests invalid class constructor for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructors() {
        // Check constructor for invalid ID: empty string
        Ip6Prefix ip6prefix = new Ip6Prefix("");
    }

    /**
     * Tests getting the value of an address.
     */
    @Test
    public void testGetValue() {
        Ip6Prefix ip6prefix = new Ip6Prefix("1100::/8");
        assertThat(ip6prefix.getAddress(), equalTo(new Ip6Address("1100::")));
        assertThat(ip6prefix.getPrefixLen(), is((short) 8));

        ip6prefix = new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/8");
        assertThat(ip6prefix.getAddress(), equalTo(new Ip6Address("1100::")));
        assertThat(ip6prefix.getPrefixLen(), is((short) 8));

        ip6prefix =
            new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8800/120");
        assertThat(ip6prefix.getAddress(),
                   equalTo(new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8800")));
        assertThat(ip6prefix.getPrefixLen(), is((short) 120));

        ip6prefix = new Ip6Prefix("::/0");
        assertThat(ip6prefix.getAddress(), equalTo(new Ip6Address("::")));
        assertThat(ip6prefix.getPrefixLen(), is((short) 0));

        ip6prefix =
            new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/128");
        assertThat(ip6prefix.getAddress(),
                   equalTo(new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8885")));
        assertThat(ip6prefix.getPrefixLen(), is((short) 128));

        ip6prefix =
            new Ip6Prefix("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ip6prefix.getAddress(),
                   equalTo(new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
        assertThat(ip6prefix.getPrefixLen(), is((short) 128));

        ip6prefix =
            new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/64");
        assertThat(ip6prefix.getAddress(),
                   equalTo(new Ip6Address("1111:2222:3333:4444::")));
        assertThat(ip6prefix.getPrefixLen(), is((short) 64));
    }

    /**
     * Tests equality of {@link Ip6Address}.
     */
    @Test
    public void testEquality() {
        Ip6Prefix addr1net = new Ip6Prefix("1100::/8");
        Ip6Prefix addr2net = new Ip6Prefix("1100::/8");
        assertThat(addr1net, is(addr2net));

        addr1net = new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/8");
        addr2net = new Ip6Prefix("1100::/8");
        assertThat(addr1net, is(addr2net));

        addr1net = new Ip6Prefix("::/0");
        addr2net = new Ip6Prefix("::/0");
        assertThat(addr1net, is(addr2net));

        addr1net =
            new Ip6Prefix("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        addr2net =
            new Ip6Prefix("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(addr1net, is(addr2net));
    }

    /**
     * Tests non-equality of {@link Ip6Address}.
     */
    @Test
    public void testNonEquality() {
        Ip6Prefix addr1net = new Ip6Prefix("1100::/8");
        Ip6Prefix addr2net = new Ip6Prefix("1200::/8");
        Ip6Prefix addr3net = new Ip6Prefix("1200::/12");
        Ip6Prefix addr4net = new Ip6Prefix("::/0");
        Ip6Prefix addr5net =
            new Ip6Prefix("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(addr1net, is(not(addr2net)));
        assertThat(addr3net, is(not(addr2net)));
        assertThat(addr4net, is(not(addr2net)));
        assertThat(addr5net, is(not(addr2net)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        Ip6Prefix ip6prefix = new Ip6Prefix("1100::/8");
        assertThat(ip6prefix.toString(), is("1100::/8"));

        ip6prefix = new Ip6Prefix("1111:2222:3333:4444:5555:6666:7777:8885/8");
        assertThat(ip6prefix.toString(), is("1100::/8"));

        ip6prefix = new Ip6Prefix("::/0");
        assertThat(ip6prefix.toString(), is("::/0"));

        ip6prefix =
            new Ip6Prefix("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ip6prefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }
}
