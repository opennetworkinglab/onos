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

/**
 * Factory class for creating various mapping actions.
 */
public final class MappingActions {

    /**
     * Prevents instantiation from external.
     */
    private MappingActions() {}

    /**
     * Creates a drop mapping action.
     *
     * @return a drop mapping action
     */
    public static DropMappingAction drop() {
        return new DropMappingAction();
    }

    /**
     * Creates a forward mapping action.
     *
     * @return a forward mapping action
     */
    public static ForwardMappingAction forward() {
        return new ForwardMappingAction();
    }

    /**
     * Creates a native forward mapping action.
     *
     * @return a native forward mapping action
     */
    public static NativeForwardMappingAction nativeForward() {
        return new NativeForwardMappingAction();
    }

    /**
     * Creates a no action.
     *
     * @return a no action
     */
    public static NoMappingAction noAction() {
        return new NoMappingAction();
    }
}
