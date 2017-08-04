/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.cluster.messaging;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for MessageSubject class.
 */
public class MessageSubjectTest {
    private final MessageSubject subject1 = new MessageSubject("Message 1");
    private final MessageSubject sameAsSubject1 = new MessageSubject("Message 1");
    private final MessageSubject subject2 = new MessageSubject("Message 2");
    private final MessageSubject subject3 = new MessageSubject("Message 3");

    /**
     * Checks that the MessageSubject class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(MessageSubject.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(subject1, sameAsSubject1)
                .addEqualityGroup(subject2)
                .addEqualityGroup(subject3)
                .testEquals();
    }

    /**
     * Checks the construction of a MessageSubject object.
     */
    @Test
    public void testConstruction() {
        assertThat(subject3.value(), is("Message 3"));
        MessageSubject serializerObject = new MessageSubject();
        assertThat(serializerObject.value(), is(""));
    }
}
