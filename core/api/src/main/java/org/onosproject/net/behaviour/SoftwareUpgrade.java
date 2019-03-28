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
 */

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;

import org.onosproject.net.driver.HandlerBehaviour;

import java.util.concurrent.CompletableFuture;

/**
 * Behaviour that upgrades the software on a device.
 *
 */
@Beta
public interface SoftwareUpgrade extends HandlerBehaviour {

    /**
     * Upload a package to device. If no destination path is specified
     * the package will be stored in the /tmp folder with a randomized name.
     *
     * @param sourcePath path to local package.
     * @param destinationPath (optional) path where the package will be saved.
     * @return path where the package was saved on device or null in case of an error.
     */
    public CompletableFuture<String> uploadPackage(String sourcePath, String destinationPath);

    /**
     * Causes the device to switch from the current software agent
     * to the provided agent.
     *
     * @param packagePath path to package on device.
     * @return success - if no exceptions occured; device uptime; device version.
     */
    public CompletableFuture<Response> swapAgent(String packagePath);

    /**
     * Response of SwapAgent.
     */
    public final class Response {
        private final Long uptime;
        private final String version;
        private final boolean success;

        public Response(Long a, String b) {
            uptime = a;
            version = b;
            success = true;
        }

        public Response() {
            uptime = 0L;
            version = "";
            success = false;
        }

        public Long getUptime() {
            return uptime;
        }

        public String getVersion() {
            return version;
        }

        public boolean isSuccess() {
            return success;
        }
    }

}
