/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.node;

/**
 * The termination capabilities between tunnel-termination-point
 * and link termination-point.
 */
public class TerminationCapability {
    // See reference - org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang
   // .ietf.te.topology.rev20160708.ietftetopology.augmentednwnode.te
    //.tunnelterminationpoint.config.DefaultTerminationCapability
    private TerminationPointKey linkTpId;
    //List<MaxLspBandwidth> maxLspBandwidth
    // TODO - to be extended per future standard definitions

}
