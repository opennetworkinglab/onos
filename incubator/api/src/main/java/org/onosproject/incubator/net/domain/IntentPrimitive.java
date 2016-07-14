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
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;

/**
 * Abstract base class for intent primitives.
 */
@Beta
public abstract class IntentPrimitive {

    private final ApplicationId appId;

    public IntentPrimitive(ApplicationId appId) {
        this.appId = appId;
    }

    /**
     * The getter for the application ID associated with the intent primitive upon creation.
     *
     * @return the application ID associated with the intent primitive
     */
    public ApplicationId appId() {
        return appId;
    }
}