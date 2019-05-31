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


public class OpenRoadmMcInterface extends OpenRoadmInterface {

    protected Frequency mcMinFrequency; // expressed in Thz
    protected Frequency mcMaxFrequency; // expressed in Thz

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<interface>");
        sb.append("<name>" + name + "</name>");
        sb.append("<description>Media-Channel</description>");
        sb.append("<type xmlns:openROADM-if=\"http://org/openroadm/interfaces\">"
                  + "openROADM-if:mediaChannelTrailTerminationPoint</type>");
        sb.append("<administrative-state>inService</administrative-state>");
        sb.append("<supporting-circuit-pack-name>" + supportingCircuitPack +
                  "</supporting-circuit-pack-name>");
        sb.append("<supporting-port>" + supportingPort + "</supporting-port>");
        if (!supportingInterface.isEmpty()) {
            sb.append("<supporting-interface>" + supportingInterface +
                      "</supporting-interface>");
        }
        sb.append("<mc-ttp xmlns=\"http://org/openroadm/media-channel-interfaces\">");
        sb.append("<min-freq>" + mcMinFrequency.asTHz() + "</min-freq>");
        sb.append("<max-freq>" + mcMaxFrequency.asTHz() + "</max-freq>");
        sb.append("</mc-ttp>");
        sb.append("</interface>");

        return sb.toString();
    }

    public abstract static class Builder<T extends Builder<T>>
      extends OpenRoadmInterface.Builder<T> {

        protected Frequency mcMinFrequency; // expressed in Thz
        protected Frequency mcMaxFrequency; // expressed in Thz

        public T mcMinFrequency(Frequency mcMinFrequency) {
            this.mcMinFrequency = mcMinFrequency;
            return self();
        }

        public T mcMaxFrequency(Frequency mcMaxFrequency) {
            this.mcMaxFrequency = mcMaxFrequency;
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


    protected OpenRoadmMcInterface(Builder<?> builder) {
        super(builder);
        this.mcMinFrequency = builder.mcMinFrequency;
        this.mcMaxFrequency = builder.mcMaxFrequency;
    }
}
