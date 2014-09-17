package org.projectfloodlight.openflow.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.MessageFormat;

import org.hamcrest.Matchers;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Before;
import org.junit.Test;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class U128Test {
    private Triple[] triples;


    @Test
    public void testPositiveRaws() {
        assertThat(U128.of(0, 0).getMsb(), equalTo(0L));
        assertThat(U128.of(0, 0).getLsb(), equalTo(0L));

        assertThat(U128.of(1, 2).getMsb(), equalTo(1L));
        assertThat(U128.of(1, 2).getLsb(), equalTo(2L));
    }

    @Test
    public void testReadBytes() {
        ChannelBuffer empty = ChannelBuffers.wrappedBuffer(new byte[16]);
        U128 uEmpty = U128.read16Bytes(empty);
        assertThat(uEmpty.getMsb(), equalTo(0L));
        assertThat(uEmpty.getLsb(), equalTo(0L));

        ChannelBuffer value = ChannelBuffers.wrappedBuffer(
                new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88,
                        (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
                        (byte) 0xee, (byte) 0xff, 0x11 });
        U128 uValue = U128.read16Bytes(value);
        assertThat(uValue.getMsb(), equalTo(0x1122334455667788L));
        assertThat(uValue.getLsb(), equalTo(0x99aabbccddeeff11L));
    }

    @Test
    public void testPutTo() {
        U128 h         = U128.of(0x1234_5678_90ab_cdefL,0xdeafbeefdeadbeefL);
        U128 hSame     = U128.of(0x1234_5678_90ab_cdefL,0xdeafbeefdeadbeefL);

        U128 hBothDiff = U128.of(0x1234_5678_90ab_cdefL,0x1234_5678_90ab_cdefL);
        U128 hMsbDiff  = U128.of(0x0234_5678_90ab_cdefL,0xdeafbeefdeadbeefL);
        U128 hLsbDiff  = U128.of(0x1234_5678_90ab_cdefL,0xdeafbeefdeadbeeeL);

        assertThat(hash(h), equalTo(hash(hSame)));
        assertThat(hash(h), not(hash(hBothDiff)));
        assertThat(hash(h), not(hash(hMsbDiff)));
        assertThat(hash(h), not(hash(hLsbDiff)));
    }

    private HashCode hash(U128 f) {
        Hasher hash = Hashing.murmur3_128().newHasher();
        f.putTo(hash);
        return hash.hash();

    }

    @Test
    public void testEqualHashCode() {
        U128 h1 = U128.of(0xdeafbeefdeadbeefL, 0xdeafbeefdeadbeefL);
        U128 h2 = U128.of(0xdeafbeefdeadbeefL, 0xdeafbeefdeadbeefL);
        U128 h3 = U128.of(0xeeafbeefdeadbeefL, 0xdeafbeefdeadbeefL);
        U128 h3_2 = U128.of(0xdeafbeefdeadbeefL, 0xeeafbeefdeadbeefL);

        assertTrue(h1.equals(h1));
        assertTrue(h1.equals(h2));
        assertFalse(h1.equals(h3));
        assertFalse(h1.equals(h3_2));
        assertTrue(h2.equals(h1));

        assertEquals(h1.hashCode(), h2.hashCode());
        assertNotEquals(h1.hashCode(), h3.hashCode()); // not technically a requirement, but we'll hopefully be lucky.
        assertNotEquals(h1.hashCode(), h3_2.hashCode()); // not technically a requirement, but we'll hopefully be lucky.
    }

    @Test
    public void testXor() {
        U128 hNull = U128.of(0, 0);
        U128 hDeadBeef = U128.of(0xdeafbeefdeadbeefL, 0xdeafbeefdeadbeefL);
        assertThat(hNull.xor(hNull), equalTo(hNull));
        assertThat(hNull.xor(hDeadBeef), equalTo(hDeadBeef));
        assertThat(hDeadBeef.xor(hNull), equalTo(hDeadBeef));
        assertThat(hDeadBeef.xor(hDeadBeef), equalTo(hNull));


        U128 h1_0 = U128.of(1L, 0);
        U128 h8_0 = U128.of(0x8000000000000000L, 0);
        U128 h81_0 = U128.of(0x8000000000000001L, 0);
        assertThat(h1_0.xor(h8_0), equalTo(h81_0));

        U128 h0_1 = U128.of(0, 1L);
        U128 h0_8 = U128.of(0, 0x8000000000000000L);
        U128 h0_81 = U128.of(0, 0x8000000000000001L);
        assertThat(h0_1.xor(h0_8), equalTo(h0_81));
    }

    @Test
    public void testKeyBits() {
        U128 zeroU = U128.of(0,0);
        assertThat(zeroU.prefixBits(0), equalTo(0));
        assertThat(zeroU.prefixBits(16), equalTo(0));
        assertThat(zeroU.prefixBits(32), equalTo(0));

        checkInvalidKeyBitSize(zeroU, 33);
        checkInvalidKeyBitSize(zeroU, 64);
        assertThat(zeroU.prefixBits(3), equalTo(0));

        U128 positiveU = U128.of(0x1234_5678_1234_5678L, 0x1234_5678_1234_5678L);
        assertThat(positiveU.prefixBits(0), equalTo(0));
        assertThat(positiveU.prefixBits(16), equalTo(0x1234));
        assertThat(positiveU.prefixBits(32), equalTo(0x12345678));
        checkInvalidKeyBitSize(positiveU, 33);
        checkInvalidKeyBitSize(positiveU, 64);

        U128 signedBitU = U128.of(0x8765_4321_8765_4321L, 0x1234_5678_1234_5678L);
        assertThat(signedBitU.prefixBits(0), equalTo(0));
        assertThat(signedBitU.prefixBits(16), equalTo(0x8765));
        assertThat(signedBitU.prefixBits(32), equalTo(0x8765_4321));
        checkInvalidKeyBitSize(signedBitU, 33);
        checkInvalidKeyBitSize(signedBitU, 64);
    }

    private void
    checkInvalidKeyBitSize(U128 u, int prefixBit) {
        try {
            u.prefixBits(prefixBit);
            fail("Expected exception not thrown for "+prefixBit + " bits");
        } catch(IllegalArgumentException e) {
            // expected
        }
    }

    public static class Triple {
        U128 a, b, c;

        public Triple(U128 a, U128 b, U128 c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public static Triple of(U128 a, U128 b, U128 c) {
            return new Triple(a, b, c);
        }

        public String msg(String string) {
            return MessageFormat.format(string, a,b,c);
        }
    }

    @Before
    public void setup() {
        U128 u0_0 = U128.of(0, 0);
        U128 u0_1 = U128.of(0, 1);
        U128 u1_0 = U128.of(1, 0);
        U128 u1_1 = U128.of(1, 1);

        U128 u0_2 = U128.of(0, 2);
        U128 u2_0 = U128.of(2, 0);

        U128 u0_f = U128.of(0, -1L);
        U128 uf_0 = U128.of(-1L, 0);

        triples = new Triple[] {
              Triple.of(u0_0, u0_0, u0_0),
              Triple.of(u0_0, u0_1, u0_1),
              Triple.of(u0_0, u1_0, u1_0),
              Triple.of(u0_1, u1_0, u1_1),

              Triple.of(u0_1, u0_1, u0_2),
              Triple.of(u1_0, u1_0, u2_0),

              Triple.of(u0_1, u0_f, u1_0),

              Triple.of(u0_1, u0_f, u1_0),
              Triple.of(u0_f, u0_f, U128.of(1, 0xffff_ffff_ffff_fffeL)),
              Triple.of(uf_0, u0_f, U128.of(-1, -1)),
              Triple.of(uf_0, u1_0, U128.ZERO),

              Triple.of(U128.of(0x1234_5678_9abc_def1L, 0x1234_5678_9abc_def1L),
                        U128.of(0xedcb_a987_6543_210eL, 0xedcb_a987_6543_210fL),
                        U128.ZERO)
        };
    }

    @Test
    public void testAddSubtract() {
        for(Triple t: triples) {
            assertThat(t.msg("{0} + {1} = {2}"), t.a.add(t.b), equalTo(t.c));
            assertThat(t.msg("{1} + {0} = {2}"), t.b.add(t.a), equalTo(t.c));

            assertThat(t.msg("{2} - {0} = {1}"), t.c.subtract(t.a), equalTo(t.b));
            assertThat(t.msg("{2} - {1} = {0}"), t.c.subtract(t.b), equalTo(t.a));
        }
    }

    @Test
    public void testAddSubtractBuilder() {
        for(Triple t: triples) {
            assertThat(t.msg("{0} + {1} = {2}"), t.a.builder().add(t.b).build(), equalTo(t.c));
            assertThat(t.msg("{1} + {0} = {2}"), t.b.builder().add(t.a).build(), equalTo(t.c));

            assertThat(t.msg("{2} - {0} = {1}"), t.c.builder().subtract(t.a).build(), equalTo(t.b));
            assertThat(t.msg("{2} - {1} = {0}"), t.c.builder().subtract(t.b).build(), equalTo(t.a));
        }
    }

    @Test
    public void testCompare() {
        U128 u0_0 = U128.of(0, 0);
        U128 u0_1 = U128.of(0, 1);
        U128 u0_8 = U128.of(0, 0x8765_4321_8765_4321L);
        U128 u1_0 = U128.of(0x1234_5678_1234_5678L, 0);
        U128 u8_0 = U128.of(0x8765_4321_8765_4321L, 0);
        U128 uf_0 = U128.of(0xFFFF_FFFF_FFFF_FFFFL, 0);

        U128[] us = new U128[] { u0_0, u0_1, u0_8, u1_0, u8_0, uf_0 };

        for(int i = 0; i< us.length; i++) {
            U128 u_base = us[i];
            assertThat(
                    String.format("%s should be equal to itself (compareTo)", u_base),
                    u_base.compareTo(u_base), equalTo(0));
            assertThat(
                    String.format("%s should be equal to itself (equals)", u_base),
                    u_base.equals(u_base), equalTo(true));
            assertThat(
                    String.format("%s should be equal to itself (equals, by value)", u_base),
                    u_base.equals(U128.of(u_base.getMsb(), u_base.getLsb())), equalTo(true));

            for(int j = i+1; j< us.length; j++) {
                U128 u_greater = us[j];
                assertThat(
                        String.format("%s should not be equal to %s", u_base, u_greater),
                        u_base.equals(u_base), equalTo(true));
                assertThat(
                        String.format("%s should be smaller than %s", u_base, u_greater),
                        u_base.compareTo(u_greater), Matchers.lessThan(0));
                assertThat(
                        String.format("%s should be greater than %s", u_greater, u_base),
                        u_greater.compareTo(u_base), Matchers.greaterThan(0));
            }
        }
    }

    @Test
    public void testBitwiseOperators() {
        U128 one =   U128.of(0x5, 0x8);
        U128 two = U128.of(0x7, 0x3);

        assertThat(one.inverse(), equalTo(U128.of(0xfffffffffffffffaL, 0xfffffffffffffff7L)));
        assertThat(one.and(two), equalTo(U128.of(0x5L, 0x0L)));
        assertThat(one.or(two), equalTo(U128.of(0x7L, 0xbL)));
        assertThat(one.xor(two), equalTo(U128.of(0x2L, 0xbL)));
    }

    @Test
    public void testBitwiseOperatorsBuilder() {
        U128 one =   U128.of(0x5, 0x8);
        U128 two = U128.of(0x7, 0x3);

        assertThat(one.builder().invert().build(), equalTo(U128.of(0xfffffffffffffffaL, 0xfffffffffffffff7L)));
        assertThat(one.builder().and(two).build(), equalTo(U128.of(0x5L, 0x0L)));
        assertThat(one.builder().or(two).build(), equalTo(U128.of(0x7L, 0xbL)));
        assertThat(one.builder().xor(two).build(), equalTo(U128.of(0x2L, 0xbL)));
    }

}
