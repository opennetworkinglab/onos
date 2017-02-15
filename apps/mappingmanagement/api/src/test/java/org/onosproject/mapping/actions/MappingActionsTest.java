/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping.actions;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.junit.UtilityClassChecker.assertThatClassIsUtility;

/**
 * Unit tests for various mapping action implementation classes.
 */
public class MappingActionsTest {

    /**
     * Checks that a MappingAction object has the proper type, and then converts
     * it to the proper type.
     *
     * @param action MappingAction object to convert
     * @param type   Enumerated type value for the MappingAction class
     * @param clazz  Desired MappingAction class
     * @param <T>    The type the caller wants returned
     * @return converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T checkAndConvert(MappingAction action, MappingAction.Type type, Class clazz) {
        assertThat(action, is(notNullValue()));
        assertThat(action.type(), is(equalTo(type)));
        assertThat(action, instanceOf(clazz));
        return (T) action;
    }

    /**
     * Checks that the MappingActions class is a valid utility class.
     */
    @Test
    public void testMappingActionsUtility() {
        assertThatClassIsUtility(MappingActions.class);
    }

    /**
     * Checks that the mapping action implementations are immutable.
     */
    @Test
    public void testMappingActionsImmutability() {
        assertThatClassIsImmutable(NoMappingAction.class);
        assertThatClassIsImmutable(ForwardMappingAction.class);
        assertThatClassIsImmutable(NativeForwardMappingAction.class);
        assertThatClassIsImmutable(DropMappingAction.class);
    }

    /**
     * Tests the noAction method.
     */
    @Test
    public void testNoActionMethod() {
        MappingAction mappingAction = MappingActions.noAction();
        checkAndConvert(mappingAction,
                        MappingAction.Type.NO_ACTION,
                        NoMappingAction.class);
    }

    /**
     * Tests the forward method.
     */
    @Test
    public void testForwardMethod() {
        MappingAction mappingAction = MappingActions.forward();
        checkAndConvert(mappingAction,
                        MappingAction.Type.FORWARD,
                        ForwardMappingAction.class);
    }

    /**
     * Tests the native forward method.
     */
    @Test
    public void testNativeForwardMethod() {
        MappingAction mappingAction = MappingActions.nativeForward();
        checkAndConvert(mappingAction,
                        MappingAction.Type.NATIVE_FORWARD,
                        NativeForwardMappingAction.class);
    }

    /**
     * Tests the drop method.
     */
    @Test
    public void testDropMethod() {
        MappingAction mappingAction = MappingActions.drop();
        checkAndConvert(mappingAction,
                        MappingAction.Type.DROP,
                        DropMappingAction.class);
    }
}
