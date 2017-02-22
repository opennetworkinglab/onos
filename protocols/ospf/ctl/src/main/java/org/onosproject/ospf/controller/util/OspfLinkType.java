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
package org.onosproject.ospf.controller.util;

/**
 * Enum represents OSPF link type.
 */
public enum OspfLinkType {

    /**
     * Indicates a point-to-point connection to another router.
     */
    POINT_TO_POINT,

    /**
     * Indicates a connection to a transit network.
     */
    TO_TRANSIT_NET,

    /**
     * Indicates a connection to a stub network.
     */
    TO_STUB_NET,

    /**
     * Indicates a Virtual link to another area border router.
     */
    VIRTUAL_LINK
}