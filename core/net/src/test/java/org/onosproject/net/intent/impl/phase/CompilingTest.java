/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.impl.IntentProcessor;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Timestamp;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.intent.IntentState.INSTALL_REQ;

/**
 * Unit tests for Compiling phase.
 */
public class CompilingTest extends AbstractIntentTest {

    private final ApplicationId appId = new TestApplicationId("test");
    private final ProviderId pid = new ProviderId("of", "test");
    private final TrafficSelector selector = DefaultTrafficSelector.emptySelector();
    private final TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
    private final ConnectPoint cp1 = new ConnectPoint(deviceId("1"), portNumber(1));
    private final ConnectPoint cp2 = new ConnectPoint(deviceId("1"), portNumber(2));
    private final ConnectPoint cp3 = new ConnectPoint(deviceId("2"), portNumber(1));
    private final ConnectPoint cp4 = new ConnectPoint(deviceId("2"), portNumber(2));

    private final List<Link> links = Collections.singletonList(
            DefaultLink.builder().providerId(pid).src(cp2).dst(cp4).type(DIRECT).build());
    private final Path path = new DefaultPath(pid, links, 10);

    private PointToPointIntent input;
    private PathIntent compiled;

    private IdGenerator idGenerator;
    private IntentProcessor processor;
    private Timestamp version;

    @Before
    public void setUp() {
        super.setUp();

        processor = createMock(IntentProcessor.class);
        version = createMock(Timestamp.class);

        // Intent creation should be placed after binding an ID generator
        input = PointToPointIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(cp1)
                .egressPoint(cp3)
                .build();
        compiled = PathIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .path(path)
                .build();
    }

    /**
     * Tests a next phase when no exception occurs.
     */
    @Test
    public void testMoveToNextPhaseWithoutError() {
        IntentData pending = new IntentData(input, INSTALL_REQ, version);

        expect(processor.compile(input, null)).andReturn(Collections.singletonList(compiled));
        replay(processor);

        Compiling sut = new Compiling(processor, pending, Optional.empty());

        Optional<IntentProcessPhase> output = sut.execute();

        verify(processor);
        assertThat(output.get(), is(instanceOf(Installing.class)));
    }

    /**
     * Tests a next phase when IntentCompilationException occurs.
     */
    @Test
    public void testWhenIntentCompilationExceptionOccurs() {
        IntentData pending = new IntentData(input, INSTALL_REQ, version);

        expect(processor.compile(input, null)).andThrow(new IntentCompilationException());
        replay(processor);

        Compiling sut = new Compiling(processor, pending, Optional.empty());

        Optional<IntentProcessPhase> output = sut.execute();

        verify(processor);
        assertThat(output.get(), is(instanceOf(Failed.class)));
    }
}
