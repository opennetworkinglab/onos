/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.driver.traceable;

import org.junit.Before;
import org.junit.Test;

import org.onosproject.driver.pipeline.ofdpa.Ofdpa2Pipeline;
import org.onosproject.driver.pipeline.ofdpa.OvsOfdpaPipeline;
import org.onosproject.net.DataPlaneEntity;
import org.onosproject.net.PipelineTraceableHitChain;
import org.onosproject.net.PipelineTraceableInput;
import org.onosproject.net.PipelineTraceableOutput;
import org.onosproject.net.PipelineTraceableOutput.PipelineTraceableResult;
import org.onosproject.net.PipelineTraceablePacket;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PipelineTraceable;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;

import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onosproject.driver.traceable.TraceableDataPlaneObjects.getDataPlaneEntities;
import static org.onosproject.driver.traceable.TraceableDataPlaneObjects.getHitChains;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_ARP_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_L2_BRIDG_UNTAG_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_L2_BROAD_UNTAG_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_L3_ECMP_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_L3_UCAST_UNTAG_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_MPLS_ECMP_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_MPLS_ECMP_PACKET_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_PUNT_IP_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.IN_PUNT_LLDP_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.OFDPA_CP;
import static org.onosproject.driver.traceable.TraceableTestObjects.OFDPA_DEVICE;
import static org.onosproject.driver.traceable.TraceableTestObjects.OFDPA_DRIVER;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_L2_BROAD_EMPTY;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_L3_ECMP_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_L3_ECMP_PACKET_1;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_L3_ECMP_PACKET_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_L3_ECMP_PACKET_OFDPA_1;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_L3_UCAST_UNTAG_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_MPLS_ECMP_PACKET;
import static org.onosproject.driver.traceable.TraceableTestObjects.OUT_PORT;
import static org.onosproject.driver.traceable.TraceableTestObjects.OVS_OFDPA_DRIVER;
import static org.onosproject.driver.traceable.TraceableTestObjects.PORT;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.ARP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.ARP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_NOT_ORDERED_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_NOT_ORDERED_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_UNTAG_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_UNTAG_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_EMPTY_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_EMPTY_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_UNTAG_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_UNTAG_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_ECMP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_ECMP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_UCAST_UNTAG_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_UCAST_UNTAG_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.MPLS_ECMP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.MPLS_ECMP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_IP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_IP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_LLDP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_LLDP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.UP_OFDPA_CP;
import static org.onosproject.driver.traceable.TraceableTestObjects.UP_PORT;
import static org.onosproject.driver.traceable.TraceableTestObjects.UP_PORT_1;

/**
 * Tests for Ofdpa pipeline traceable implementation.
 */
public class OfdpaPipelineTraceableTest {

    private TraceableTestObjects.TestDriver ofdpaDriver = new TraceableTestObjects.TestDriver(OFDPA_DRIVER);
    private TraceableTestObjects.TestDriver ovsOfdpaDriver = new TraceableTestObjects.TestDriver(OVS_OFDPA_DRIVER);

    private DriverHandler testDriverHandlerOfdpa;
    private DriverHandler testDriverHandlerOvsOfdpa;

    @Before
    public void setUp() {
        testDriverHandlerOfdpa = createNiceMock(DriverHandler.class);
        testDriverHandlerOvsOfdpa = createNiceMock(DriverHandler.class);
        expect(testDriverHandlerOfdpa.hasBehaviour(Pipeliner.class)).andReturn(true).anyTimes();
        expect(testDriverHandlerOvsOfdpa.hasBehaviour(Pipeliner.class)).andReturn(true).anyTimes();
        expect(testDriverHandlerOfdpa.behaviour(Pipeliner.class)).andReturn(new Ofdpa2Pipeline()).anyTimes();
        expect(testDriverHandlerOvsOfdpa.behaviour(Pipeliner.class)).andReturn(new OvsOfdpaPipeline()).anyTimes();
        replay(testDriverHandlerOfdpa);
        replay(testDriverHandlerOvsOfdpa);
    }

    private PipelineTraceable setUpOfdpa() {
        PipelineTraceable behaviour = new OfdpaPipelineTraceable();
        DriverData driverData = new DefaultDriverData(ofdpaDriver, OFDPA_DEVICE);
        behaviour.setData(driverData);
        behaviour.setHandler(testDriverHandlerOfdpa);
        behaviour.init();
        return behaviour;
    }

    private PipelineTraceable setUpOvsOfdpa() {
        PipelineTraceable behaviour = new OfdpaPipelineTraceable();
        DriverData driverData = new DefaultDriverData(ovsOfdpaDriver, OFDPA_DEVICE);
        behaviour.setData(driverData);
        behaviour.setHandler(testDriverHandlerOvsOfdpa);
        behaviour.init();
        return behaviour;
    }

    /**
     * Test punt ip for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaPuntIP() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_PUNT_IP_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER, PUNT_IP_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(PUNT_IP_OVS_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PortNumber.CONTROLLER));
        assertThat(hitChain.hitChain().size(), is(7));
        assertEquals(IN_PUNT_IP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test punt ip for ofdpa.
     */
    @Test
    public void testOfdpaPuntIP() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_PUNT_IP_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, PUNT_IP_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(PUNT_IP_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PortNumber.CONTROLLER));
        assertThat(hitChain.hitChain().size(), is(4));
        assertEquals(IN_PUNT_IP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test punt arp for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaArp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_ARP_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER, ARP_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(3));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(ARP_OVS_OFDPA);
        assertThat(chains.size(), is(3));

        // This is the copy sent to the controller
        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PortNumber.CONTROLLER));
        assertThat(hitChain.hitChain().size(), is(7));
        assertEquals(IN_ARP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());

        // This is the copy sent to the member port
        hitChain = pipelineOutput.hitChains().get(1);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(8));
        assertEquals(IN_ARP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(1), hitChain.hitChain());

        // This is the copy sent on the input port
        hitChain = pipelineOutput.hitChains().get(2);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PORT));
        assertThat(hitChain.hitChain().size(), is(8));
        assertEquals(IN_ARP_PACKET, hitChain.egressPacket().packet());
        assertTrue(hitChain.isDropped());
        assertEquals(chains.get(2), hitChain.hitChain());
    }

    /**
     * Test punt arp for ovs-ofdpa.
     */
    @Test
    public void testOfdpaArp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_ARP_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, ARP_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(3));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(ARP_OFDPA);
        assertThat(chains.size(), is(3));


        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PortNumber.CONTROLLER));
        assertThat(hitChain.hitChain().size(), is(4));
        assertEquals(IN_ARP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());

        hitChain = pipelineOutput.hitChains().get(1);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(6));
        assertEquals(IN_ARP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(1), hitChain.hitChain());

        hitChain = pipelineOutput.hitChains().get(2);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PORT));
        assertThat(hitChain.hitChain().size(), is(6));
        assertEquals(IN_ARP_PACKET, hitChain.egressPacket().packet());
        assertTrue(hitChain.isDropped());
        assertEquals(chains.get(2), hitChain.hitChain());
    }

    /**
     * Test punt lldp for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaPuntLldp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_PUNT_LLDP_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER, PUNT_LLDP_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(PUNT_LLDP_OVS_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PortNumber.CONTROLLER));
        assertThat(hitChain.hitChain().size(), is(7));
        assertEquals(IN_PUNT_LLDP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test punt lldp for ovs-ofdpa.
     */
    @Test
    public void testOfdpaPuntLldp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_PUNT_LLDP_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, PUNT_LLDP_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(PUNT_LLDP_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PortNumber.CONTROLLER));
        assertThat(hitChain.hitChain().size(), is(4));
        assertEquals(IN_PUNT_LLDP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test l2 bridging with untagged hosts for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaL2BridingUntagged() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BRIDG_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER, L2_BRIDG_UNTAG_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BRIDG_UNTAG_OVS_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(6));
        assertEquals(IN_L2_BRIDG_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test l2 bridging with untagged hosts for ofdpa.
     */
    @Test
    public void testOfdpaL2BridingUntagged() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BRIDG_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, L2_BRIDG_UNTAG_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BRIDG_UNTAG_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(4));
        assertEquals(IN_L2_BRIDG_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test l2 broadcast with untagged hosts for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaL2BroadcastUntagged() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BROAD_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER, L2_BROAD_UNTAG_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(2));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BROAD_UNTAG_OVS_OFDPA);
        assertThat(chains.size(), is(2));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(7));
        assertEquals(IN_L2_BROAD_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());

        // Dropped chain - input port!
        hitChain = pipelineOutput.hitChains().get(1);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PORT));
        assertThat(hitChain.hitChain().size(), is(7));
        assertEquals(IN_L2_BROAD_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertTrue(hitChain.isDropped());
        assertEquals(chains.get(1), hitChain.hitChain());
    }

    /**
     * Test l2 broadcast with untagged hosts for ofdpa.
     */
    @Test
    public void testOfdpaL2BroadcastUntagged() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BROAD_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, L2_BROAD_UNTAG_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(2));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BROAD_UNTAG_OFDPA);
        assertThat(chains.size(), is(2));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(5));
        assertEquals(IN_L2_BROAD_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());

        // Dropped chain - input port!
        hitChain = pipelineOutput.hitChains().get(1);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(PORT));
        assertThat(hitChain.hitChain().size(), is(5));
        assertEquals(IN_L2_BROAD_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertTrue(hitChain.isDropped());
        assertEquals(chains.get(1), hitChain.hitChain());
    }

    /**
     * Test l3 unicast routing for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaL3Unicast() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L3_UCAST_UNTAG_PACKET), UP_OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER,
                L3_UCAST_UNTAG_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);

        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L3_UCAST_UNTAG_OVS_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(7));
        assertEquals(OUT_L3_UCAST_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test l3 unicast routing for ofdpa.
     */
    @Test
    public void testOfdpaL3Unicast() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L3_UCAST_UNTAG_PACKET), UP_OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, L3_UCAST_UNTAG_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);

        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L3_UCAST_UNTAG_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(6));
        assertEquals(OUT_L3_UCAST_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test l3 ecmp routing for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaL3Ecmp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L3_ECMP_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER, L3_ECMP_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);

        assertThat(pipelineOutput.hitChains().size(), is(2));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L3_ECMP_OVS_OFDPA);
        assertThat(chains.size(), is(2));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(UP_PORT));
        assertThat(hitChain.hitChain().size(), is(9));
        assertEquals(OUT_L3_ECMP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());

        // 2nd spine!
        hitChain = pipelineOutput.hitChains().get(1);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(UP_PORT_1));
        assertThat(hitChain.hitChain().size(), is(9));
        assertEquals(OUT_L3_ECMP_PACKET_1, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(1), hitChain.hitChain());
    }

    /**
     * Test l3 ecmp routing for ofdpa.
     */
    @Test
    public void testOfdpaL3Ecmp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L3_ECMP_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, L3_ECMP_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);

        assertThat(pipelineOutput.hitChains().size(), is(2));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L3_ECMP_OFDPA);
        assertThat(chains.size(), is(2));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(UP_PORT));
        assertThat(hitChain.hitChain().size(), is(8));
        assertEquals(OUT_L3_ECMP_PACKET_OFDPA, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());

        hitChain = pipelineOutput.hitChains().get(1);
        assertNotNull(hitChain);

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(UP_PORT_1));
        assertThat(hitChain.hitChain().size(), is(8));
        assertEquals(OUT_L3_ECMP_PACKET_OFDPA_1, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(1), hitChain.hitChain());
    }

    /**
     * Test mpls ecmp routing for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaMplsEcmp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_MPLS_ECMP_PACKET), UP_OFDPA_CP,
                getDataPlaneEntities(OVS_OFDPA_DRIVER, MPLS_ECMP_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);

        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(MPLS_ECMP_OVS_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(UP_PORT_1));
        assertThat(hitChain.hitChain().size(), is(9));
        assertEquals(OUT_MPLS_ECMP_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test mpls ecmp routing for ofdpa.
     */
    @Test
    public void testOfdpaMplsEcmp() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_MPLS_ECMP_PACKET_OFDPA), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, MPLS_ECMP_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);

        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(MPLS_ECMP_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(UP_PORT_1));
        assertThat(hitChain.hitChain().size(), is(7));
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
        assertEquals(OUT_MPLS_ECMP_PACKET, hitChain.egressPacket().packet());
    }

    /**
     * Test failure due l2 flood group with no buckets for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaL2BroadEmpty() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BROAD_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER, L2_BROAD_EMPTY_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.NO_GROUP_MEMBERS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BROAD_EMPTY_OVS_OFDPA);
        assertThat(chains.size(), is(1));

        assertNull(hitChain.outputPort());
        assertThat(hitChain.hitChain().size(), is(6));
        assertEquals(OUT_L2_BROAD_EMPTY, hitChain.egressPacket().packet());
        assertTrue(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test failure due l2 flood group with no buckets for ofdpa.
     */
    @Test
    public void testOfdpaL2BroadEmpty() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BROAD_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, L2_BROAD_EMPTY_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.NO_GROUP_MEMBERS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BROAD_EMPTY_OFDPA);
        assertThat(chains.size(), is(1));

        assertNull(hitChain.outputPort());
        assertThat(hitChain.hitChain().size(), is(4));
        assertEquals(OUT_L2_BROAD_EMPTY, hitChain.egressPacket().packet());
        assertTrue(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test l2 bridging with l2 interface group that has actions not in order for ovs-ofdpa.
     */
    @Test
    public void testOvsOfdpaL2BridingNotOrdered() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BRIDG_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OVS_OFDPA_DRIVER,
                L2_BRIDG_NOT_ORDERED_OVS_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOvsOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BRIDG_NOT_ORDERED_OVS_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(6));
        assertEquals(IN_L2_BRIDG_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

    /**
     * Test l2 bridging with l2 interface group that has actions not in order for ofdpa.
     */
    @Test
    public void testOfdpaL2BridingNotOrdered() {
        PipelineTraceableInput pipelineInput = new PipelineTraceableInput(new PipelineTraceablePacket(
                IN_L2_BRIDG_UNTAG_PACKET), OFDPA_CP, getDataPlaneEntities(OFDPA_DRIVER, L2_BRIDG_NOT_ORDERED_OFDPA));
        PipelineTraceable pipelineTraceable = setUpOfdpa();

        PipelineTraceableOutput pipelineOutput = pipelineTraceable.apply(pipelineInput);
        assertNotNull(pipelineOutput);
        assertThat(pipelineOutput.hitChains().size(), is(1));
        assertThat(pipelineOutput.result(), is(PipelineTraceableResult.SUCCESS));

        PipelineTraceableHitChain hitChain = pipelineOutput.hitChains().get(0);
        assertNotNull(hitChain);
        List<List<DataPlaneEntity>> chains = getHitChains(L2_BRIDG_NOT_ORDERED_OFDPA);
        assertThat(chains.size(), is(1));

        assertNotNull(hitChain.outputPort());
        assertThat(hitChain.outputPort().port(), is(OUT_PORT));
        assertThat(hitChain.hitChain().size(), is(4));
        assertEquals(IN_L2_BRIDG_UNTAG_PACKET, hitChain.egressPacket().packet());
        assertFalse(hitChain.isDropped());
        assertEquals(chains.get(0), hitChain.hitChain());
    }

}