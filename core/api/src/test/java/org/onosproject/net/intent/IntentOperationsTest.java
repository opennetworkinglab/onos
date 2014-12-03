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
package org.onosproject.net.intent;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.TrafficSelector;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Tests for the IntentOperations class.
 */
public class IntentOperationsTest {

    final ConnectPoint egress = NetTestTools.connectPoint("egress", 3);
    final ConnectPoint ingress = NetTestTools.connectPoint("ingress", 3);
    final TrafficSelector selector = new IntentTestsMocks.MockSelector();
    final IntentTestsMocks.MockTreatment treatment = new IntentTestsMocks.MockTreatment();

    private final ApplicationId appId = new DefaultApplicationId((short) 1, "IntentOperationsTest");

    private Intent intent;
    protected IdGenerator idGenerator = new MockIdGenerator();

    @Before
    public void setUp() {
        Intent.bindIdGenerator(idGenerator);

        intent = new PointToPointIntent(NetTestTools.APP_ID,
                                        selector,
                                        treatment,
                                        ingress,
                                        egress);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Checks that the IntentOperation and IntentOperations classes are immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(IntentOperations.class);
        assertThatClassIsImmutable(IntentOperations.Builder.class);
        assertThatClassIsImmutable(IntentOperation.class);
    }

    /**
     * Tests equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final IntentOperations operations1 =
                IntentOperations.builder(appId)
                        .addSubmitOperation(intent)
                        .build();
        final IntentOperations sameAsOperations1 =
                IntentOperations.builder(appId)
                        .addSubmitOperation(intent)
                        .build();
        final IntentOperations operations2 =
                IntentOperations.builder(appId)
                        .addReplaceOperation(intent.id(), intent)
                        .build();

        new EqualsTester()
                .addEqualityGroup(operations1, sameAsOperations1)
                .addEqualityGroup(operations2)
                .testEquals();
    }

    /**
     * Checks that objects are created correctly.
     */
    @Test
    public void testConstruction() {
        final IntentOperations operations =
                IntentOperations.builder(appId)
                        .addUpdateOperation(intent.id())
                        .addWithdrawOperation(intent.id())
                        .build();
        final List<IntentOperation> operationList = operations.operations();
        assertThat(operationList, hasSize(2));
        for (final IntentOperation operation : operationList) {
            assertThat(operation.type(),
                    isOneOf(IntentOperation.Type.UPDATE,
                            IntentOperation.Type.WITHDRAW));
            assertThat(operation.intent(), is((Intent) null));
            assertThat(operation.intentId(), is(intent.id()));
        }
    }
}
