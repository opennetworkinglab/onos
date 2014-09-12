package org.onlab.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test of the miscellaneous tools.
 */
public class ToolsTest {

    @Test
    public void fromHex() throws Exception {
        assertEquals(15, Tools.fromHex("0f"));
        assertEquals(16, Tools.fromHex("10"));
        assertEquals(65535, Tools.fromHex("ffff"));
        assertEquals(4096, Tools.fromHex("1000"));
        assertEquals(0xffffffffffffffffL, Tools.fromHex("ffffffffffffffff"));
    }

    @Test
    public void toHex() throws Exception {
        assertEquals("0f", Tools.toHex(15, 2));
        assertEquals("ffff", Tools.toHex(65535, 4));
        assertEquals("1000", Tools.toHex(4096, 4));
        assertEquals("000000000000000f", Tools.toHex(15));
        assertEquals("ffffffffffffffff", Tools.toHex(0xffffffffffffffffL));

    }
}
