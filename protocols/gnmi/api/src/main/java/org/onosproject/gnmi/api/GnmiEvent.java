/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.gnmi.api;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Representation of an event received from a gNMI device.
 */
@Beta
public final class GnmiEvent extends AbstractEvent<GnmiEvent.Type, GnmiEventSubject> {

    /**
     * Type of gNMI event.
     */
    public enum Type {
        /**
         * Update.
         */
        UPDATE
    }

    public GnmiEvent(Type type, GnmiEventSubject subject) {
        super(type, subject);
    }
}
