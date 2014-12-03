package org.onosproject.core.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;

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
