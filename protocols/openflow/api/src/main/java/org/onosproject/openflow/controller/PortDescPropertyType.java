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
package org.onosproject.openflow.controller;

/**
 * Port description property types (OFPPDPT enums) in OF 1.3 &lt;.
 */
public enum PortDescPropertyType {
    ETHERNET(0),            /* Ethernet port */
    OPTICAL(1),             /* Optical port */
    OPTICAL_TRANSPORT(2),   /* OF1.3 Optical transport extension */
    PIPELINE_INPUT(2),      /* Ingress pipeline */
    PIPELINE_OUTPUT(3),     /* Egress pipeline */
    RECIRCULATE(4),         /* Recirculation */
    EXPERIMENTER(0xffff);   /* Experimenter-implemented */

    private final int value;

    PortDescPropertyType(int v) {
        value = v;
    }

    public int valueOf() {
        return value;
    }
}
