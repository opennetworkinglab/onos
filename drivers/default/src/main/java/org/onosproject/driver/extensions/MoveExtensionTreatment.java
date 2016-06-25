/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.driver.extensions;

import org.onosproject.net.flow.instructions.ExtensionTreatment;

/**
 * The abstraction of Move Treatment.
 */
public interface MoveExtensionTreatment extends ExtensionTreatment {

    /**
     * Returns SRC_OFS field of move extension action.
     *
     * @return SRC_OFS
     */
    int srcOffset();

    /**
     * Returns DST_OFS field of move extension action.
     *
     * @return DST_OFS
     */
    int dstOffset();

    /**
     * Returns SRC field of move extension action.
     *
     * @return SRC
     */
    int src();

    /**
     * Returns DST field of move extension action.
     *
     * @return DST
     */
    int dst();

    /**
     * Returns N_BITS field of move extension action.
     *
     * @return N_BITS
     */
    int nBits();
}
