package org.onlab.onos.of.controller.impl.internal;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.onrc.onos.of.ctl.IOFSwitch;
import net.onrc.onos.of.ctl.Role;
import org.onlab.onos.of.controller.impl.debugcounter.DebugCounter;
import org.onlab.onos.of.controller.impl.debugcounter.IDebugCounterService;
import org.onlab.onos.of.controller.impl.internal.OFChannelHandler.RoleRecvStatus;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFHelloElem;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRole;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFSetConfig;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U32;

/**
 * Channel handler deals with the switch connection and dispatches
 * switch messages to the appropriate locations. These Unit Testing cases
 * test the channeler state machine and role changer. In the first release,
 * we will focus on OF version 1.0. we will add the testing case for
 * version 1.3 later.
 */
public class OFChannelHandlerTest {
    private Controller controller;
    private IDebugCounterService debugCounterService;
    private OFChannelHandler handler;
    private Channel channel;
    private ChannelHandlerContext ctx;
    private MessageEvent messageEvent;
    private ChannelStateEvent channelStateEvent;
    private ChannelPipeline pipeline;
    private Capture<ExceptionEvent> exceptionEventCapture;
    private Capture<List<OFMessage>> writeCapture;
    private OFFeaturesReply featuresReply;
    private Set<Integer> seenXids = null;
    private IOFSwitch swImplBase;
    private OFVersion ofVersion = OFVersion.OF_10;
    private OFFactory factory13;
    private OFFactory factory10;
    private OFFactory factory;

    @Before
    public void setUp() throws Exception {
        controller = createMock(Controller.class);
        ctx = createMock(ChannelHandlerContext.class);
        channelStateEvent = createMock(ChannelStateEvent.class);
        channel = createMock(Channel.class);
        messageEvent = createMock(MessageEvent.class);
        exceptionEventCapture = new Capture<ExceptionEvent>(CaptureType.ALL);
        pipeline = createMock(ChannelPipeline.class);
        writeCapture = new Capture<List<OFMessage>>(CaptureType.ALL);
        swImplBase = createMock(IOFSwitch.class);
        seenXids = null;
        factory13 = OFFactories.getFactory(OFVersion.OF_13);
        factory10 = OFFactories.getFactory(OFVersion.OF_10);
        factory = (ofVersion == OFVersion.OF_13) ? factory13 : factory10;

        // TODO: should mock IDebugCounterService and make sure
        // the expected counters are updated.
        debugCounterService = new DebugCounter();
        Controller.Counters counters =
                new Controller.Counters();
        counters.createCounters(debugCounterService);
        expect(controller.getCounters()).andReturn(counters).anyTimes();
        expect(controller.getOFMessageFactory10()).andReturn(factory10)
            .anyTimes();
        expect(controller.getOFMessageFactory13()).andReturn(factory13)
            .anyTimes();
        expect(controller.addConnectedSwitch(2000, handler)).andReturn(true)
            .anyTimes();
        replay(controller);
        handler = new OFChannelHandler(controller);
        verify(controller);
        reset(controller);

        resetChannel();

        // replay controller. Reset it if you need more specific behavior
        replay(controller);

        // replay switch. Reset it if you need more specific behavior
        replay(swImplBase);

        // Mock ctx and channelStateEvent
        expect(ctx.getChannel()).andReturn(channel).anyTimes();
        expect(channelStateEvent.getChannel()).andReturn(channel).anyTimes();
        replay(ctx, channelStateEvent);

        /* Setup an exception event capture on the channel. Right now
         * we only expect exception events to be send up the channel.
         * However, it's easy to extend to other events if we need it
         */
        pipeline.sendUpstream(capture(exceptionEventCapture));
        expectLastCall().anyTimes();
        replay(pipeline);
        featuresReply = (OFFeaturesReply) buildOFMessage(OFType.FEATURES_REPLY);
    }

    @After
    public void tearDown() {
        /* ensure no exception was thrown */
        if (exceptionEventCapture.hasCaptured()) {
            Throwable ex = exceptionEventCapture.getValue().getCause();
            throw new AssertionError("Unexpected exception: " +
                    ex.getClass().getName() + "(" + ex + ")");
        }
        assertFalse("Unexpected messages have been captured",
                writeCapture.hasCaptured());
        // verify all mocks.
        verify(channel);
        verify(messageEvent);
        verify(controller);
        verify(ctx);
        verify(channelStateEvent);
        verify(pipeline);
        verify(swImplBase);

    }

    /**
     * Reset the channel mock and set basic method call expectations.
     *
     **/
    void resetChannel() {
        reset(channel);
        expect(channel.getPipeline()).andReturn(pipeline).anyTimes();
        expect(channel.getRemoteAddress()).andReturn(null).anyTimes();
    }

    /**
     * reset, setup, and replay the messageEvent mock for the given
     * messages.
     */
    void setupMessageEvent(List<OFMessage> messages) {
        reset(messageEvent);
        expect(messageEvent.getMessage()).andReturn(messages).atLeastOnce();
        replay(messageEvent);
    }

    /**
     * reset, setup, and replay the messageEvent mock for the given
     * messages, mock controller  send message to channel handler.
     *
     * This method will reset, start replay on controller, and then verify
     */
    void sendMessageToHandlerWithControllerReset(List<OFMessage> messages)
            throws Exception {
        verify(controller);
        reset(controller);

        sendMessageToHandlerNoControllerReset(messages);
    }

    /**
     * reset, setup, and replay the messageEvent mock for the given
     * messages, mock controller  send message to channel handler.
     *
     * This method will start replay on controller, and then verify
     */
    void sendMessageToHandlerNoControllerReset(List<OFMessage> messages)
            throws Exception {
        setupMessageEvent(messages);

        expect(controller.addConnectedSwitch(1000, handler))
        .andReturn(true).anyTimes();
        replay(controller);

        handler.messageReceived(ctx, messageEvent);
        verify(controller);
    }

    /**
     * Extract the list of OFMessages that was captured by the Channel.write()
     * capture. Will check that something was actually captured first. We'll
     * collapse the messages from multiple writes into a single list of
     * OFMessages.
     * Resets the channelWriteCapture.
     */
    List<OFMessage> getMessagesFromCapture() {
        List<OFMessage> msgs = new ArrayList<OFMessage>();

        assertTrue("No write on channel was captured",
                writeCapture.hasCaptured());
        List<List<OFMessage>> capturedVals = writeCapture.getValues();

        for (List<OFMessage> oneWriteList: capturedVals) {
            msgs.addAll(oneWriteList);
        }
        writeCapture.reset();
        return msgs;
    }


    /**
     * Verify that the given exception event capture (as returned by
     * getAndInitExceptionCapture) has thrown an exception of the given
     * expectedExceptionClass.
     * Resets the capture
     */
    void verifyExceptionCaptured(
            Class<? extends Throwable> expectedExceptionClass) {
        assertTrue("Excpected exception not thrown",
                exceptionEventCapture.hasCaptured());
        Throwable caughtEx = exceptionEventCapture.getValue().getCause();
        assertEquals(expectedExceptionClass, caughtEx.getClass());
        exceptionEventCapture.reset();
    }

    /**
     * Make sure that the transaction ids in the given messages are
     * not 0 and differ between each other.
     * While it's not a defect per se if the xids are we want to ensure
     * we use different ones for each message we send.
     */
    void verifyUniqueXids(List<OFMessage> msgs) {
        if (seenXids == null) {
            seenXids = new HashSet<Integer>();
        }
        for (OFMessage m: msgs)  {
            int xid = (int) m.getXid();
            assertTrue("Xid in messags is 0", xid != 0);
            assertFalse("Xid " + xid + " has already been used",
                    seenXids.contains(xid));
            seenXids.add(xid);
        }
    }



    public void testInitState() throws Exception {
        OFMessage m = buildOFMessage(OFType.HELLO);

        expect(messageEvent.getMessage()).andReturn(null);
        replay(channel, messageEvent);

        // We don't expect to receive /any/ messages in init state since
        // channelConnected moves us to a different state
        sendMessageToHandlerWithControllerReset(Collections.singletonList(m));

        verifyExceptionCaptured(SwitchStateException.class);
        assertEquals(OFChannelHandler.ChannelState.INIT,
                handler.getStateForTesting());
    }

    /**
     * move the channel from scratch to WAIT_HELLO state.
     *
     */
    @Test
    public void moveToWaitHello() throws Exception {
        resetChannel();
        channel.write(capture(writeCapture));
        expectLastCall().andReturn(null).once();
        replay(channel);
        // replay unused mocks
        replay(messageEvent);

        handler.channelConnected(ctx, channelStateEvent);

        List<OFMessage> msgs = getMessagesFromCapture();
        assertEquals(1, msgs.size());
        assertEquals(OFType.HELLO, msgs.get(0).getType());
        assertEquals(OFChannelHandler.ChannelState.WAIT_HELLO,
                handler.getStateForTesting());
        //Should verify that the Hello received from the controller
        //is ALWAYS OF1.3 hello regardless of the switch version
        assertEquals(OFVersion.OF_13, msgs.get(0).getVersion());
        verifyUniqueXids(msgs);
    }


    /**
     * Move the channel from scratch to WAIT_FEATURES_REPLY state.
     * Builds on moveToWaitHello().
     * adds testing for WAIT_HELLO state.
     */
    @Test
    public void moveToWaitFeaturesReply() throws Exception {
        moveToWaitHello();
        resetChannel();
        channel.write(capture(writeCapture));
        expectLastCall().andReturn(null).atLeastOnce();
        replay(channel);

        OFMessage hello = buildOFMessage(OFType.HELLO);
        sendMessageToHandlerWithControllerReset(Collections.singletonList(hello));

        List<OFMessage> msgs = getMessagesFromCapture();
        assertEquals(1, msgs.size());
        assertEquals(OFType.FEATURES_REQUEST, msgs.get(0).getType());
        if (ofVersion == OFVersion.OF_10) {
            assertEquals(OFVersion.OF_10, msgs.get(0).getVersion());
        }
        verifyUniqueXids(msgs);

        assertEquals(OFChannelHandler.ChannelState.WAIT_FEATURES_REPLY,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to WAIT_CONFIG_REPLY state.
     * Builds on moveToWaitFeaturesReply.
     * adds testing for WAIT_FEATURES_REPLY state.
     */
    @Test
    public void moveToWaitConfigReply() throws Exception {
        moveToWaitFeaturesReply();

        resetChannel();
        channel.write(capture(writeCapture));
        expectLastCall().andReturn(null).atLeastOnce();
        replay(channel);

        sendMessageToHandlerWithControllerReset(Collections.<OFMessage>singletonList(featuresReply));
        List<OFMessage> msgs = getMessagesFromCapture();
        assertEquals(3, msgs.size());
        assertEquals(OFType.SET_CONFIG, msgs.get(0).getType());
        OFSetConfig sc = (OFSetConfig) msgs.get(0);
        assertEquals((short) 0xffff, sc.getMissSendLen());
        assertEquals(OFType.BARRIER_REQUEST, msgs.get(1).getType());
        assertEquals(OFType.GET_CONFIG_REQUEST, msgs.get(2).getType());
        verifyUniqueXids(msgs);
        assertEquals(OFChannelHandler.ChannelState.WAIT_CONFIG_REPLY,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to WAIT_DESCRIPTION_STAT_REPLY state.
     * Builds on moveToWaitConfigReply().
     * adds testing for WAIT_CONFIG_REPLY state.
     */
    @Test
    public void moveToWaitDescriptionStatReply() throws Exception {
        moveToWaitConfigReply();
        resetChannel();
        channel.write(capture(writeCapture));
        expectLastCall().andReturn(null).atLeastOnce();
        replay(channel);

        OFGetConfigReply cr = (OFGetConfigReply) buildOFMessage(OFType.GET_CONFIG_REPLY);

        sendMessageToHandlerWithControllerReset(Collections.<OFMessage>singletonList(cr));

        List<OFMessage> msgs = getMessagesFromCapture();
        assertEquals(1, msgs.size());
        assertEquals(OFType.STATS_REQUEST, msgs.get(0).getType());
        OFStatsRequest<?> sr = (OFStatsRequest<?>) msgs.get(0);
        assertEquals(OFStatsType.DESC, sr.getStatsType());
        verifyUniqueXids(msgs);
        assertEquals(OFChannelHandler.ChannelState.WAIT_DESCRIPTION_STAT_REPLY,
                handler.getStateForTesting());
    }


    private OFStatsReply createDescriptionStatsReply() throws IOException {
        OFStatsReply sr = (OFStatsReply) buildOFMessage(OFType.STATS_REPLY);
        return sr;
    }

    /**
     * Move the channel from scratch to WAIT_INITIAL_ROLE state.
     * for a switch that does not have a sub-handshake.
     * Builds on moveToWaitDescriptionStatReply().
     * adds testing for WAIT_DESCRIPTION_STAT_REPLY state.
     *
     */
    @Test
    public void moveToWaitInitialRole()
            throws Exception {
        moveToWaitDescriptionStatReply();

        long xid = 2000;

        // build the stats reply
        OFStatsReply sr = createDescriptionStatsReply();

        resetChannel();
        replay(channel);

        setupMessageEvent(Collections.<OFMessage>singletonList(sr));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();
        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();
        controller.submitRegistryRequest(1000);
        expectLastCall().once();
        replay(controller);

        //TODO: With the description stats message you are sending in the test,
        //you will end up with an OFSwitchImplBase object
        //which by default does NOT support the nicira role messages.
        //If you wish to test the case where Nicira role messages are supported,
        //then make a comment here that states that this is different
        //from the default behavior of switchImplbase /or/
        //send the right desc-stats (for example send what is expected from OVS 1.0)

        if (ofVersion == OFVersion.OF_10) {
            expect(swImplBase.getAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE))
            .andReturn(true).once();

            swImplBase.write(capture(writeCapture));
            expectLastCall().anyTimes();
        }

        swImplBase.setOFVersion(ofVersion);
        expectLastCall().once();
        swImplBase.setConnected(true);
        expectLastCall().once();
        swImplBase.setChannel(channel);
        expectLastCall().once();
        swImplBase.setDebugCounterService(controller.getDebugCounter());
        expectLastCall().once();
        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        swImplBase.setRole(Role.EQUAL);
        expectLastCall().once();

        expect(swImplBase.getNextTransactionId())
        .andReturn((int) xid).anyTimes();
        expect(swImplBase.getId())
        .andReturn(1000L).once();

        swImplBase.setFeaturesReply(featuresReply);
        expectLastCall().once();
        swImplBase.setPortDescReply((OFPortDescStatsReply) null);
        replay(swImplBase);

        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);

        List<OFMessage> msgs = getMessagesFromCapture();
        assertEquals(1, msgs.size());
        assertEquals(OFType.EXPERIMENTER, msgs.get(0).getType());
        verifyNiciraMessage((OFExperimenter) msgs.get(0));

        verify(controller);
        assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to.
     *  WAIT_SWITCH_DRIVER_SUB_HANDSHAKE state.
     * Builds on moveToWaitInitialRole().
     */
    @Test
    public void moveToWaitSubHandshake()
            throws Exception {
        moveToWaitInitialRole();

        int xid = 2000;
        resetChannel();
        replay(channel);

        reset(swImplBase);
        // Set the role
        setupSwitchSendRoleRequestAndVerify(true, xid, Role.SLAVE);
        assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                handler.getStateForTesting());

        // build the stats reply
        OFStatsReply sr = createDescriptionStatsReply();
        OFMessage rr = getRoleReply(xid, Role.SLAVE);
        setupMessageEvent(Collections.<OFMessage>singletonList(rr));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();
        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();

        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        swImplBase.setRole(Role.SLAVE);
        expectLastCall().once();
        expect(swImplBase.getNextTransactionId())
        .andReturn(xid).anyTimes();
        swImplBase.startDriverHandshake();
        expectLastCall().once();

        //when this flag is false, state machine will move to
        //WAIT_SWITCH_DRIVER_SUB_HANDSHAKE state
        expect(swImplBase.isDriverHandshakeComplete())
        .andReturn(false).once();

        replay(swImplBase);

        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);

        assertEquals(OFChannelHandler.ChannelState.WAIT_SWITCH_DRIVER_SUB_HANDSHAKE,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to WAIT_INITIAL_ROLE state,
     * then move the channel to EQUAL state based on the switch Role.
     * This test basically test the switch with role support.
     * Builds on moveToWaitInitialRole().
     *
     * In WAIT_INITIAL_ROLE state, when any messages (except ECHO_REQUEST
     * and PORT_STATUS), state machine will transit to MASTER or
     * EQUAL state based on the switch role.
     */
    @Test
    public void moveToSlaveWithHandshakeComplete()
            throws Exception {

        moveToWaitInitialRole();

        int xid = 2000;
        resetChannel();
        replay(channel);

        reset(swImplBase);
        // Set the role
        setupSwitchSendRoleRequestAndVerify(true, xid, Role.SLAVE);
        assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                handler.getStateForTesting());

        // build the stats reply
        OFStatsReply sr = createDescriptionStatsReply();
        OFMessage rr = getRoleReply(xid, Role.SLAVE);
        setupMessageEvent(Collections.<OFMessage>singletonList(rr));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();

        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();

        expect(controller.addActivatedEqualSwitch(1000, swImplBase))
        .andReturn(true).once();
        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        //consult the role in sw to determine the next state.
        //in this testing case, we are testing that channel handler
        // will move to EQUAL state when switch role is in SLAVE.
        expect(swImplBase.getRole()).andReturn(Role.SLAVE).once();
        swImplBase.setRole(Role.SLAVE);
        expectLastCall().once();

        expect(swImplBase.getNextTransactionId())
        .andReturn(xid).anyTimes();
        expect(swImplBase.getId())
        .andReturn(1000L).once();
        swImplBase.startDriverHandshake();
        expectLastCall().once();

        //when this flag is true, don't need to move interim state
        //WAIT_SWITCH_DRIVER_SUB_HANDSHAKE. channel handler will
        //move to corresponding state after consulting the role in sw
        //This is essentially the same test as the one above,
        //except for this line
        expect(swImplBase.isDriverHandshakeComplete())
        .andReturn(true).once();

        replay(swImplBase);

        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);

        assertEquals(OFChannelHandler.ChannelState.EQUAL,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to WAIT_INITIAL_ROLE state,
     * then to MASTERL state based on the switch Role.
     * This test basically test the switch with role support.
     * Builds on moveToWaitInitialRole().
     *
     * In WAIT_INITIAL_ROLE state, when any messages (except ECHO_REQUEST
     * and PORT_STATUS), state machine will transit to MASTER or
     * EQUAL state based on the switch role.
     */
    @Test
    public void moveToMasterWithHandshakeComplete()
            throws Exception {

        moveToWaitInitialRole();

        int xid = 2000;
        resetChannel();
        replay(channel);

        reset(swImplBase);
        // Set the role
        setupSwitchSendRoleRequestAndVerify(true, xid, Role.MASTER);
        assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                handler.getStateForTesting());

        // build the stats reply
        OFStatsReply sr = createDescriptionStatsReply();
        OFMessage rr = getRoleReply(xid, Role.MASTER);
        setupMessageEvent(Collections.<OFMessage>singletonList(rr));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();

        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();

        expect(controller.addActivatedMasterSwitch(1000, swImplBase))
        .andReturn(true).once();
        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        expect(swImplBase.getRole()).andReturn(Role.MASTER).once();
        swImplBase.setRole(Role.MASTER);
        expectLastCall().once();

        expect(swImplBase.getNextTransactionId())
        .andReturn(xid).anyTimes();
        expect(swImplBase.getId())
        .andReturn(1000L).once();
        swImplBase.startDriverHandshake();
        expectLastCall().once();
        expect(swImplBase.isDriverHandshakeComplete())
        .andReturn(true).once();

        replay(swImplBase);


        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);

        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to
     *  WAIT_SWITCH_DRIVER_SUB_HANDSHAKE state.
     * Builds on moveToWaitSubHandshake().
     */
    @Test
    public void moveToEqualViaWaitSubHandshake()
            throws Exception {
        moveToWaitSubHandshake();

        long xid = 2000;
        resetChannel();
        replay(channel);

        // build the stats reply
        OFStatsReply sr = createDescriptionStatsReply();

        setupMessageEvent(Collections.<OFMessage>singletonList(sr));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();
        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();

        expect(controller.addActivatedEqualSwitch(1000, swImplBase))
        .andReturn(true).once();
        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        expect(swImplBase.getRole()).andReturn(Role.SLAVE).once();
        expect(swImplBase.getNextTransactionId())
        .andReturn((int) xid).anyTimes();
        expect(swImplBase.getId())
        .andReturn(1000L).once();

        swImplBase.processDriverHandshakeMessage(sr);
        expectLastCall().once();
        expect(swImplBase.isDriverHandshakeComplete())
        .andReturn(true).once();

        replay(swImplBase);

        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);

        assertEquals(OFChannelHandler.ChannelState.EQUAL,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to
     *  WAIT_SWITCH_DRIVER_SUB_HANDSHAKE state.
     * Builds on moveToWaitSubHandshake().
     */
    @Test
    public void moveToMasterViaWaitSubHandshake()
            throws Exception {
        moveToWaitSubHandshake();

        long xid = 2000;
        resetChannel();
        replay(channel);

        // In this state, any messages except echo request, port status and
        // error go to the switch sub driver handshake. Once the switch reports
        // that its sub driver handshake is complete (#isDriverHandshakeComplete
        // return true) then the channel handle consults the switch role and
        // moves the state machine to the appropriate state (MASTER or EQUALS).
        // In this test we expect the state machine to end up in MASTER state.
        OFStatsReply sr = createDescriptionStatsReply();

        setupMessageEvent(Collections.<OFMessage>singletonList(sr));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();

        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();
        expect(controller.addActivatedMasterSwitch(1000, swImplBase))
        .andReturn(true).once();
        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        expect(swImplBase.getRole()).andReturn(Role.MASTER).once();
        expect(swImplBase.getNextTransactionId())
        .andReturn((int) xid).anyTimes();
        expect(swImplBase.getId())
        .andReturn(1000L).once();

        swImplBase.processDriverHandshakeMessage(sr);
        expectLastCall().once();
        expect(swImplBase.isDriverHandshakeComplete())
        .andReturn(true).once();

        replay(swImplBase);

        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);
        verify(controller);
        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());
    }

    /**
     * Test the behavior in WAIT_SWITCH_DRIVER_SUB_HANDSHAKE state.
     * ECHO_REQUEST message received case.
     */
    @Test
    public void testWaitSwitchDriverSubhandshake() throws Exception {
        moveToWaitSubHandshake();

        long xid = 2000;
        resetChannel();
        channel.write(capture(writeCapture));
        expectLastCall().andReturn(null).atLeastOnce();
        replay(channel);

        OFMessage er = buildOFMessage(OFType.ECHO_REQUEST);

        setupMessageEvent(Collections.<OFMessage>singletonList(er));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFMessageFactory10()).andReturn(factory10);
        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();

        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        expect(swImplBase.getNextTransactionId())
        .andReturn((int) xid).anyTimes();

        replay(swImplBase);

        handler.messageReceived(ctx, messageEvent);

        List<OFMessage> msgs = getMessagesFromCapture();
        assertEquals(1, msgs.size());
        assertEquals(OFType.ECHO_REPLY, msgs.get(0).getType());
        verifyUniqueXids(msgs);
        assertEquals(OFChannelHandler.ChannelState.WAIT_SWITCH_DRIVER_SUB_HANDSHAKE,
                handler.getStateForTesting());
    }

    /**
     * Helper.
     * Verify that the given OFMessage is a correct Nicira RoleRequest message.
     */
    private void verifyNiciraMessage(OFExperimenter ofMessage) {

        int vendor = (int) ofMessage.getExperimenter();
        assertEquals(vendor, 0x2320); // magic number representing nicira
    }

    /**
     * Setup the mock switch and write capture for a role request, set the
     * role and verify mocks.
     * @param supportsNxRole whether the switch supports role request messages
     * to setup the attribute. This must be null (don't yet know if roles
     * supported: send to check) or true.
     * @param xid The xid to use in the role request
     * @param role The role to send
     * @throws IOException
     */
    private void setupSwitchSendRoleRequestAndVerify(Boolean supportsNxRole,
            int xid,
            Role role) throws IOException {

        RoleRecvStatus expectation = RoleRecvStatus.MATCHED_SET_ROLE;

        expect(swImplBase.getAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE))
        .andReturn(supportsNxRole).atLeastOnce();

        if (supportsNxRole != null && supportsNxRole) {
            expect(swImplBase.getNextTransactionId()).andReturn(xid).once();
            swImplBase.write(capture(writeCapture));
            expectLastCall().anyTimes();
        }
        replay(swImplBase);

        handler.sendRoleRequest(role, expectation);

        if (supportsNxRole != null && supportsNxRole) {
            List<OFMessage> msgs = getMessagesFromCapture();
            assertEquals(1, msgs.size());
            verifyNiciraMessage((OFExperimenter) msgs.get(0));
        }
    }

    /**
     * Setup the mock switch for a role change request where the switch
     * does not support roles.
     *
     * Needs to verify and reset the controller since we need to set
     * an expectation
     */
    private void setupSwitchRoleChangeUnsupported(int xid,
            Role role) {
        boolean supportsNxRole = false;
        RoleRecvStatus expectation = RoleRecvStatus.NO_REPLY;
        reset(swImplBase);
        expect(swImplBase.getAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE))
        .andReturn(supportsNxRole).atLeastOnce();
        // TODO: hmmm. While it's not incorrect that we set the attribute
        // again it looks odd. Maybe change
        swImplBase.setAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE, supportsNxRole);
        expectLastCall().anyTimes();

        replay(swImplBase);

        handler.sendRoleRequest(role, expectation);

        verify(swImplBase);
    }

    /*
     * Return a Nicira RoleReply message for the given role.
     */
    private OFMessage getRoleReply(long xid, Role role) {

        OFNiciraControllerRole nr = null;

        switch(role) {
        case MASTER:
            nr = OFNiciraControllerRole.ROLE_MASTER;
            break;
        case EQUAL:
            nr = OFNiciraControllerRole.ROLE_SLAVE;
            break;
        case SLAVE:
            nr = OFNiciraControllerRole.ROLE_SLAVE;
            break;
        default: //handled below
        }
        OFMessage m = factory10.buildNiciraControllerRoleReply()
                .setRole(nr)
                .setXid(xid)
                .build();
        return m;
    }

    /**
     * Move the channel from scratch to MASTER state.
     * Builds on moveToWaitInitialRole().
     * adds testing for WAIT_INITAL_ROLE state.
     *
     * This method tests the case that the switch does NOT support roles.
     * In ONOS if the switch-driver says that nicira-role messages are not
     * supported, then ONOS does NOT send role-request messages
     * (see handleUnsentRoleMessage())
     */
    @Test
    public void testInitialMoveToMasterNoRole() throws Exception {
        int xid = 43;
        // first, move us to WAIT_INITIAL_ROLE_STATE

        moveToWaitInitialRole();
        assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                handler.getStateForTesting());

        OFStatsReply sr = createDescriptionStatsReply();

        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();

        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();

        expect(controller.addActivatedMasterSwitch(1000, swImplBase))
        .andReturn(true).once();
        replay(controller);

        reset(swImplBase);
        swImplBase.setRole(Role.MASTER);
        expectLastCall().once();
        swImplBase.startDriverHandshake();
        expectLastCall().once();
        expect(swImplBase.isDriverHandshakeComplete())
        .andReturn(true).once();
        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        expect(swImplBase.getRole()).andReturn(Role.MASTER).once();

        expect(swImplBase.getId())
        .andReturn(1000L).once();
        // Set the role
        setupSwitchSendRoleRequestAndVerify(false, xid, Role.MASTER);

        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());
    }

    /**
     * Move the channel from scratch to WAIT_INITIAL_ROLE state.
     * Builds on moveToWaitInitialRole().
     * adds testing for WAIT_INITAL_ROLE state
     *
     * We let the initial role request time out. Role support should be
     * disabled but the switch should be activated.
     */
    /* TBD
        @Test
        public void testInitialMoveToMasterTimeout() throws Exception {
            int timeout = 50;
            handler.useRoleChangerWithOtherTimeoutForTesting(timeout);
            int xid = 4343;

            // first, move us to WAIT_INITIAL_ROLE_STATE

            moveToWaitInitialRole();
            assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                    handler.getStateForTesting());

            // prepare mocks and inject the role reply message
            reset(swImplBase);
            // Set the role
            swImplBase.setRole(Role.MASTER);
            expectLastCall().once();
            swImplBase.startDriverHandshake();
            expectLastCall().once();
            expect(swImplBase.isDriverHandshakeComplete())
            .andReturn(false).once();
            if (ofVersion == OFVersion.OF_10) {
                expect(swImplBase.getAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE))
                 .andReturn(true).once();

                swImplBase.write(capture(writeCapture),
                        EasyMock.<FloodlightContext>anyObject());
                expectLastCall().anyTimes();
             }
            expect(swImplBase.getNextTransactionId()).andReturn(xid).once();

            assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                    handler.getStateForTesting());

            // Set the role
            setupSwitchSendRoleRequestAndVerify(null, xid, Role.MASTER);
            assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                    handler.getStateForTesting());

            OFMessage m = buildOFMessage(OFType.ECHO_REPLY);

            setupMessageEvent(Collections.<OFMessage>singletonList(m));

            Thread.sleep(timeout+5);

            verify(controller);
            reset(controller);

            expect(controller.addActivatedMasterSwitch(1000, swImplBase))
            .andReturn(true).once();
            controller.flushAll();
            expectLastCall().once();

            replay(controller);

            handler.messageReceived(ctx, messageEvent);

            assertEquals(OFChannelHandler.ChannelState.MASTER,
                    handler.getStateForTesting());

        }

     */
    /**
     * Move the channel from scratch to SLAVE state.
     * Builds on doMoveToWaitInitialRole().
     * adds testing for WAIT_INITAL_ROLE state
     *
     * This method tests the case that the switch does NOT support roles.
     * The channel handler still needs to send the initial request to find
     * out that whether the switch supports roles.
     *
     */
    @Test
    public void testInitialMoveToSlaveNoRole() throws Exception {
        int xid = 44;
        // first, move us to WAIT_INITIAL_ROLE_STATE
        moveToWaitInitialRole();
        assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                handler.getStateForTesting());

        reset(swImplBase);
        // Set the role
        setupSwitchSendRoleRequestAndVerify(false, xid, Role.SLAVE);
        assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                handler.getStateForTesting());

    }

    /**
     * Move the channel from scratch to SLAVE state.
     * Builds on doMoveToWaitInitialRole().
     * adds testing for WAIT_INITAL_ROLE state
     *
     * We let the initial role request time out. The switch should be
     * disconnected
     */
    /* TBD
        @Test
        public void testInitialMoveToSlaveTimeout() throws Exception {
            int timeout = 50;
            handler.useRoleChangerWithOtherTimeoutForTesting(timeout);
            int xid = 4444;

            // first, move us to WAIT_INITIAL_ROLE_STATE
            moveToWaitInitialRole();
            assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                    handler.getStateForTesting());

            // Set the role
            setupSwitchSendRoleRequestAndVerify(null, xid, Role.SLAVE);
            assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                    handler.getStateForTesting());

            // prepare mocks and inject the role reply message
            reset(sw);
            sw.setAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE, false);
            expectLastCall().once();
            sw.setRole(Role.SLAVE);
            expectLastCall().once();
            sw.disconnectSwitch(); // Make sure we disconnect
            expectLastCall().once();
            replay(sw);

            OFMessage m = buildOFMessage(OFType.ECHO_REPLY);

            Thread.sleep(timeout+5);

            sendMessageToHandlerWithControllerReset(Collections.singletonList(m));
        }

     */
    /**
     * Move channel from scratch to WAIT_INITIAL_STATE, then MASTER,
     * then SLAVE for cases where the switch does not support roles.
     * I.e., the final SLAVE transition should disconnect the switch.
     */
    @Test
    public void testNoRoleInitialToMasterToSlave() throws Exception {
        int xid = 46;
        reset(swImplBase);
        replay(swImplBase);

        reset(controller);
        replay(controller);

        // First, lets move the state to MASTER without role support
        testInitialMoveToMasterNoRole();
        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());

        // try to set master role again. should be a no-op
        setupSwitchRoleChangeUnsupported(xid, Role.MASTER);

        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());

        setupSwitchRoleChangeUnsupported(xid, Role.SLAVE);
        //switch does not support role message. there is no role set
        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());

    }

    /**
     * Move the channel to MASTER state.
     * Expects that the channel is in MASTER or SLAVE state.
     *
     */
    public void changeRoleToMasterWithRequest() throws Exception {
        int xid = 4242;

        assertTrue("This method can only be called when handler is in " +
                "MASTER or SLAVE role", handler.isHandshakeComplete());

        reset(swImplBase);
        reset(controller);
        // Set the role
        setupSwitchSendRoleRequestAndVerify(true, xid, Role.MASTER);

        // prepare mocks and inject the role reply message

        reset(controller);
        expect(controller.addActivatedMasterSwitch(1000, swImplBase))
        .andReturn(true).once();
        OFMessage reply = getRoleReply(xid, Role.MASTER);

        // sendMessageToHandler will verify and rest controller mock

        OFStatsReply sr = createDescriptionStatsReply();
        setupMessageEvent(Collections.<OFMessage>singletonList(reply));

        // mock controller
        reset(controller);
        reset(swImplBase);

        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();

        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();
        controller.transitionToMasterSwitch(1000);
        expectLastCall().once();

        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        expect(swImplBase.getRole()).andReturn(Role.EQUAL).atLeastOnce();
        expect(swImplBase.getNextTransactionId())
        .andReturn(xid).anyTimes();
        expect(swImplBase.getId())
        .andReturn(1000L).once();

        swImplBase.setRole(Role.MASTER);
        expectLastCall().once();
        replay(swImplBase);

        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);

        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());
    }

    /**
     * Move the channel to SLAVE state.
     * Expects that the channel is in MASTER or SLAVE state.
     *
     */
    public void changeRoleToSlaveWithRequest() throws Exception {
        int xid = 2323;

        assertTrue("This method can only be called when handler is in " +
                "MASTER or SLAVE role", handler.isHandshakeComplete());

        // Set the role
        reset(controller);
        reset(swImplBase);

        swImplBase.write(capture(writeCapture));
        expectLastCall().anyTimes();

        expect(swImplBase.getNextTransactionId())
        .andReturn(xid).anyTimes();


        if (ofVersion == OFVersion.OF_10) {
            expect(swImplBase.getAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE))
            .andReturn(true).once();

            swImplBase.write(capture(writeCapture));
            expectLastCall().anyTimes();
        }
        replay(swImplBase);

        handler.sendRoleRequest(Role.SLAVE, RoleRecvStatus.MATCHED_SET_ROLE);

        List<OFMessage> msgs = getMessagesFromCapture();
        assertEquals(1, msgs.size());
        verifyNiciraMessage((OFExperimenter) msgs.get(0));


        OFMessage reply = getRoleReply(xid, Role.SLAVE);
        OFStatsReply sr = createDescriptionStatsReply();
        setupMessageEvent(Collections.<OFMessage>singletonList(reply));

        // mock controller
        reset(controller);
        reset(swImplBase);

        controller.transitionToEqualSwitch(1000);
        expectLastCall().once();
        expect(controller.getOFSwitchInstance((OFDescStatsReply) sr, ofVersion))
        .andReturn(swImplBase).anyTimes();

        expect(controller.getDebugCounter())
        .andReturn(debugCounterService).anyTimes();

        replay(controller);

        expect(swImplBase.getStringId())
        .andReturn(null).anyTimes();
        expect(swImplBase.getRole()).andReturn(Role.MASTER).atLeastOnce();
        expect(swImplBase.getNextTransactionId())
        .andReturn(xid).anyTimes();

        // prepare mocks and inject the role reply message
        swImplBase.setRole(Role.SLAVE);
        expectLastCall().once();
        expect(swImplBase.getId())
        .andReturn(1000L).once();
        replay(swImplBase);

        handler.messageReceived(ctx, messageEvent);

        assertEquals(OFChannelHandler.ChannelState.EQUAL,
                handler.getStateForTesting());
    }

    @Test
    public void testMultiRoleChange1() throws Exception {
        moveToMasterWithHandshakeComplete();
        changeRoleToMasterWithRequest();
        changeRoleToSlaveWithRequest();
        changeRoleToSlaveWithRequest();
        changeRoleToMasterWithRequest();
        changeRoleToSlaveWithRequest();
    }

    @Test
    public void testMultiRoleChange2() throws Exception {
        moveToSlaveWithHandshakeComplete();
        changeRoleToMasterWithRequest();
        changeRoleToSlaveWithRequest();
        changeRoleToSlaveWithRequest();
        changeRoleToMasterWithRequest();
        changeRoleToSlaveWithRequest();
    }

    /**
     * Start from scratch and reply with an unexpected error to the role
     * change request.
     * Builds on doMoveToWaitInitialRole()
     * adds testing for WAIT_INITAL_ROLE state
     */
    /* TBD
        @Test
        public void testInitialRoleChangeOtherError() throws Exception {
            int xid = 4343;
            // first, move us to WAIT_INITIAL_ROLE_STATE
            moveToWaitInitialRole();
            assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                    handler.getStateForTesting());

            reset(swImplBase);
            // Set the role
            setupSwitchSendRoleRequestAndVerify(true, xid, Role.MASTER);
            assertEquals(OFChannelHandler.ChannelState.WAIT_INITIAL_ROLE,
                    handler.getStateForTesting());


            // FIXME: shouldn't use ordinal(), but OFError is broken

            OFMessage err = factory.errorMsgs().buildBadActionErrorMsg()
                    .setCode(OFBadActionCode.BAD_LEN)
                    .setXid(2000)
                    .build();
            verify(swImplBase);
            reset(swImplBase);
            replay(swImplBase);
            sendMessageToHandlerWithControllerReset(Collections.singletonList(err));

            verifyExceptionCaptured(SwitchStateException.class);
        }
     */
    /**
     * Test dispatch of messages while in MASTER role.
     */
    @Test
    public void testMessageDispatchMaster() throws Exception {

        moveToMasterWithHandshakeComplete();

        // Send packet in. expect dispatch
        OFPacketIn pi = (OFPacketIn)
                buildOFMessage(OFType.PACKET_IN);
        setupMessageEvent(Collections.<OFMessage>singletonList(pi));

        reset(swImplBase);
        swImplBase.handleMessage(pi);
        expectLastCall().once();
        replay(swImplBase);
        // send the description stats reply
        handler.messageReceived(ctx, messageEvent);

        assertEquals(OFChannelHandler.ChannelState.MASTER,
                handler.getStateForTesting());

        verify(controller);
        // TODO: many more to go
    }

    /**
     * Test port status message handling while MASTER.
     *
     */
    /* Patrick: TBD
        @Test
        public void testPortStatusMessageMaster() throws Exception {
            long dpid = featuresReply.getDatapathId().getLong();
            testInitialMoveToMasterWithRole();
            List<OFPortDesc> ports = new ArrayList<OFPortDesc>();
            // A dummy port.
            OFPortDesc p = factory.buildPortDesc()
                        .setName("Eth1")
                        .setPortNo(OFPort.ofInt(1))
                        .build();
            ports.add(p);

            p.setName("Port1");
            p.setPortNumber((short)1);

            OFPortStatus ps = (OFPortStatus)buildOFMessage(OFType.PORT_STATUS);
            ps.setDesc(p);

            // The events we expect sw.handlePortStatus to return
            // We'll just use the same list for all valid OFPortReasons and add
            // arbitrary events for arbitrary ports that are not necessarily
            // related to the port status message. Our goal
            // here is not to return the correct set of events but the make sure
            // that a) sw.handlePortStatus is called
            //      b) the list of events sw.handlePortStatus returns is sent
            //         as IOFSwitchListener notifications.
            OrderedCollection<PortChangeEvent> events =
                    new LinkedHashSetWrapper<PortChangeEvent>();
            ImmutablePort p1 = ImmutablePort.create("eth1", (short)1);
            ImmutablePort p2 = ImmutablePort.create("eth2", (short)2);
            ImmutablePort p3 = ImmutablePort.create("eth3", (short)3);
            ImmutablePort p4 = ImmutablePort.create("eth4", (short)4);
            ImmutablePort p5 = ImmutablePort.create("eth5", (short)5);
            events.add(new PortChangeEvent(p1, PortChangeType.ADD));
            events.add(new PortChangeEvent(p2, PortChangeType.DELETE));
            events.add(new PortChangeEvent(p3, PortChangeType.UP));
            events.add(new PortChangeEvent(p4, PortChangeType.DOWN));
            events.add(new PortChangeEvent(p5, PortChangeType.OTHER_UPDATE));


            for (OFPortReason reason: OFPortReason.values()) {
                ps.setReason(reason.getReasonCode());

                reset(sw);
                expect(sw.getId()).andReturn(dpid).anyTimes();

                expect(sw.processOFPortStatus(ps)).andReturn(events).once();
                replay(sw);

                reset(controller);
                controller.notifyPortChanged(sw, p1, PortChangeType.ADD);
                controller.notifyPortChanged(sw, p2, PortChangeType.DELETE);
                controller.notifyPortChanged(sw, p3, PortChangeType.UP);
                controller.notifyPortChanged(sw, p4, PortChangeType.DOWN);
                controller.notifyPortChanged(sw, p5, PortChangeType.OTHER_UPDATE);
                sendMessageToHandlerNoControllerReset(
                        Collections.<OFMessage>singletonList(ps));
                verify(sw);
                verify(controller);
            }
        }

     */
    /**
     * Build an OF message.
     * @throws IOException
     */
    private OFMessage buildOFMessage(OFType t) throws IOException {
        OFMessage m = null;
        switch (t) {

        case HELLO:
            // The OF protocol requires us to start things off by sending the highest
            // version of the protocol supported.

            // bitmap represents OF1.0 (ofp_version=0x01) and OF1.3 (ofp_version=0x04)
            // see Sec. 7.5.1 of the OF1.3.4 spec
            if (ofVersion == OFVersion.OF_13) {
                U32 bitmap = U32.ofRaw(0x00000012);
                OFHelloElem hem = factory13.buildHelloElemVersionbitmap()
                    .setBitmaps(Collections.singletonList(bitmap))
                    .build();
                m = factory13.buildHello()
                        .setXid(2000)
                        .setElements(Collections.singletonList(hem))
                        .build();
            } else {
                m = factory10.buildHello()
                    .setXid(2000)
                    .build();
            }
            break;
        case FEATURES_REQUEST:
            m = factory.buildFeaturesRequest()
            .setXid(2000)
            .build();
            break;
        case FEATURES_REPLY:

            m = factory.buildFeaturesReply()
            .setDatapathId(DatapathId.of(1000L))
            .setXid(2000)
            .build();
            break;
        case SET_CONFIG:
            m = factory.buildSetConfig()
            .setMissSendLen((short) 0xffff)
            .setXid(2000)
            .build();
            break;
        case BARRIER_REQUEST:
            m = factory.buildBarrierRequest()
            .setXid(2000)
            .build();
            break;
        case GET_CONFIG_REQUEST:
            m = factory.buildGetConfigRequest()
            .setXid(2000)
            .build();
            break;
        case GET_CONFIG_REPLY:
            m = factory.buildGetConfigReply()
            .setMissSendLen((short) 0xffff)
            .setXid(2000)
            .build();
            break;
        case STATS_REQUEST:
            break;
        case STATS_REPLY:
            m = factory.buildDescStatsReply()
            .setDpDesc("Datapath Description")
            .setHwDesc("Hardware Secription")
            .setMfrDesc("Manufacturer Desctiption")
            .setSerialNum("Serial Number")
            .setSwDesc("Software Desription")
            .build();
            break;
        case ECHO_REQUEST:
            m = factory.buildEchoRequest()
            .setXid(2000)
            .build();
            break;
        case FLOW_REMOVED:
            break;

        case PACKET_IN:
            m = factory.buildPacketIn()
            .setReason(OFPacketInReason.NO_MATCH)
            .setTotalLen(1500)
            .setXid(2000)
            .build();
            break;
        case PORT_STATUS:
            m = factory.buildPortStatus()
            .setXid(2000)
            .build();
            break;

        default:
            m = factory.buildFeaturesRequest()
            .setXid(2000)
            .build();
            break;
        }

        return (m);
    }
}
