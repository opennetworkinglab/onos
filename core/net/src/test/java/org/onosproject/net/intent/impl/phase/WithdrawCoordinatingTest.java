/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.intent.impl.phase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.impl.IntentProcessor;
import org.onosproject.net.intent.impl.IntentRemovalException;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Timestamp;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.intent.IntentState.INSTALLED;
import static org.onosproject.net.intent.IntentState.WITHDRAW_REQ;

/**
 * Unit tests for WithdrawCoordinating phase.
 */
public class WithdrawCoordinatingTest {

    private final ApplicationId appId = new TestApplicationId("test");
    private final ProviderId pid = new ProviderId("of", "test");
    private final TrafficSelector selector = DefaultTrafficSelector.emptySelector();
    private final TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
    private final ConnectPoint cp1 = new ConnectPoint(deviceId("1"), portNumber(1));
    private final ConnectPoint cp2 = new ConnectPoint(deviceId("1"), portNumber(2));
    private final ConnectPoint cp3 = new ConnectPoint(deviceId("2"), portNumber(1));
    private final ConnectPoint cp4 = new ConnectPoint(deviceId("2"), portNumber(2));

    private final List<Link> links = Arrays.asList(new DefaultLink(pid, cp2, cp4, DIRECT));
    private final Path path = new DefaultPath(pid, links, 10);

    private PointToPointIntent input;
    private PathIntent compiled;

    private IdGenerator idGenerator;
    private IntentProcessor processor;
    private Timestamp version;

    @Before
    public void setUp() {
        processor = createMock(IntentProcessor.class);
        version = createMock(Timestamp.class);

        idGenerator = new MockIdGenerator();

        Intent.bindIdGenerator(idGenerator);

        // Intent creation should be placed after binding an ID generator
        input = new PointToPointIntent(appId, selector, treatment, cp1, cp3);
        compiled = new PathIntent(appId, selector, treatment, path);
    }


    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Tests a next phase when no exception occurs.
     */
    @Test
    public void testMoveToNextPhaseWithoutError() {
        IntentData pending = new IntentData(input, WITHDRAW_REQ, version);
        IntentData current = new IntentData(input, INSTALLED, version);
        current.setInstallables(Arrays.asList(compiled));

        FlowRuleOperations operations = createMock(FlowRuleOperations.class);
        expect(processor.uninstallCoordinate(current, pending)).andReturn(operations);
        replay(processor);

        WithdrawCoordinating sut = new WithdrawCoordinating(processor, pending, current);

        Optional<IntentProcessPhase> executed = sut.execute();
        verify(processor);
        assertThat(executed.get(), is(instanceOf(Withdrawing.class)));
    }

    /**
     * Tests a next phase when IntentRemovalExceptionOccurs.
     */
    @Test
    public void testWhenIntentRemovalExceptionOccurs() {
        IntentData pending = new IntentData(input, WITHDRAW_REQ, version);
        IntentData current = new IntentData(input, INSTALLED, version);
        current.setInstallables(Arrays.asList(compiled));

        expect(processor.uninstallCoordinate(current, pending)).andThrow(new IntentRemovalException());
        replay(processor);

        WithdrawCoordinating sut = new WithdrawCoordinating(processor, pending, current);

        Optional<IntentProcessPhase> executed = sut.execute();
        verify(processor);
        assertThat(executed.get(), is(instanceOf(WithdrawingFailed.class)));
    }

}
