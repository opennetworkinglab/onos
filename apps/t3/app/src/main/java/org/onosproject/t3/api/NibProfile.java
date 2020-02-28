/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import java.text.SimpleDateFormat;

/**
 * Basic descriptions about a single Network information Base (NIB) instance.
 */
public class NibProfile {
    // default false means the contents of this NIB are empty at the instantiation
    private boolean valid = false;
    private String date;
    private SourceType sourceType;

    public enum SourceType {
        /**
         * Provided by dump files.
         */
        FILE,
        /**
         * Provided by a running system.
         */
        SNAPSHOT
    }

    public NibProfile(long date, SourceType sourceType) {
        this.valid = true;
        this.date = new SimpleDateFormat("dd-MM-yyyy hh:mm").format(date);
        this.sourceType = sourceType;
    }

    /**
     * Returns the validity state of this NIB.
     *
     * @return true once this profile is initialized
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the time this NIB has been filled.
     *
     * @return string representation for the time
     */
    public String date() {
        return date;
    }

    /**
     * Returns the type of the source used to fill this NIB.
     *
     * @return source type
     */
    public SourceType sourceType() {
        return sourceType;
    }

}
