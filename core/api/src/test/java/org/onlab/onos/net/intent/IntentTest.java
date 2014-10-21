package org.onlab.onos.net.intent;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Base facilities to test various intent tests.
 */
public abstract class IntentTest {
    /**
     * Produces a set of items from the supplied items.
     *
     * @param items items to be placed in set
     * @param <T>   item type
     * @return set of items
     */
    protected static <T> Set<T> itemSet(T[] items) {
        return new HashSet<>(Arrays.asList(items));
    }

    @Test
    public void equalsAndHashCode() {
        Intent one = createOne();
        Intent like = createOne();
        Intent another = createAnother();

        assertTrue("should be equal", one.equals(like));
        assertEquals("incorrect hashCode", one.hashCode(), like.hashCode());
        assertFalse("should not be equal", one.equals(another));
    }

    @Test
    public void testToString() {
        Intent one = createOne();
        Intent like = createOne();
        assertEquals("incorrect toString", one.toString(), like.toString());
    }

    /**
     * Creates a new intent, but always a like intent, i.e. all instances will
     * be equal, but should not be the same.
     *
     * @return intent
     */
    protected abstract Intent createOne();

    /**
     * Creates another intent, not equals to the one created by
     * {@link #createOne()} and with a different hash code.
     *
     * @return another intent
     */
    protected abstract Intent createAnother();
}
