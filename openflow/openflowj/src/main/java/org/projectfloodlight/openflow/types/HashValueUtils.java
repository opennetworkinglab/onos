package org.projectfloodlight.openflow.types;

import com.google.common.base.Preconditions;

public class HashValueUtils {
    private HashValueUtils() { }

    public static long combineWithValue(long key, long value, int keyBits) {
        Preconditions.checkArgument(keyBits >= 0 && keyBits <= 64, "keyBits must be [0,64]");

        int valueBits = 64 - keyBits;
        long valueMask = valueBits == 64 ? 0xFFFFFFFFFFFFFFFFL : (1L << valueBits) - 1;

        return key ^ (value & valueMask);
    }

    public static int prefixBits(long raw1, int numBits) {
        Preconditions.checkArgument(numBits >= 0 && numBits <= 32,
                "numBits must be in range [0, 32]");

        if(numBits == 0)
            return 0;

        final int shiftDown = 64 - numBits;

        return (int) (raw1 >>> shiftDown);
    }

}
