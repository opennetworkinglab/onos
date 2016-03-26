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
package org.onosproject.pce.pceservice.api;

import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.LspType;

/**
 * Service to compute path based on constraints, release path and update path with new constraints.
 */
public interface PceService {

    /**
     * Creates new path based on constraints and lsp type.
     *
     * @param src source device
     * @param dst destination device
     * @param constraints list of constraints to be applied on path
     * @param lspType type of path to be setup
     * @return false on failure and true on successful path creation
     */
    boolean setupPath(DeviceId src, DeviceId dst, List<Constraint> constraints, LspType lspType);

    //TODO: updatePath
    //TODO: releasePath
    //TODO: queryPath
}