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
package org.onosproject.net.driver;

/**
 * Abstraction of handler behaviour used to set-up and tear-down driver
 * connection with a device.
 */
public interface DriverConnect extends HandlerBehaviour {

    /**
     * Connects to the device.
     *
     * @param credentials optional login credentials in string form
     */
    void connect(String... credentials);

    /**
     * Disconnects from the device.
     */
    void disconnect();

}
