/*
 * Copyright 2018-present Open Networking Foundation
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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.openroadm;

import org.onlab.util.Frequency;

public class OpenRoadmNmcInterface extends OpenRoadmInterface {

    protected Frequency nmcFrequency; // expressed in Thz
    protected Frequency nmcWidth;     // expressed in Ghz

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<interface>");
        sb.append("<name>" + name + "</name>");
        sb.append("<description>Network-Media-Channel</description>");
        sb.append(
            "<type xmlns:openROADM-if=\"http://org/openroadm/interfaces\">"
            +
            "openROADM-if:networkMediaChannelConnectionTerminationPoint</type>");
        sb.append("<administrative-state>inService</administrative-state>");
        sb.append("<supporting-circuit-pack-name>" + supportingCircuitPack +
                  "</supporting-circuit-pack-name>");
        sb.append("<supporting-port>" + supportingPort + "</supporting-port>");
        if (!supportingInterface.isEmpty()) {
            sb.append("<supporting-interface>" + supportingInterface + "</supporting-interface>");
        }

        sb.append("<nmc-ctp xmlns=\"http://org/openroadm/network-media-channel-interfaces\">");
        sb.append("<frequency>" + nmcFrequency.asTHz() + "</frequency>");
        sb.append("<width>" + nmcWidth.asGHz() + "</width>");
        sb.append("</nmc-ctp>");
        sb.append("</interface>");
        return sb.toString();
    }

    public abstract static class Builder<T extends Builder<T>>
        extends OpenRoadmInterface.Builder<T> {

      protected Frequency nmcFrequency; // expressed in Thz
      protected Frequency nmcWidth;     // expressed in Ghz

      public T nmcFrequency(Frequency nmcFrequency) {
        this.nmcFrequency = nmcFrequency;
        return self();
      }

      public T nmcWidth(Frequency nmcWidth) {
        this.nmcWidth = nmcWidth;
        return self();
      }
    }

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new Builder2();
    }

    protected OpenRoadmNmcInterface(Builder<?> builder) {
        super(builder);
        this.nmcFrequency = builder.nmcFrequency;
        this.nmcWidth = builder.nmcWidth;
    }
}
