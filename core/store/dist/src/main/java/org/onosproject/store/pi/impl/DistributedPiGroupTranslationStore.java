/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.store.pi.impl;

import org.onosproject.net.group.Group;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.service.PiGroupTranslationStore;
import org.osgi.service.component.annotations.Component;

/**
 * Distributed implementation of a PI translation store for groups.
 */
@Component(immediate = true, service = PiGroupTranslationStore.class)
public class DistributedPiGroupTranslationStore
        extends AbstractDistributedPiTranslationStore<Group, PiActionProfileGroup>
        implements PiGroupTranslationStore {

    private static final String MAP_SIMPLE_NAME = "group";

    @Override
    protected String mapSimpleName() {
        return MAP_SIMPLE_NAME;
    }
}
