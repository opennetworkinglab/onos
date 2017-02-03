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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;

/**
 * Represents the set of errors possible when processing an objective.
 */
@Beta
public enum ObjectiveError {

    /**
     * The driver processing this objective does not know how to process it.
     */
    UNSUPPORTED,

    /**
     * The flow installation for this objective failed.
     */
    FLOWINSTALLATIONFAILED,

    /**
     * The group installation for this objective failed.
     */
    GROUPINSTALLATIONFAILED,

    /**
     * The group removal for this objective failed.
     */
    GROUPREMOVALFAILED,

    /**
     * The group was reported as installed but is missing.
     */
    GROUPMISSING,

    /**
     * The device was not available to install objectives to.
     */
    DEVICEMISSING,

    /**
     * Incorrect Objective parameters passed in by the caller.
     */
    BADPARAMS,

    /**
     * The device has no pipeline driver to install objectives.
     */
    NOPIPELINER,

    /**
     * An unknown error occurred.
     */
    UNKNOWN,

    /**
     * Flow/Group installation retry threshold exceeded.
     */
    INSTALLATIONTHRESHOLDEXCEEDED,

    /**
     * Installation timeout.
     */
    INSTALLATIONTIMEOUT,

    /**
     * Group already exists.
     */
    GROUPEXISTS
}
