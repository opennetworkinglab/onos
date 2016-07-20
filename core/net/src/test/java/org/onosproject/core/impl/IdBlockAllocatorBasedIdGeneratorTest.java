/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.IdBlock;

/**
 * Suites of test of {@link org.onosproject.core.impl.BlockAllocatorBasedIdGenerator}.
 */
public class IdBlockAllocatorBasedIdGeneratorTest {
    private IdBlockAllocator allocator;
    private BlockAllocatorBasedIdGenerator sut;

    @Before
    public void setUp() {
        allocator = EasyMock.createMock(IdBlockAllocator.class);

    }

    /**
     * Tests generated IntentId sequences using two {@link org.onosproject.core.IdBlock blocks}.
     */
    @Test
    public void testIds() {
        EasyMock.expect(allocator.allocateUniqueIdBlock())
                .andReturn(new IdBlock(0, 3))
                .andReturn(new IdBlock(4, 3));

        EasyMock.replay(allocator);
        sut = new BlockAllocatorBasedIdGenerator(allocator);

        Assert.assertThat(sut.getNewId(), Matchers.is(0L));
        Assert.assertThat(sut.getNewId(), Matchers.is(1L));
        Assert.assertThat(sut.getNewId(), Matchers.is(2L));

        Assert.assertThat(sut.getNewId(), Matchers.is(4L));
        Assert.assertThat(sut.getNewId(), Matchers.is(5L));
        Assert.assertThat(sut.getNewId(), Matchers.is(6L));
    }
}
