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


public class OpenRoadmInterface {

    protected String name;
    protected String description;
    protected String type;
    protected String administrativeState;
    protected String supportingCircuitPack;
    protected String supportingPort;
    protected String supportingInterface;

    public abstract static class Builder<T extends Builder<T>> {
        protected String name;
        protected String description;
        protected String type;
        protected String administrativeState;
        protected String supportingCircuitPack;
        protected String supportingPort;
        protected String supportingInterface;

        protected abstract T self();

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T description(String description) {
            this.description = description;
            return self();
        }

        public T administrativeState(String administrativeState) {
            this.administrativeState = administrativeState;
            return self();
        }

        public T supportingCircuitPack(String supportingCircuitPack) {
            this.supportingCircuitPack = supportingCircuitPack;
            return self();
        }

        public T supportingPort(String supportingPort) {
            this.supportingPort = supportingPort;
            return self();
        }

        public T supportingInterface(String supportingInterface) {
            this.supportingInterface = supportingInterface;
            return self();
        }

        public OpenRoadmInterface build() { //
            return new OpenRoadmInterface(this);
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

    protected OpenRoadmInterface(Builder<?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.type = builder.type;
        this.administrativeState = builder.administrativeState;
        this.supportingCircuitPack = builder.supportingCircuitPack;
        this.supportingPort = builder.supportingPort;
        this.supportingInterface = builder.supportingInterface;
    }
}
