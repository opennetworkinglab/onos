package org.projectfloodlight.openflow.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Test;

public class U64Test {

    @Test
    public void testPositiveRaws() {
        for(long positive: new long[] { 0, 1, 100, Long.MAX_VALUE }) {
            assertEquals(positive, U64.ofRaw(positive).getValue());
            assertEquals(BigInteger.valueOf(positive), U64.ofRaw(positive).getBigInteger());
        }
    }

    @Test
    public void testNegativeRaws() {
        long minus_1 = 0xFFFF_FFFF_FFFF_FFFFL;
        assertEquals(minus_1, U64.ofRaw(minus_1).getValue());
        assertEquals(new BigInteger("FFFF_FFFF_FFFF_FFFF".replace("_", ""), 16),  U64.ofRaw(minus_1).getBigInteger());
        assertEquals(new BigInteger("18446744073709551615"),  U64.ofRaw(minus_1).getBigInteger());
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
    public void testCombine() {
        long key = 0x1234567890abcdefL;
        long val = 0xdeafbeefdeadbeefL;
        U64 hkey = U64.of(key);
        U64 hVal = U64.of(val);

        assertThat(hkey.combineWithValue(hVal, 0), equalTo(hkey.xor(hVal)));
        assertThat(hkey.combineWithValue(hVal, 64), equalTo(hkey));
        long mask32 = 0x00000000FFFFFFFFL;
        assertThat(hkey.combineWithValue(hVal, 32),
                equalTo(U64.of(key & ~mask32| (key ^ val) & mask32)));

        long tenMask = 0x003FFFFFFFFFFFFFL;
        assertThat(hkey.combineWithValue(hVal, 10),
                equalTo(U64.of(key & ~tenMask | (key ^ val) & tenMask)));
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

    private void
            checkInvalidKeyBitSize(U64 u, int prefixBit) {
        try {
            u.prefixBits(prefixBit);
            fail("Expected exception not thrown for "+prefixBit + " bits");
        } catch(IllegalArgumentException e) {
            // expected
        }
    }

}
