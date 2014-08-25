package org.projectfloodlight.openflow.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.text.MessageFormat;

import org.junit.Before;
import org.junit.Test;

public class U64Test {

    private Triple[] triples;

    @Test
    public void testPositiveRaws() {
        for(long positive: new long[] { 0, 1, 100, Long.MAX_VALUE }) {
            assertEquals(positive, U64.ofRaw(positive).getValue());
            assertEquals(BigInteger.valueOf(positive), U64.ofRaw(positive).getBigInteger());
        }
    }

    @Test
    public void testNegativeRaws() {
        long minu1 = 0xFFFF_FFFF_FFFF_FFFFL;
        assertEquals(minu1, U64.ofRaw(minu1).getValue());
        assertEquals(new BigInteger("FFFF_FFFF_FFFF_FFFF".replace("_", ""), 16),  U64.ofRaw(minu1).getBigInteger());
        assertEquals(new BigInteger("18446744073709551615"),  U64.ofRaw(minu1).getBigInteger());
    }

    @Test
    public void testEqualHashCode() {
        U64 h1 = U64.of(0xdeafbeefdeadbeefL);
        U64 h2 = U64.of(0xdeafbeefdeadbeefL);
        U64 h3 = U64.of(0xeeafbeefdeadbeefL);

        assertTrue(h1.equals(h1));
        assertTrue(h1.equals(h2));
        assertFalse(h1.equals(h3));
        assertTrue(h2.equals(h1));

        assertEquals(h1.hashCode(), h2.hashCode());
        assertNotEquals(h1.hashCode(), h3.hashCode()); // not technically a requirement, but we'll hopefully be lucky.
    }

    @Test
    public void testXor() {
        U64 hNull = U64.of(0);
        U64 hDeadBeef = U64.of(0xdeafbeefdeadbeefL);
        assertThat(hNull.xor(hNull), equalTo(hNull));
        assertThat(hNull.xor(hDeadBeef), equalTo(hDeadBeef));
        assertThat(hDeadBeef.xor(hNull), equalTo(hDeadBeef));
        assertThat(hDeadBeef.xor(hDeadBeef), equalTo(hNull));


        U64 h1 = U64.of(1L);
        U64 h8 = U64.of(0x8000000000000000L);
        U64 h81 = U64.of(0x8000000000000001L);
        assertThat(h1.xor(h8), equalTo(h81));
    }

    @Test
    public void testKeyBits() {
        U64 zeroU = U64.of(0);
        assertThat(zeroU.prefixBits(0), equalTo(0));
        assertThat(zeroU.prefixBits(16), equalTo(0));
        assertThat(zeroU.prefixBits(32), equalTo(0));

        checkInvalidKeyBitSize(zeroU, 33);
        checkInvalidKeyBitSize(zeroU, 64);
        assertThat(zeroU.prefixBits(3), equalTo(0));

        U64 positiveU = U64.of(0x1234_5678_1234_5678L);
        assertThat(positiveU.prefixBits(0), equalTo(0));
        assertThat(positiveU.prefixBits(16), equalTo(0x1234));
        assertThat(positiveU.prefixBits(32), equalTo(0x12345678));
        checkInvalidKeyBitSize(positiveU, 33);
        checkInvalidKeyBitSize(positiveU, 64);

        U64 signedBitU = U64.of(0x8765_4321_8765_4321L);
        assertThat(signedBitU.prefixBits(0), equalTo(0));
        assertThat(signedBitU.prefixBits(16), equalTo(0x8765));
        assertThat(signedBitU.prefixBits(32), equalTo(0x8765_4321));
        checkInvalidKeyBitSize(signedBitU, 33);
        checkInvalidKeyBitSize(signedBitU, 64);
    }

    public static class Triple {
        U64 a, b, c;

        public Triple(U64 a, U64 b, U64 c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public static Triple of(U64 a, U64 b, U64 c) {
            return new Triple(a, b, c);
        }

        public String msg(String string) {
            return MessageFormat.format(string, a,b,c);
        }
    }

    @Before
    public void setup() {
        U64 u0 = U64.of(0);
        U64 u1 = U64.of(1);

        U64 u2 = U64.of(2);
        U64 u7f = U64.of(0x7fff_ffff_ffff_ffffL);
        U64 u8 = U64.of(0x8000_0000_0000_0000L);

        U64 uf = U64.of(-1L);

        triples = new Triple[] {
              Triple.of(u0, u0, u0),
              Triple.of(u0, u1, u1),

              Triple.of(u1, u1, u2),

              Triple.of(u1, uf, u0),

              Triple.of(uf, uf, U64.of(0xffff_ffff_ffff_fffeL)),
              Triple.of(u0, uf, uf),

              Triple.of(u7f, u1, u8),

              Triple.of(U64.of(0x1234_5678_9abc_def1L),
                        U64.of(0xedcb_a987_6543_210fL),
                        U64.ZERO)
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

    private void
            checkInvalidKeyBitSize(U64 u, int prefixBit) {
        try {
            u.prefixBits(prefixBit);
            fail("Expected exception not thrown for "+prefixBit + " bits");
        } catch(IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testBitwiseOperators() {
        U64 notPi = U64.of(0x3141_5926_5358_9793L);
        U64 notE =  U64.of(0x2718_2818_8459_4523L);

        assertThat(notPi.inverse(), equalTo(U64.of(0xcebe_a6d9_aca7_686cL)));
        assertThat(notPi.and(notE), equalTo(U64.of(0x2100_0800_0058_0503L)));
        assertThat(notPi.or(notE),  equalTo(U64.of(0x3759_793e_d759_d7b3L)));
        assertThat(notPi.xor(notE), equalTo(U64.of(0x1659_713e_d701_d2b0L)));
    }

    @Test
    public void testBitwiseOperatorsBuilder() {
        U64 notPi = U64.of(0x3141_5926_5358_9793L);
        U64 notE =  U64.of(0x2718_2818_8459_4523L);

        assertThat(notPi.builder().invert().build(), equalTo(U64.of(0xcebe_a6d9_aca7_686cL)));
        assertThat(notPi.builder().and(notE).build(), equalTo(U64.of(0x2100_0800_0058_0503L)));
        assertThat(notPi.builder().or(notE).build(),  equalTo(U64.of(0x3759_793e_d759_d7b3L)));
        assertThat(notPi.builder().xor(notE).build(), equalTo(U64.of(0x1659_713e_d701_d2b0L)));
    }
}
