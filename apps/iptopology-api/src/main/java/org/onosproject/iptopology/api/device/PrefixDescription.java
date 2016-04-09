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
package org.onosproject.iptopology.api.device;

import org.onosproject.iptopology.api.PrefixIdentifier;
import org.onosproject.iptopology.api.PrefixTed;
import org.onosproject.net.Description;

/**
 * Information about a prefix.
 */
public interface PrefixDescription extends Description {

    /**
     * Returns the prefix identifier.
     *
     * @return prefix identifier
     */
    PrefixIdentifier prefixIdentifier();

    /**
     * Returns the prefix Traffic Engineering parameters.
     *
     * @return prefix Traffic Engineering parameters
     */
    PrefixTed prefixTed();

}
