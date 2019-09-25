/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric;

import com.google.common.annotations.Beta;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;

import java.net.URL;

/**
 * A service to build fabric.p4-related pipeconfs.
 * <p>
 * This service is provided such that third-party apps can build vendor-specific
 * versions of the fabric.p4 pipeconf, without needing to depend on fabric.p4
 * behaviour implementations at compile time.
 */
@Beta
public interface FabricPipeconfService {

    /**
     * Builds a pipeconf for fabric.p4.
     * <p>
     * This method expects as input a pipeconf builder already populated with
     * the pipeconf ID (i.e., {@link DefaultPiPipeconf.Builder#withId}) and
     * target-specific extensions (i.e., {@link DefaultPiPipeconf.Builder#addExtension}.
     * The implementation takes care of adding all the necessary behavior
     * implementations specific to fabric.p4, depending on the profile name,
     * e.g., adding INT-related behaviors for fabric-int profile).
     * <p>
     * Finally, the implementation takes care of parsing the given P4Info file
     * (in text format) as pipeconf pipeline model, and setting the pipeconf CPU
     * port to the one contained in the given file URL.
     *
     * @param builder    pipeconf builder already populated with ID and
     *                   target-specific extensions
     * @param profile    fabric.p4 profile name
     * @param p4InfoUrl  URL to P4Info file in text format
     * @param cpuPortUrl URL to txt file containing the CPU port
     * @return pipeconf instance
     */
    PiPipeconf buildFabricPipeconf(DefaultPiPipeconf.Builder builder, String profile,
                                   URL p4InfoUrl, URL cpuPortUrl);
}
