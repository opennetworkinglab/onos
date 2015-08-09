package org.onosproject.store.consistent.impl;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import com.google.common.base.Objects;

/**
 * Unit tests for Match.
 */
public class MatchTest {

    @Test
    public void testMatches() {
        Match<String> m1 = Match.any();
        assertTrue(m1.matches(null));
        assertTrue(m1.matches("foo"));
        assertTrue(m1.matches("bar"));

        Match<String> m2 = Match.ifNull();
        assertTrue(m2.matches(null));
        assertFalse(m2.matches("foo"));

        Match<String> m3 = Match.ifValue("foo");
        assertFalse(m3.matches(null));
        assertFalse(m3.matches("bar"));
        assertTrue(m3.matches("foo"));
    }

    @Test
    public void testEquals() {
        Match<String> m1 = Match.any();
        Match<String> m2 = Match.any();
        Match<String> m3 = Match.ifNull();
        Match<String> m4 = Match.ifValue("bar");
        assertEquals(m1, m2);
        assertFalse(Objects.equal(m1, m3));
        assertFalse(Objects.equal(m3, m4));
    }

    @Test
    public void testMap() {
        Match<String> m1 = Match.ifNull();
        assertEquals(m1.map(s -> "bar"), Match.ifNull());
        Match<String> m2 = Match.ifValue("foo");
        Match<String> m3 = m2.map(s -> "bar");
        assertTrue(m3.matches("bar"));
    }
}
