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
package org.onosproject.lisp.msg.protocols;

import java.util.List;

/**
 * LISP map record section which is part of LISP map register message.
 */
public interface LispMapRecord extends LispRecord {

    /**
     * Obtains locator count value.
     *
     * @return locator count value
     */
    int getLocatorCount();

    /**
     * Obtains a collection of locators.
     *
     * @return a collection of locators
     */
    List<LispLocator> getLocators();

    /**
     * A builder of LISP map record.
     */
    interface MapRecordBuilder extends RecordBuilder<MapRecordBuilder> {

        /**
         * Sets a collection of locators.
         *
         * @param locators a collection of locators
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withLocators(List<LispLocator> locators);

        /**
         * Builds LISP map record object.
         *
         * @return LISP map record object
         */
        LispMapRecord build();
    }
}
