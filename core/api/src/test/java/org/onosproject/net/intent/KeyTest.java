/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.intent;

import org.junit.Test;
import org.onosproject.net.NetTestTools;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the intent key class.
 */
public class KeyTest {

    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";
    private static final String KEY_3 = "key3";

    private static final long LONG_KEY_1 = 0x1111;
    private static final long LONG_KEY_2 = 0x2222;
    private static final long LONG_KEY_3 = 0x3333;

    /**
     * Tests that keys are properly immutable.
     */
    @Test
    public void keysAreImmutable() {
        assertThatClassIsImmutableBaseClass(Key.class);

        // Will be a long based key, class is private so cannot be
        // accessed directly
        Key longKey = Key.of(0xabcdefL, NetTestTools.APP_ID);
        assertThatClassIsImmutable(longKey.getClass());

        // Will be a String based key, class is private so cannot be
        // accessed directly.
        Key stringKey = Key.of("some key", NetTestTools.APP_ID);
        assertThatClassIsImmutable(stringKey.getClass());
    }

    /**
     * Tests string key construction.
     */
    @Test
    public void stringKeyConstruction() {
        Key stringKey1 = Key.of(KEY_3, NetTestTools.APP_ID);
        assertThat(stringKey1, notNullValue());
        Key stringKey2 = Key.of(KEY_3, NetTestTools.APP_ID);
        assertThat(stringKey2, notNullValue());

        assertThat(stringKey1.hash(), is(stringKey2.hash()));
    }

    /**
     * Tests long key construction.
     */
    @Test
    public void longKeyConstruction() {
        Key longKey1 = Key.of(LONG_KEY_3, NetTestTools.APP_ID);
        assertThat(longKey1, notNullValue());
        Key longKey2 = Key.of(LONG_KEY_3, NetTestTools.APP_ID);
        assertThat(longKey2, notNullValue());

        assertThat(longKey1.hash(), is(longKey2.hash()));
    }

    /**
     * Tests equals for string based keys.
     */
    @Test
    public void stringKey() {
        Key stringKey1 = Key.of(KEY_1, NetTestTools.APP_ID);
        Key copyOfStringKey1 = Key.of(KEY_1, NetTestTools.APP_ID);
        Key stringKey2 = Key.of(KEY_2, NetTestTools.APP_ID);
        Key copyOfStringKey2 = Key.of(KEY_2, NetTestTools.APP_ID);
        Key stringKey3 = Key.of(KEY_3, NetTestTools.APP_ID);

        new EqualsTester()
                .addEqualityGroup(stringKey1, copyOfStringKey1)
                .addEqualityGroup(stringKey2, copyOfStringKey2)
                .addEqualityGroup(stringKey3)
                .testEquals();
    }

    /**
     * Tests equals for long based keys.
     */
    @Test
    public void longKey() {
        Key longKey1 = Key.of(LONG_KEY_1, NetTestTools.APP_ID);
        Key copyOfLongKey1 = Key.of(LONG_KEY_1, NetTestTools.APP_ID);
        Key longKey2 = Key.of(LONG_KEY_2, NetTestTools.APP_ID);
        Key copyOfLongKey2 = Key.of(LONG_KEY_2, NetTestTools.APP_ID);
        Key longKey3 = Key.of(LONG_KEY_3, NetTestTools.APP_ID);

        new EqualsTester()
                .addEqualityGroup(longKey1, copyOfLongKey1)
                .addEqualityGroup(longKey2, copyOfLongKey2)
                .addEqualityGroup(longKey3)
                .testEquals();
    }
}
