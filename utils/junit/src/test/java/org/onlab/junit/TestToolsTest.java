package org.onlab.junit;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;

public class TestToolsTest {

    @Test
    public void testSuccess() {
        assertAfter(10, 100, new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ < 3) {
                    assertTrue(false);
                }
            }
        });
    }

    @Test(expected = AssertionError.class)
    public void testFailure() {
        assertAfter(100, new Runnable() {
            @Override
            public void run() {
                assertTrue(false);
            }
        });
    }
}
