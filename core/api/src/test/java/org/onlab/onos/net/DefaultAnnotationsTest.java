package org.onlab.onos.net;

import org.junit.Test;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.*;
import static org.onlab.onos.net.DefaultAnnotations.builder;

/**
 * Tests of the default annotations.
 */
public class DefaultAnnotationsTest {

    private DefaultAnnotations annotations;

    @Test
    public void basics() {
        annotations = builder().set("foo", "1").set("bar", "2").build();
        assertEquals("incorrect keys", of("foo", "bar"), annotations.keys());
        assertEquals("incorrect value", "1", annotations.value("foo"));
        assertEquals("incorrect value", "2", annotations.value("bar"));
    }

    @Test
    public void empty() {
        annotations = builder().build();
        assertTrue("incorrect keys", annotations.keys().isEmpty());
    }

    @Test
    public void remove() {
        annotations = builder().remove("foo").set("bar", "2").build();
        assertEquals("incorrect keys", of("foo", "bar"), annotations.keys());
        assertNull("incorrect value", annotations.value("foo"));
        assertEquals("incorrect value", "2", annotations.value("bar"));
    }

    @Test
    public void merge() {
        annotations = builder().set("foo", "1").set("bar", "2").build();
        assertEquals("incorrect keys", of("foo", "bar"), annotations.keys());

        SparseAnnotations updates = builder().remove("foo").set("bar", "3").set("goo", "4").build();

        annotations = DefaultAnnotations.merge(annotations, updates);
        assertEquals("incorrect keys", of("goo", "bar"), annotations.keys());
        assertNull("incorrect value", annotations.value("foo"));
        assertEquals("incorrect value", "3", annotations.value("bar"));
    }

    @Test
    public void noopMerge() {
        annotations = builder().set("foo", "1").set("bar", "2").build();
        assertEquals("incorrect keys", of("foo", "bar"), annotations.keys());

        SparseAnnotations updates = builder().build();
        assertSame("same annotations expected", annotations,
                   DefaultAnnotations.merge(annotations, updates));
        assertSame("same annotations expected", annotations,
                   DefaultAnnotations.merge(annotations, null));
    }

    @Test(expected = NullPointerException.class)
    public void badMerge() {
        DefaultAnnotations.merge(null, null);
    }

}