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
package org.onosproject.openstacknode;

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes OpenStack node init state event.
 */
public class OpenstackNodeEvent extends AbstractEvent<OpenstackNodeEvent.NodeState, Object> {

    public enum NodeState {
        /**
         * Indicates the node is newly added.
         */
        INIT {
            @Override
            public void process(OpenstackNodeService nodeService, OpenstackNode node) {
                nodeService.processInitState(node);
            }
        },
        /**
         * Indicates bridge devices are added according to the node state.
         */
        DEVICE_CREATED {
            @Override
            public void process(OpenstackNodeService nodeService, OpenstackNode node) {
                nodeService.processDeviceCreatedState(node);
            }
        },
        /**
         * Indicates all node initialization is done.
         */
        COMPLETE {
            @Override
            public void process(OpenstackNodeService nodeService, OpenstackNode node) {
                nodeService.processCompleteState(node);
            }
        },
        /**
         * Indicates node initialization is not done but unable to proceed to
         * the next step for some reason.
         */
        INCOMPLETE {
            @Override
            public void process(OpenstackNodeService nodeService, OpenstackNode node) {
                nodeService.processIncompleteState(node);
            }
        };

        public abstract void process(OpenstackNodeService nodeService, OpenstackNode node);
    }

    public OpenstackNodeEvent(NodeState state, Object subject) {
        super(state, subject);
    }

    public OpenstackNode node() {
        return (OpenstackNode) subject();
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("state", type())
                .add("node", subject())
                .toString();
    }
}
