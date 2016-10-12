/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.api;

import io.netty.channel.nio.NioEventLoopGroup;
import org.onosproject.incubator.net.virtual.NetworkId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Representation of an OF agent, which brokers virtual devices and external
 * controllers by handling OpenFlow connections and messages between them.
 */
public interface OFAgent {

    /**
     * Returns the identifier of the virtual network that this agent cares for.
     *
     * @return id of the virtual network
     */
    NetworkId networkId();

    /**
     * Returns the external OpenFlow controllers of the virtual network.
     *
     * @return set of openflow controllers
     */
    Set<OFController> controllers();

    /**
     * Starts the OpenFlow agent.
     */
    void start();

    /**
     * Stops the OpenFlow agent.
     */
    void stop();

    /**
     * Builder of OF agent entities.
     */
    interface Builder {

        /**
         * Returns new OF agent.
         *
         * @return of agent
         */
        OFAgent build();

        /**
         * Returns OF agent builder with the supplied network ID.
         *
         * @param networkId id of the virtual network
         * @return of agent builder
         */
        Builder networkId(NetworkId networkId);

        /**
         * Returns OF agent builder with the supplied network services for the
         * virtual network.
         *
         * @param services network services for the virtual network
         * @return of agent builder
         */
        Builder services(Map<Class<?>, Object> services);

        /**
         * Returns OF agent builder with the supplied controllers.
         *
         * @param controllers set of openflow controllers
         * @return of agent builder
         */
        Builder controllers(Set<OFController> controllers);

        /**
         * Returns OF agent builder with the supplied event executor.
         *
         * @param eventExecutor event executor
         * @return of agent builder
         */
        Builder eventExecutor(ExecutorService eventExecutor);

        /**
         * Returns OF agent builder with the supplied IO work group.
         *
         * @param ioWorker io worker group
         * @return of agent builder
         */
        Builder ioWorker(NioEventLoopGroup ioWorker);
    }
}
