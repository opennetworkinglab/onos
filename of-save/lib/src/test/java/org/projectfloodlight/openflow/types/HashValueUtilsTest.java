package org.projectfloodlight.openflow.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HashValueUtilsTest {
    @Test
    public void testBasic() {
        long key =        0x1234_5678_1234_5678L;
        long value =      0x8765_4321_8765_4321L;
        long firstword  = 0xFFFF_FFFF_0000_0000L;
        long secondword = 0x0000_0000_FFFF_FFFFL;
        long xor =        key ^ value;

        assertThat(HashValueUtils.combineWithValue(key, value, 0), equalTo(xor));
        assertThat(HashValueUtils.combineWithValue(key, value, 64), equalTo(key));
        assertThat(HashValueUtils.combineWithValue(key, value, 32), equalTo(key & firstword | xor & secondword ));
        assertThat(HashValueUtils.combineWithValue(key, value, 8), equalTo(0x1251_1559_9551_1559L));
    }

}
