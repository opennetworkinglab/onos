/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onosproject.net.PipelineTraceableInput;
import org.onosproject.net.PipelineTraceableOutput;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Represents a driver behavior that enables a logical packet to trace existing flows and groups in a device.
 */
public interface PipelineTraceable extends HandlerBehaviour {

    /**
     * Initializes the traceable with a context required for its operation.
     */
    void init();

    /**
     * Applies pipeline processing on the given ingress state.
     *
     * @param input the input of the apply process
     * @return the output of the apply process
     */
    PipelineTraceableOutput apply(PipelineTraceableInput input);

}
