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
package org.onosproject.iptopology.api;

import org.onosproject.net.Annotated;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Provided;

/**
 * Abstraction of a network ip link.
 */
public interface IpLink extends Annotated, Provided, NetworkResource {

    /**
     * Returns source termination point of link.
     *
     * @return source termination point of link
     */
    TerminationPoint src();

    /**
     * Returns destination termination point of link.
     *
     * @return destination termination point of link
     */
    TerminationPoint dst();

    /**
     * Returns link identifier details.
     *
     * @return link identifier details
     */
    IpLinkIdentifier linkIdentifier();

    /**
     * Returns the link traffic engineering parameters.
     *
     * @return links traffic engineering parameters
     */
    LinkTed linkTed();
}