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
package org.onosproject.incubator.net.tunnel;

import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.net.NetworkResource;

import java.util.List;

/**
 * Representation of a label stack in a network which represents the network path.
 */
public interface LabelStack extends NetworkResource {

    /**
     * Returns sequence of label resources comprising the path.
     *
     * @return list of links
     */
    List<LabelResourceId> labelResources();
}
