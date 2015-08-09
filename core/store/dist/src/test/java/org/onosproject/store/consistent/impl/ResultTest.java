package org.onosproject.store.consistent.impl;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

/**
 * Unit tests for Result.
 */
public class ResultTest {

    @Test
    public void testLocked() {
        Result<String> r = Result.locked();
        assertFalse(r.success());
        assertNull(r.value());
        assertEquals(Result.Status.LOCKED, r.status());
    }

    @Test
    public void testOk() {
        Result<String> r = Result.ok("foo");
        assertTrue(r.success());
        assertEquals("foo", r.value());
        assertEquals(Result.Status.OK, r.status());
    }

    @Test
    public void testEquality() {
        Result<String> r1 = Result.ok("foo");
        Result<String> r2 = Result.locked();
        Result<String> r3 = Result.ok("bar");
        Result<String> r4 = Result.ok("foo");
        assertTrue(r1.equals(r4));
        assertFalse(r1.equals(r2));
        assertFalse(r1.equals(r3));
        assertFalse(r2.equals(r3));
    }
}
