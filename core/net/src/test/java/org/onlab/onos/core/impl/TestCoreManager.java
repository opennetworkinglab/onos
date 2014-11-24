package org.onlab.onos.core.impl;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.core.IdGenerator;
import org.onlab.onos.core.Version;

import java.util.Set;

public class TestCoreManager implements CoreService {
    @Override
    public Version version() {
        return null;
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return null;
    }

    @Override
    public ApplicationId getAppId(Short id) {
        return null;
    }

    @Override
    public ApplicationId registerApplication(String identifier) {
        return null;
    }

    @Override
    public IdGenerator getIdGenerator(String topic) {
        IdBlockAllocator idBlockAllocator = new DummyIdBlockAllocator();
        return new BlockAllocatorBasedIdGenerator(idBlockAllocator);
    }
}
