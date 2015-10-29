/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.resource.link;

import java.util.Set;

/**
 * Abstraction of a resources of a link.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public interface LinkResources {

    /**
     * Returns resources as a set of {@link LinkResource}s.
     *
     * @return a set of {@link LinkResource}s
     */
    Set<LinkResource> resources();

    /**
     * Builder of {@link LinkResources}.
     *
     * @deprecated in Emu Release
     */
    @Deprecated
    interface Builder {

        /**
         * Adds bandwidth resource.
         * <p>
         * This operation adds given bandwidth to previous bandwidth and
         * generates single bandwidth resource.
         *
         * @param bandwidth bandwidth value to be added
         * @return self
         */
        Builder addBandwidth(double bandwidth);

        /**
         * Adds lambda resource.
         *
         * @param lambda lambda value to be added
         * @return self
         */
        Builder addLambda(int lambda);

        /**
         * Builds an immutable link resources.
         *
         * @return link resources
         */
        LinkResources build();
    }
}
