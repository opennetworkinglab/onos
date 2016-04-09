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
package org.onosproject.net;

import org.onlab.util.Identifier;

/*
 * Representation of NSH Service path Identifier
 */
public final class NshServicePathId extends Identifier<Integer> {

    /**
     * Default constructor.
     *
     * @param servicePathId nsh service path identifier
     */
    private NshServicePathId(int servicePathId) {
        super(servicePathId);
    }

    /**
     * Returns the NshServicePathId by setting its value.
     *
     * @param servicePathId nsh service path identifier
     * @return NshServicePathId
     */
    public static NshServicePathId of(int servicePathId) {
        return new NshServicePathId(servicePathId);
    }

    /**
     * Returns nsh context service path identifier.
     *
     * @return the nsh context service path id
     */
    public int servicePathId() {
        return identifier;
    }
}

