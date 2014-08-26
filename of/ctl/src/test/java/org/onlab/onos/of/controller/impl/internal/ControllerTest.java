/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.onlab.onos.of.controller.impl.internal;

import junit.framework.TestCase;
import org.onlab.onos.of.controller.impl.IOFSwitch;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ControllerTest extends TestCase {

    private Controller controller;
    private IOFSwitch sw;
    private OFChannelHandler h;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        sw = EasyMock.createMock(IOFSwitch.class);
        h = EasyMock.createMock(OFChannelHandler.class);
        controller = new Controller();
        ControllerRunThread t = new ControllerRunThread();
        t.start();
        /*
         * Making sure the thread is properly started before making calls
         * to controller class.
         */
        Thread.sleep(200);
    }

    /**
     * Starts the base mocks used in these tests.
     */
    private void startMocks() {
        EasyMock.replay(sw, h);
    }

    /**
     * Reset the mocks to a known state.
     * Automatically called after tests.
     */
    @After
    private void resetMocks() {
        EasyMock.reset(sw);
    }

    /**
     * Fetches the controller instance.
     * @return the controller
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Run the controller's main loop so that updates are processed.
     */
    protected class ControllerRunThread extends Thread {
        @Override
        public void run() {
            controller.openFlowPort = 0; // Don't listen
            controller.activate();
        }
    }

    /**
     * Verify that we are able to add a switch that just connected.
     * If it already exists then this should fail
     *
     * @throws Exception error
     */
    @Test
    public void testAddConnectedSwitches() throws Exception {
        startMocks();
        assertTrue(controller.addConnectedSwitch(0, h));
        assertFalse(controller.addConnectedSwitch(0, h));
    }

    /**
     * Add active master but cannot re-add active master.
     * @throws Exception an error occurred.
     */
    @Test
    public void testAddActivatedMasterSwitch() throws Exception {
        startMocks();
        controller.addConnectedSwitch(0, h);
        assertTrue(controller.addActivatedMasterSwitch(0, sw));
        assertFalse(controller.addActivatedMasterSwitch(0, sw));
    }

    /**
     * Tests that an activated switch can be added but cannot be re-added.
     *
     * @throws Exception an error occurred
     */
    @Test
    public void testAddActivatedEqualSwitch() throws Exception {
        startMocks();
        controller.addConnectedSwitch(0, h);
        assertTrue(controller.addActivatedEqualSwitch(0, sw));
        assertFalse(controller.addActivatedEqualSwitch(0, sw));
    }

    /**
     * Move an equal switch to master.
     * @throws Exception an error occurred
     */
    @Test
    public void testTranstitionToMaster() throws Exception {
        startMocks();
        controller.addConnectedSwitch(0, h);
        controller.addActivatedEqualSwitch(0, sw);
        controller.transitionToMasterSwitch(0);
        assertNotNull(controller.getMasterSwitch(0));
    }

    /**
     * Transition a master switch to equal state.
     * @throws Exception an error occurred
     */
    @Test
    public void testTranstitionToEqual() throws Exception {
        startMocks();
        controller.addConnectedSwitch(0, h);
        controller.addActivatedMasterSwitch(0, sw);
        controller.transitionToEqualSwitch(0);
        assertNotNull(controller.getEqualSwitch(0));
    }

    /**
     * Remove the switch from the controller instance.
     * @throws Exception an error occurred
     */
    @Test
    public void testRemoveSwitch() throws Exception {
        sw.cancelAllStatisticsReplies();
        EasyMock.expectLastCall().once();
        sw.setConnected(false);
        EasyMock.expectLastCall().once();
        startMocks();
        controller.addConnectedSwitch(0, h);
        controller.addActivatedMasterSwitch(0, sw);
        controller.removeConnectedSwitch(0);
        assertNull(controller.getSwitch(0));
        EasyMock.verify(sw, h);
    }
}
