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

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.Path;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.NetTestTools.createLambda;
import static org.onosproject.net.NetTestTools.createPath;

public class OpticalPathIntentTest extends AbstractIntentTest {

    static final int PRIORITY = 777;

    OpticalPathIntent intent1;
    OpticalPathIntent intent2;
    Path defaultPath;

    @Before
    public void opticalPathIntentTestSetUp() {
        defaultPath = createPath("a", "b", "c");
        intent1 = OpticalPathIntent.builder()
                .appId(APP_ID)
                .src(connectPoint("one", 1))
                .dst(connectPoint("two", 2))
                .path(defaultPath)
                .lambda(createLambda())
                .signalType(OchSignalType.FIXED_GRID)
                .priority(PRIORITY)
                .build();

        intent2 = OpticalPathIntent.builder()
                .appId(APP_ID)
                .src(connectPoint("two", 1))
                .dst(connectPoint("one", 2))
                .path(defaultPath)
                .lambda(createLambda())
                .signalType(OchSignalType.FIXED_GRID)
                .priority(PRIORITY)
                .build();
    }

    /**
     * Checks that the OpticalPathIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(OpticalPathIntent.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(intent1)
                .addEqualityGroup(intent2)
                .testEquals();
    }

    /**
     * Checks that the optical path intent objects are created correctly.
     */
    @Test
    public void testContents() {
        assertThat(intent1.appId(), equalTo(APP_ID));
        assertThat(intent1.src(), Matchers.equalTo(connectPoint("one", 1)));
        assertThat(intent1.dst(), Matchers.equalTo(connectPoint("two", 2)));
        assertThat(intent1.priority(), is(PRIORITY));
        assertThat(intent1.path(), is(defaultPath));
    }
}
