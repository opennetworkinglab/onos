/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.rabbitmq.api;

/**
 * Interface for declaring a start, publish and stop api's for mq transactions.
 */
public interface Manageable {
    /**
     * Establishes connection with MQ server.
     */
    void start();

    /**
     * Publishes onos events on to MQ server.
     */
    void publish();

    /**
     * Releases connection and channels.
     */
    void stop();

}
