/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.store.cluster.messaging.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.VersionService;
import org.onosproject.store.cluster.messaging.MessageSubject;

@Component(immediate = true)
@Service
public class ClusterCommunicationManager extends AbstractClusterCommunicationManager {

    private static final char VERSION_SEP = '-';

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private VersionService versionService;

    @Override
    protected String getType(MessageSubject subject) {
        return subject.value() + VERSION_SEP + versionService.version().toString();
    }
}
