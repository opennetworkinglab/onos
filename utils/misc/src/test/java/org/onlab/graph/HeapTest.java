package org.onlab.graph;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Heap data structure tests.
 */
public class HeapTest {

    private ArrayList<Integer> data =
            new ArrayList<>(ImmutableList.of(6, 4, 5, 9, 8, 3, 2, 1, 7, 0));

    private static final Comparator<Integer> ASCENDING = Ordering.natural().reverse();
    private static final Comparator<Integer> DESCENDING = Ordering.natural();

    @Test
    public void equality() {
        new EqualsTester()
                .addEqualityGroup(new Heap<>(data, ASCENDING),
                                  new Heap<>(data, ASCENDING))
                .addEqualityGroup(new Heap<>(data, DESCENDING))
                .testEquals();
    }

    @Test
    public void ascending() {
        Heap<Integer> h = new Heap<>(data, ASCENDING);
        assertEquals("incorrect size", 10, h.size());
        assertFalse("should not be empty", h.isEmpty());
        assertEquals("incorrect extreme", (Integer) 0, h.extreme());

        for (int i = 0, n = h.size(); i < n; i++) {
            assertEquals("incorrect element", (Integer) i, h.extractExtreme());
        }
        assertTrue("should be empty", h.isEmpty());
    }

    @Test
    public void descending() {
        Heap<Integer> h = new Heap<>(data, DESCENDING);
        assertEquals("incorrect size", 10, h.size());
        assertFalse("should not be empty", h.isEmpty());
        assertEquals("incorrect extreme", (Integer) 9, h.extreme());

        for (int i = h.size(); i > 0; i--) {
            assertEquals("incorrect element", (Integer) (i - 1), h.extractExtreme());
        }
        assertTrue("should be empty", h.isEmpty());
    }

    @Test
    public void empty() {
        Heap<Integer> h = new Heap<>(new ArrayList<Integer>(), ASCENDING);
        assertEquals("incorrect size", 0, h.size());
        assertTrue("should be empty", h.isEmpty());
        assertNull("no item expected", h.extreme());
        assertNull("no item expected", h.extractExtreme());
    }

    @Test
    public void insert() {
        Heap<Integer> h = new Heap<>(data, ASCENDING);
        assertEquals("incorrect size", 10, h.size());
        h.insert(3);
        assertEquals("incorrect size", 11, h.size());
    }

    @Test
    public void iterator() {
        Heap<Integer> h = new Heap<>(data, ASCENDING);
        Iterator<Integer> it = h.iterator();
        while (it.hasNext()) {
            int item = it.next();
        }
    }

}
