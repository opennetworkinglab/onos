/*
 *
 * Copyright 2015 AT&T Foundry
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.onosproject.aaa;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class StateMachineTest {
    StateMachine stateMachine = null;

    @Before
    public void setUp() {
        System.out.println("Set Up.");
        StateMachine.bitSet.clear();
        StateMachine.initializeMaps();
        stateMachine = new StateMachine("session0", null);
    }

    @After
    public void tearDown() {
        System.out.println("Tear Down.");
        StateMachine.bitSet.clear();
        StateMachine.destroyMaps();
        stateMachine = null;
    }

    @Test
    /**
     * Test all the basic inputs from state to state: IDLE -> STARTED -> PENDING -> AUTHORIZED -> IDLE
     */
    public void basic() throws StateMachineException {
        System.out.println("======= BASIC =======.");
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);

        stateMachine.start();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_STARTED);

        stateMachine.requestAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);

        stateMachine.authorizeAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_AUTHORIZED);

        stateMachine.logoff();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);
    }

    @Test
    /**
     * Test all inputs from an IDLE state (starting with the ones that are not impacting the current state)
     */
    public void testIdleState() throws StateMachineException {
        System.out.println("======= IDLE STATE TEST =======.");
        stateMachine.requestAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);

        stateMachine.authorizeAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);

        stateMachine.denyAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);

        stateMachine.logoff();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);

        stateMachine.start();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_STARTED);
    }

    @Test
    /**
     * Test all inputs from an STARTED state (starting with the ones that are not impacting the current state)
     */
    public void testStartedState() throws StateMachineException {
        System.out.println("======= STARTED STATE TEST =======.");
        stateMachine.start();

        stateMachine.authorizeAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_STARTED);

        stateMachine.denyAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_STARTED);

        stateMachine.logoff();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_STARTED);

        stateMachine.start();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_STARTED);

        stateMachine.requestAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);
    }

    @Test
    /**
     * Test all inputs from a PENDING state (starting with the ones that are not impacting the current state).
     * The next valid state for this test is AUTHORIZED
     */
    public void testPendingStateToAuthorized() throws StateMachineException {
        System.out.println("======= PENDING STATE TEST (AUTHORIZED) =======.");
        stateMachine.start();
        stateMachine.requestAccess();

        stateMachine.logoff();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);

        stateMachine.start();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);

        stateMachine.requestAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);

        stateMachine.authorizeAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_AUTHORIZED);

        stateMachine.denyAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_AUTHORIZED);
    }

    @Test
    /**
     * Test all inputs from an PENDING state (starting with the ones that are not impacting the current state).
     * The next valid state for this test is UNAUTHORIZED
     */
    public void testPendingStateToUnauthorized() throws StateMachineException {
        System.out.println("======= PENDING STATE TEST (DENIED) =======.");
        stateMachine.start();
        stateMachine.requestAccess();

        stateMachine.logoff();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);

        stateMachine.start();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);

        stateMachine.requestAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_PENDING);

        stateMachine.denyAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_UNAUTHORIZED);

        stateMachine.authorizeAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_UNAUTHORIZED);
    }

    @Test
    /**
     * Test all inputs from an AUTHORIZED state (starting with the ones that are not impacting the current state).
     */
    public void testAuthorizedState() throws StateMachineException {
        System.out.println("======= AUTHORIZED STATE TEST =======.");
        stateMachine.start();
        stateMachine.requestAccess();
        stateMachine.authorizeAccess();

        stateMachine.start();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_AUTHORIZED);

        stateMachine.requestAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_AUTHORIZED);

        stateMachine.authorizeAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_AUTHORIZED);

        stateMachine.denyAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_AUTHORIZED);

        stateMachine.logoff();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);
    }

    @Test
    /**
     * Test all inputs from an UNAUTHORIZED state (starting with the ones that are not impacting the current state).
     */
    public void testUnauthorizedState() throws StateMachineException {
        System.out.println("======= UNAUTHORIZED STATE TEST =======.");
        stateMachine.start();
        stateMachine.requestAccess();
        stateMachine.denyAccess();

        stateMachine.start();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_UNAUTHORIZED);

        stateMachine.requestAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_UNAUTHORIZED);

        stateMachine.authorizeAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_UNAUTHORIZED);

        stateMachine.denyAccess();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_UNAUTHORIZED);

        stateMachine.logoff();
        Assert.assertEquals(stateMachine.state(), StateMachine.STATE_IDLE);
    }


    @Test
    public void testIdentifierAvailability() throws StateMachineException {
        System.out.println("======= IDENTIFIER TEST =======.");
        byte identifier = stateMachine.identifier();
        System.out.println("State: " + stateMachine.state());
        System.out.println("Identifier: " + Byte.toUnsignedInt(identifier));
        Assert.assertEquals(-1, identifier);
        stateMachine.start();


        StateMachine sm247 = null;
        StateMachine sm3 = null;


        //create 255 others state machines
        for (int i = 1; i <= 255; i++) {
                StateMachine sm = new StateMachine("session" + i, null);
                sm.start();
                byte id = sm.identifier();
                Assert.assertEquals(i, Byte.toUnsignedInt(id));
                if (i == 3) {
                    sm3 = sm;
                    System.out.println("SM3: " + sm3.toString());
                }
                if (i == 247) {
                    sm247 = sm;
                    System.out.println("SM247: " + sm247.toString());
                }
        }

        //simulate the state machine for a specific session and logoff so we can free up a spot for an identifier
        //let's choose identifier 247 then we free up 3
        Assert.assertNotNull(sm247);
        sm247.requestAccess();
        sm247.authorizeAccess();
        sm247.logoff();

        Assert.assertNotNull(sm3);
        sm3.requestAccess();
        sm3.authorizeAccess();
        sm3.logoff();

        StateMachine otherSM3 = new StateMachine("session3b", null);
        otherSM3.start();
        otherSM3.requestAccess();
        byte id3 = otherSM3.identifier();
        Assert.assertEquals(3, Byte.toUnsignedInt(id3));

        StateMachine otherSM247 = new StateMachine("session247b", null);
        otherSM247.start();
        otherSM247.requestAccess();
        byte id247 = otherSM247.identifier();
        Assert.assertEquals(247, Byte.toUnsignedInt(id247));
    }

    @Test
    public void testSessionIdLookups() {
        String sessionId1 = "session1";
        String sessionId2 = "session2";
        String sessionId3 = "session3";

        StateMachine machine1ShouldBeNull =
                StateMachine.lookupStateMachineBySessionId(sessionId1);
        assertNull(machine1ShouldBeNull);
        StateMachine machine2ShouldBeNull =
                StateMachine.lookupStateMachineBySessionId(sessionId2);
        assertNull(machine2ShouldBeNull);

        StateMachine stateMachine1 = new StateMachine(sessionId1, null);
        StateMachine stateMachine2 = new StateMachine(sessionId2, null);

        assertEquals(stateMachine1,
                     StateMachine.lookupStateMachineBySessionId(sessionId1));
        assertEquals(stateMachine2,
                     StateMachine.lookupStateMachineBySessionId(sessionId2));
        assertNull(StateMachine.lookupStateMachineBySessionId(sessionId3));
    }

    @Test
    public void testIdentifierLookups() throws StateMachineException {
        String sessionId1 = "session1";
        String sessionId2 = "session2";

        StateMachine machine1ShouldBeNull =
                StateMachine.lookupStateMachineById((byte) 1);
        assertNull(machine1ShouldBeNull);
        StateMachine machine2ShouldBeNull =
                StateMachine.lookupStateMachineById((byte) 2);
        assertNull(machine2ShouldBeNull);

        StateMachine stateMachine1 = new StateMachine(sessionId1, null);
        stateMachine1.start();
        StateMachine stateMachine2 = new StateMachine(sessionId2, null);
        stateMachine2.start();

        assertEquals(stateMachine1,
                     StateMachine.lookupStateMachineById(stateMachine1.identifier()));
        assertEquals(stateMachine2,
                     StateMachine.lookupStateMachineById(stateMachine2.identifier()));
    }
}
