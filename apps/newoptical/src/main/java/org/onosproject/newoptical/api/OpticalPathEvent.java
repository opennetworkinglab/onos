/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.newoptical.api;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Event related to optical domain path setup.
 */
@Beta
public class OpticalPathEvent extends AbstractEvent<OpticalPathEvent.Type, OpticalConnectivityId> {
    public enum Type {
        PATH_INSTALLED,
        PATH_REMOVED
    }

    /**
     * Creates OpticalPathEvent object with specified type and subject.
     *
     * @param type type of event
     * @param subject subject of the event
     */
    public OpticalPathEvent(Type type, OpticalConnectivityId subject) {
        super(type, subject);
    }

}
