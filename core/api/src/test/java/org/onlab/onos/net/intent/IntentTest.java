/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.onos.net.intent;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Base facilities to test various intent tests.
 */
public abstract class IntentTest extends AbstractIntentTest {
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

    @Test @Ignore("Equality is based on ids, which will be different")
    public void equalsAndHashCode() {
        Intent one = createOne();
        Intent like = createOne();
        Intent another = createAnother();

        assertTrue("should be equal", one.equals(like));
        assertEquals("incorrect hashCode", one.hashCode(), like.hashCode());
        assertFalse("should not be equal", one.equals(another));
    }

    //@Test FIXME
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
