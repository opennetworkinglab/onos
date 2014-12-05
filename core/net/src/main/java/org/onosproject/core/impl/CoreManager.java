/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.core.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationIdStore;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdBlockStore;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;
import org.onlab.util.Tools;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Core service implementation.
 */
@Component(immediate = true)
@Service
public class CoreManager implements CoreService {

    private static final File VERSION_FILE = new File("../VERSION");
    private static Version version = Version.version("1.0.0-SNAPSHOT");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationIdStore applicationIdStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IdBlockStore idBlockStore;

    @Activate
    public void activate() {
        List<String> versionLines = Tools.slurp(VERSION_FILE);
        if (versionLines != null && !versionLines.isEmpty()) {
            version = Version.version(versionLines.get(0));
        }
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return applicationIdStore.getAppIds();
    }

    @Override
    public ApplicationId getAppId(Short id) {
        return applicationIdStore.getAppId(id);
    }

    @Override
    public ApplicationId registerApplication(String name) {
        checkNotNull(name, "Application ID cannot be null");
        return applicationIdStore.registerApplication(name);
    }

    @Override
    public IdGenerator getIdGenerator(String topic) {
        IdBlockAllocator allocator = new StoreBasedIdBlockAllocator(topic, idBlockStore);
        return new BlockAllocatorBasedIdGenerator(allocator);
    }

}
