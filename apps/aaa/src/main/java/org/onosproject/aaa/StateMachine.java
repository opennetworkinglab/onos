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

import java.util.BitSet;
import java.util.Map;

import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.xosintegration.VoltTenant;
import org.onosproject.xosintegration.VoltTenantService;
import org.slf4j.Logger;

import com.google.common.collect.Maps;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * AAA Finite State Machine.
 */

class StateMachine {
    //INDEX to identify the state in the transition table
    static final int STATE_IDLE = 0;
    static final int STATE_STARTED = 1;
    static final int STATE_PENDING = 2;
    static final int STATE_AUTHORIZED = 3;
    static final int STATE_UNAUTHORIZED = 4;

    //INDEX to identify the transition in the transition table
    static final int TRANSITION_START = 0; // --> started
    static final int TRANSITION_REQUEST_ACCESS = 1;
    static final int TRANSITION_AUTHORIZE_ACCESS = 2;
    static final int TRANSITION_DENY_ACCESS = 3;
    static final int TRANSITION_LOGOFF = 4;

    //map of access identifiers (issued at EAPOL START)
    static BitSet bitSet = new BitSet();
    private final VoltTenantService voltService;

    private int identifier = -1;
    private byte challengeIdentifier;
    private byte[] challengeState;
    private byte[] username;
    private byte[] requestAuthenticator;

    // Supplicant connectivity info
    private ConnectPoint supplicantConnectpoint;
    private MacAddress supplicantAddress;
    private short vlanId;

    private String sessionId = null;

    private final Logger log = getLogger(getClass());


    private State[] states = {
            new Idle(), new Started(), new Pending(), new Authorized(), new Unauthorized()
    };


    //State transition table
    /*

                state       IDLE    |   STARTED         |   PENDING         |   AUTHORIZED  |   UNAUTHORIZED
             ////
       input
       ----------------------------------------------------------------------------------------------------

       START                STARTED |   _               |   _               |   _           |   _

       REQUEST_ACCESS       _       |   PENDING         |   _               |   _           |   _

       AUTHORIZE_ACCESS     _       |   _               |   AUTHORIZED      |   _           |   _

       DENY_ACCESS          _       |   -               |   UNAUTHORIZED    |   _           |   _

       LOGOFF               _       |   _               |   _               |   IDLE        |   IDLE
     */

    private int[] idleTransition =
            {STATE_STARTED, STATE_IDLE, STATE_IDLE, STATE_IDLE, STATE_IDLE};
    private int[] startedTransition =
            {STATE_STARTED, STATE_PENDING, STATE_STARTED, STATE_STARTED, STATE_STARTED};
    private int[] pendingTransition =
            {STATE_PENDING, STATE_PENDING, STATE_AUTHORIZED, STATE_UNAUTHORIZED, STATE_PENDING};
    private int[] authorizedTransition =
            {STATE_AUTHORIZED, STATE_AUTHORIZED, STATE_AUTHORIZED, STATE_AUTHORIZED, STATE_IDLE};
    private int[] unauthorizedTransition =
            {STATE_UNAUTHORIZED, STATE_UNAUTHORIZED, STATE_UNAUTHORIZED, STATE_UNAUTHORIZED, STATE_IDLE};

    //THE TRANSITION TABLE
    private int[][] transition =
            {idleTransition, startedTransition, pendingTransition, authorizedTransition,
                    unauthorizedTransition};

    private int currentState = STATE_IDLE;

    // Maps of state machines. Each state machine is represented by an
    // unique identifier on the switch: dpid + port number
    private static Map<String, StateMachine> sessionIdMap;
    private static Map<Integer, StateMachine> identifierMap;

    public static void initializeMaps() {
        sessionIdMap = Maps.newConcurrentMap();
        identifierMap = Maps.newConcurrentMap();
    }

    public static void destroyMaps() {
        sessionIdMap = null;
        identifierMap = null;
    }

    public static StateMachine lookupStateMachineById(byte identifier) {
        return identifierMap.get((int) identifier);
    }

    public static StateMachine lookupStateMachineBySessionId(String sessionId) {
        return sessionIdMap.get(sessionId);
    }    /**
     * State Machine Constructor.
     *
     * @param sessionId   session Id represented by the switch dpid +  port number
     * @param voltService volt service reference
     */
    public StateMachine(String sessionId, VoltTenantService voltService) {
        log.info("Creating a new state machine for {}", sessionId);
        this.sessionId = sessionId;
        this.voltService = voltService;
        sessionIdMap.put(sessionId, this);
    }

    /**
     * Gets the connect point for the supplicant side.
     *
     * @return supplicant connect point
     */
    public ConnectPoint supplicantConnectpoint() {
        return supplicantConnectpoint;
    }

    /**
     * Sets the supplicant side connect point.
     *
     * @param supplicantConnectpoint supplicant select point.
     */
    public void setSupplicantConnectpoint(ConnectPoint supplicantConnectpoint) {
        this.supplicantConnectpoint = supplicantConnectpoint;
    }

    /**
     * Gets the MAC address of the supplicant.
     *
     * @return supplicant MAC address
     */
    public MacAddress supplicantAddress() {
        return supplicantAddress;
    }

    /**
     * Sets the supplicant MAC address.
     *
     * @param supplicantAddress new supplicant MAC address
     */
    public void setSupplicantAddress(MacAddress supplicantAddress) {
        this.supplicantAddress = supplicantAddress;
    }

    /**
     * Gets the client's Vlan ID.
     *
     * @return client vlan ID
     */
    public short vlanId() {
        return vlanId;
    }

    /**
     * Sets the client's vlan ID.
     *
     * @param vlanId new client vlan ID
     */
    public void setVlanId(short vlanId) {
        this.vlanId = vlanId;
    }

    /**
     * Gets the client id that is requesting for access.
     *
     * @return The client id.
     */
    public String sessionId() {
        return this.sessionId;
    }

    /**
     * Create the identifier for the state machine (happens when goes to STARTED state).
     */
    private void createIdentifier() throws StateMachineException {
        log.debug("Creating Identifier.");
        int index;

        try {
            //find the first available spot for identifier assignment
            index = StateMachine.bitSet.nextClearBit(0);

            //there is a limit of 256 identifiers
            if (index == 256) {
                throw new StateMachineException("Cannot handle any new identifier. Limit is 256.");
            }
        } catch (IndexOutOfBoundsException e) {
            throw new StateMachineException(e.getMessage());
        }

        log.info("Assigning identifier {}", index);
        StateMachine.bitSet.set(index);
        this.identifier = index;
    }

    /**
     * Set the challenge identifier and the state issued by the RADIUS.
     *
     * @param challengeIdentifier The challenge identifier set into the EAP packet from the RADIUS message.
     * @param challengeState      The challenge state from the RADIUS.
     */
    protected void setChallengeInfo(byte challengeIdentifier, byte[] challengeState) {
        this.challengeIdentifier = challengeIdentifier;
        this.challengeState = challengeState;
    }

    /**
     * Set the challenge identifier issued by the RADIUS on the access challenge request.
     *
     * @param challengeIdentifier The challenge identifier set into the EAP packet from the RADIUS message.
     */
    protected void setChallengeIdentifier(byte challengeIdentifier) {
        log.info("Set Challenge Identifier to {}", challengeIdentifier);
        this.challengeIdentifier = challengeIdentifier;
    }

    /**
     * Gets the challenge EAP identifier set by the RADIUS.
     *
     * @return The challenge EAP identifier.
     */
    protected byte challengeIdentifier() {
        return this.challengeIdentifier;
    }


    /**
     * Set the challenge state info issued by the RADIUS.
     *
     * @param challengeState The challenge state from the RADIUS.
     */
    protected void setChallengeState(byte[] challengeState) {
        log.info("Set Challenge State");
        this.challengeState = challengeState;
    }

    /**
     * Gets the challenge state set by the RADIUS.
     *
     * @return The challenge state.
     */
    protected byte[] challengeState() {
        return this.challengeState;
    }

    /**
     * Set the username.
     *
     * @param username The username sent to the RADIUS upon access request.
     */
    protected void setUsername(byte[] username) {
        this.username = username;
    }


    /**
     * Gets the username.
     *
     * @return The requestAuthenticator.
     */
    protected byte[] requestAuthenticator() {
        return this.requestAuthenticator;
    }

    /**
     * Sets the authenticator.
     *
     * @param authenticator The username sent to the RADIUS upon access request.
     */
    protected void setRequestAuthenticator(byte[] authenticator) {
        this.requestAuthenticator = authenticator;
    }


    /**
     * Gets the username.
     *
     * @return The username.
     */
    protected byte[] username() {
        return this.username;
    }

    /**
     * Return the identifier of the state machine.
     *
     * @return The state machine identifier.
     */
    public byte identifier() {
        return (byte) this.identifier;
    }


    protected void deleteIdentifier() {
        if (this.identifier != -1) {
            log.info("Freeing up " + this.identifier);
            //this state machine should be deleted and free up the identifier
            StateMachine.bitSet.clear(this.identifier);
            this.identifier = -1;
        }
    }


    /**
     * Move to the next state.
     *
     * @param msg message
     */
    private void next(int msg) {
        currentState = transition[currentState][msg];
        log.info("Current State " + currentState);
    }

    /**
     * Client has requested the start action to allow network access.
     *
     * @throws StateMachineException if authentication protocol is violated
     */
    public void start() throws StateMachineException {
        states[currentState].start();
        //move to the next state
        next(TRANSITION_START);
        createIdentifier();
        identifierMap.put(identifier, this);
    }

    /**
     * An Identification information has been sent by the supplicant.
     * Move to the next state if possible.
     *
     * @throws StateMachineException if authentication protocol is violated
     */
    public void requestAccess() throws StateMachineException {
        states[currentState].requestAccess();
        //move to the next state
        next(TRANSITION_REQUEST_ACCESS);
    }

    /**
     * RADIUS has accepted the identification.
     * Move to the next state if possible.
     *
     * @throws StateMachineException if authentication protocol is violated
     */
    public void authorizeAccess() throws StateMachineException {
        states[currentState].radiusAccepted();
        //move to the next state
        next(TRANSITION_AUTHORIZE_ACCESS);

        if (voltService != null) {
            voltService.addTenant(
                    VoltTenant.builder()
                            .withHumanReadableName("VCPE-" + this.identifier)
                            .withId(this.identifier)
                            .withProviderService(1)
                            .withServiceSpecificId(String.valueOf(this.identifier))
                            .withPort(this.supplicantConnectpoint)
                            .withVlanId(String.valueOf(this.vlanId)).build());
        }

        deleteIdentifier();
    }

    /**
     * RADIUS has denied the identification.
     * Move to the next state if possible.
     *
     * @throws StateMachineException if authentication protocol is violated
     */
    public void denyAccess() throws StateMachineException {
        states[currentState].radiusDenied();
        //move to the next state
        next(TRANSITION_DENY_ACCESS);
        deleteIdentifier();
    }

    /**
     * Logoff request has been requested.
     * Move to the next state if possible.
     *
     * @throws StateMachineException if authentication protocol is violated
     */
    public void logoff() throws StateMachineException {
        states[currentState].logoff();
        //move to the next state
        next(TRANSITION_LOGOFF);
    }

    /**
     * Gets the current state.
     *
     * @return The current state. Could be STATE_IDLE, STATE_STARTED, STATE_PENDING, STATE_AUTHORIZED,
     * STATE_UNAUTHORIZED.
     */
    public int state() {
        return currentState;
    }

    @Override
    public String toString() {
        return ("sessionId: " + this.sessionId) + "\t" + ("identifier: " + this.identifier) + "\t" +
                ("state: " + this.currentState);
    }

    abstract class State {
        private final Logger log = getLogger(getClass());

        private String name = "State";

        public void start() throws StateMachineInvalidTransitionException {
            log.warn("START transition from this state is not allowed.");
        }

        public void requestAccess() throws StateMachineInvalidTransitionException {
            log.warn("REQUEST ACCESS transition from this state is not allowed.");
        }

        public void radiusAccepted() throws StateMachineInvalidTransitionException {
            log.warn("AUTHORIZE ACCESS transition from this state is not allowed.");
        }

        public void radiusDenied() throws StateMachineInvalidTransitionException {
            log.warn("DENY ACCESS transition from this state is not allowed.");
        }

        public void logoff() throws StateMachineInvalidTransitionException {
            log.warn("LOGOFF transition from this state is not allowed.");
        }
    }

    /**
     * Idle state: supplicant is logged of from the network.
     */
    class Idle extends State {
        private final Logger log = getLogger(getClass());
        private String name = "IDLE_STATE";

        public void start() {
            log.info("Moving from IDLE state to STARTED state.");
        }
    }

    /**
     * Started state: supplicant has entered the network and informed the authenticator.
     */
    class Started extends State {
        private final Logger log = getLogger(getClass());
        private String name = "STARTED_STATE";

        public void requestAccess() {
            log.info("Moving from STARTED state to PENDING state.");
        }
    }

    /**
     * Pending state: supplicant has been identified by the authenticator but has not access yet.
     */
    class Pending extends State {
        private final Logger log = getLogger(getClass());
        private String name = "PENDING_STATE";

        public void radiusAccepted() {
            log.info("Moving from PENDING state to AUTHORIZED state.");
        }

        public void radiusDenied() {
            log.info("Moving from PENDING state to UNAUTHORIZED state.");
        }
    }

    /**
     * Authorized state: supplicant port has been accepted, access is granted.
     */
    class Authorized extends State {
        private final Logger log = getLogger(getClass());
        private String name = "AUTHORIZED_STATE";

        public void logoff() {

            log.info("Moving from AUTHORIZED state to IDLE state.");
        }
    }

    /**
     * Unauthorized state: supplicant port has been rejected, access is denied.
     */
    class Unauthorized extends State {
        private final Logger log = getLogger(getClass());
        private String name = "UNAUTHORIZED_STATE";

        public void logoff() {
            log.info("Moving from UNAUTHORIZED state to IDLE state.");
        }
    }


}
