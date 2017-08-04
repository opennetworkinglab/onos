/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onlab.packet.ipv6;

/**
 * Interface for IPv6 extension header.
 */
public interface IExtensionHeader {
    /**
     * Gets the type of next header.
     *
     * @return next header
     */
    byte getNextHeader();

    /**
     * Sets the type of next header.
     *
     * @param nextHeader the next header to set
     * @return this
     */
    IExtensionHeader setNextHeader(final byte nextHeader);
}
