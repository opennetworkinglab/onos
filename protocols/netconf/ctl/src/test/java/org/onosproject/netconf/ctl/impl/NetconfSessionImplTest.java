/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.netconf.ctl.impl;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.onosproject.netconf.DatastoreId.CANDIDATE;
import static org.onosproject.netconf.DatastoreId.RUNNING;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.packet.Ip4Address;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Unit tests for NetconfSession.
 * <p>
 * Sets up an SSH Server with Apache SSHD and connects to it using 2 clients
 * Truly verifies that the NETCONF flows are compliant with a NETCONF server.
 */
public class NetconfSessionImplTest {
    private static final Logger log = LoggerFactory
            .getLogger(NetconfSessionImplTest.class);

    private static final String TEST_USERNAME = "netconf";
    private static final String TEST_PASSWORD = "netconf123";
    private static final String TEST_HOSTNAME = "127.0.0.1";

    private static final String TEST_SERFILE =
            System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "testkey.ser";

    private static final String SAMPLE_REQUEST =
            "<some-yang-element xmlns=\"some-namespace\">"
                    + "<some-child-element/>"
                    + "</some-yang-element>";

    private static final String EDIT_CONFIG_REQUEST =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rpc message-id=\"6\"  "
                    + "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
                    + "<edit-config>\n"
                    + "<target><running/></target>\n"
                    + "<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
                    + "<some-yang-element xmlns=\"some-namespace\">"
                    + "<some-child-element/></some-yang-element></config>\n"
                    + "</edit-config>\n"
                    + "</rpc>]]>]]>";

    private static NetconfSession session1;
    private static NetconfSession session2;
    private static NetconfSession session3;
    private static NetconfSession session4;
    private static SshServer sshServerNetconf;

    @BeforeClass
    public static void setUp() throws Exception {
        int portNumber = TestTools.findAvailablePort(50830);
        sshServerNetconf = SshServer.setUpDefaultServer();
        sshServerNetconf.setPasswordAuthenticator(
                new PasswordAuthenticator() {
                    @Override
                    public boolean authenticate(
                            String username,
                            String password,
                            ServerSession session) {
                        return TEST_USERNAME.equals(username) && TEST_PASSWORD.equals(password);
                    }
                });
        sshServerNetconf.setPort(portNumber);
        SimpleGeneratorHostKeyProvider provider = new SimpleGeneratorHostKeyProvider();
        provider.setFile(new File(TEST_SERFILE));
        sshServerNetconf.setKeyPairProvider(provider);
        sshServerNetconf.setSubsystemFactories(
                Arrays.<NamedFactory<Command>>asList(new NetconfSshdTestSubsystem.Factory()));
        sshServerNetconf.open();
        log.info("SSH Server opened on port {}", portNumber);

        NetconfController netconfCtl = new NetconfControllerImpl();
        NetconfControllerImpl.netconfConnectTimeout = NetconfControllerImpl.DEFAULT_CONNECT_TIMEOUT_SECONDS;
        NetconfControllerImpl.netconfIdleTimeout = NetconfControllerImpl.DEFAULT_IDLE_TIMEOUT_SECONDS;
        NetconfControllerImpl.netconfReplyTimeout = NetconfControllerImpl.DEFAULT_REPLY_TIMEOUT_SECONDS;

        NetconfDeviceInfo deviceInfo1 = new NetconfDeviceInfo(
                TEST_USERNAME, TEST_PASSWORD, Ip4Address.valueOf(TEST_HOSTNAME), portNumber);

        session1 = new NetconfSessionImpl(deviceInfo1, ImmutableList.of("urn:ietf:params:netconf:base:1.0"));
        log.info("Started NETCONF Session {} with test SSHD server in Unit Test", session1.getSessionId());
        assertTrue("Incorrect sessionId", !session1.getSessionId().equalsIgnoreCase("-1"));
        assertTrue("Incorrect sessionId", !session1.getSessionId().equalsIgnoreCase("0"));
        assertThat(session1.getDeviceCapabilitiesSet(), containsInAnyOrder(
                NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES.toArray()));

        NetconfDeviceInfo deviceInfo2 = new NetconfDeviceInfo(
                TEST_USERNAME, TEST_PASSWORD, Ip4Address.valueOf(TEST_HOSTNAME), portNumber);
        deviceInfo2.setConnectTimeoutSec(OptionalInt.of(11));
        deviceInfo2.setReplyTimeoutSec(OptionalInt.of(10));
        deviceInfo2.setIdleTimeoutSec(OptionalInt.of(12));
        session2 = new NetconfSessionMinaImpl(deviceInfo2, ImmutableList.of("urn:ietf:params:netconf:base:1.0"));
        log.info("Started NETCONF Session {} with test SSHD server in Unit Test", session2.getSessionId());
        assertTrue("Incorrect sessionId", !session2.getSessionId().equalsIgnoreCase("-1"));
        assertTrue("Incorrect sessionId", !session2.getSessionId().equalsIgnoreCase("0"));
        assertThat(session2.getDeviceCapabilitiesSet(), containsInAnyOrder(
                NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES.toArray()));
        session3 = new NetconfSessionImpl(deviceInfo1);
        log.info("Started NETCONF Session {} with test SSHD server in Unit Test", session3.getSessionId());
        assertTrue("Incorrect sessionId", !session3.getSessionId().equalsIgnoreCase("-1"));
        assertTrue("Incorrect sessionId", !session3.getSessionId().equalsIgnoreCase("0"));
        assertThat(session3.getDeviceCapabilitiesSet(), containsInAnyOrder(
                NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES_1_1.toArray()));
        session4 = new NetconfSessionImpl(deviceInfo1);
        log.info("Started NETCONF Session {} with test SSHD server in Unit Test", session4.getSessionId());
        assertTrue("Incorrect sessionId", !session4.getSessionId().equalsIgnoreCase("-1"));
        assertTrue("Incorrect sessionId", !session4.getSessionId().equalsIgnoreCase("0"));
        assertThat(session4.getDeviceCapabilitiesSet(), containsInAnyOrder(
                NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES_1_1.toArray()));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (session1 != null) {
            session1.close();
        }
        if (session2 != null) {
            session2.close();
        }
        if (session3 != null) {
            session3.close();
        }
        if (session4 != null) {
            session4.close();
        }

        sshServerNetconf.stop();
        NetconfControllerImpl.netconfConnectTimeout = NetconfControllerImpl.DEFAULT_CONNECT_TIMEOUT_SECONDS;
        NetconfControllerImpl.netconfIdleTimeout = NetconfControllerImpl.DEFAULT_IDLE_TIMEOUT_SECONDS;
        NetconfControllerImpl.netconfReplyTimeout = NetconfControllerImpl.DEFAULT_REPLY_TIMEOUT_SECONDS;
    }

    @Test
    public void testEditConfigRequest() {
        log.info("Starting edit-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF edit-config command failed",
                    session1.editConfig(DatastoreId.RUNNING,
                            null, SAMPLE_REQUEST));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF edit-config test failed: " + e.getMessage());
        }
        log.info("Finishing edit-config async");
    }

    @Test
    public void testEditConfigRequestWithChunkedFraming() {
        log.info("Starting edit-config async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF edit-config command failed",
                    session3.editConfig(DatastoreId.RUNNING,
                            null, SAMPLE_REQUEST));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF edit-config test failed: " + e.getMessage());
        }
        log.info("Finishing edit-config async");
    }

    @Test
    public void testEditConfigRequestWithOnlyNewConfiguration() {
        log.info("Starting edit-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF edit-config command failed",
                    session1.editConfig(NetconfSessionMinaImplTest.EDIT_CONFIG_REQUEST));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF edit-config test failed: " + e.getMessage());
        }
        log.info("Finishing edit-config async");
    }

    @Test
    public void testEditConfigRequestWithOnlyNewConfigurationWithChunkedFraming() {
        log.info("Starting edit-config async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF edit-config command failed",
                    session3.editConfig(NetconfSessionMinaImplTest.EDIT_CONFIG_REQUEST));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF edit-config test failed: " + e.getMessage());
        }
        log.info("Finishing edit-config async");
    }

    @Test
    public void testDeleteConfigRequestWithRunningTargetConfiguration() {
        log.info("Starting delete-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertFalse("NETCONF delete-config command failed",
                    session1.deleteConfig(DatastoreId.RUNNING));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF delete-config test failed: " + e.getMessage());
        }
        log.info("Finishing delete-config async");
    }

    @Test
    public void testDeleteConfigRequestWithRunningTargetConfigurationWithChunkedFraming() {
        log.info("Starting delete-config async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertFalse("NETCONF delete-config command failed",
                    session3.deleteConfig(DatastoreId.RUNNING));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF delete-config test failed: " + e.getMessage());
        }
        log.info("Finishing delete-config async");
    }

    @Test
    public void testCopyConfigRequest() {
        log.info("Starting copy-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF copy-config command failed",
                    session1.copyConfig(DatastoreId.RUNNING,
                            DatastoreId.CANDIDATE));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF copy-config test failed: " + e.getMessage());
        }
        log.info("Finishing copy-config async");
    }

    @Test
    public void testCopyConfigRequestWithChunkedFraming() {
        log.info("Starting copy-config async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF copy-config command failed",
                    session3.copyConfig(DatastoreId.RUNNING,
                            DatastoreId.CANDIDATE));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF copy-config test failed: " + e.getMessage());
        }
        log.info("Finishing copy-config async");
    }

    @Test
    public void testCopyConfigXml() {
        log.info("Starting copy-config XML async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF copy-config command failed",
                    session1.copyConfig(DatastoreId.RUNNING,
                            "<configuration><device-specific/></configuration>"));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF copy-config test failed: " + e.getMessage());
        }
        log.info("Finishing copy-config XML async");
    }

    @Test
    public void testCopyConfigXmlWithChunkedFraming() {
        log.info("Starting copy-config XML async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF copy-config command failed",
                    session3.copyConfig(DatastoreId.RUNNING,
                            "<configuration><device-specific/></configuration>"));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF copy-config test failed: " + e.getMessage());
        }
        log.info("Finishing copy-config XML async");
    }

    // remove test when ready to dump bare XML String API.
    @Test
    public void testCopyConfigBareXml() {
        log.info("Starting copy-config bare XML async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF copy-config command failed",
                    session1.copyConfig(DatastoreId.RUNNING,
                            "<config>"
                                    + "<configuration><device-specific/></configuration>"
                                    + "</config>"));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF copy-config test failed: " + e.getMessage());
        }
        log.info("Finishing copy-config bare XML async");
    }

    @Test
    public void testCopyConfigBareXmlWithChunkedFraming() {
        log.info("Starting copy-config bare XML async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF copy-config command failed",
                    session3.copyConfig(DatastoreId.RUNNING,
                            "<config>"
                                    + "<configuration><device-specific/></configuration>"
                                    + "</config>"));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF copy-config test failed: " + e.getMessage());
        }
        log.info("Finishing copy-config bare XML async");
    }

    @Test
    public void testGetConfigRequest() {
        log.info("Starting get-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF get-config running command failed. ",
                    NetconfSessionMinaImplTest.GET_REPLY_PATTERN.matcher(session1.getConfig(RUNNING,
                            SAMPLE_REQUEST)).matches());

            assertTrue("NETCONF get-config candidate command failed. ",
                    NetconfSessionMinaImplTest.GET_REPLY_PATTERN.matcher(session1.getConfig(CANDIDATE,
                            SAMPLE_REQUEST)).matches());

        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF get-config test failed: " + e.getMessage());
        }
        log.info("Finishing get-config async");
    }

    @Test
    public void testGetConfigRequestWithChunkedFraming() {
        log.info("Starting get-config async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF get-config running command failed. ",
                    NetconfSessionMinaImplTest.GET_REPLY_PATTERN.matcher(session3.getConfig(RUNNING,
                            SAMPLE_REQUEST)).matches());

            assertTrue("NETCONF get-config candidate command failed. ",
                    NetconfSessionMinaImplTest.GET_REPLY_PATTERN.matcher(session3.getConfig(CANDIDATE,
                            SAMPLE_REQUEST)).matches());

        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF get-config test failed: " + e.getMessage());
        }
        log.info("Finishing get-config async");
    }

    @Test
    public void testGetRequest() {
        log.info("Starting get async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF get running command failed. ",
                    NetconfSessionMinaImplTest.GET_REPLY_PATTERN.matcher(session1.get(SAMPLE_REQUEST,
                            null)).matches());

        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF get test failed: " + e.getMessage());
        }
        log.info("Finishing get async");
    }

    @Test
    public void testGetRequestWithChunkedFraming() {
        log.info("Starting get async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF get running command failed. ",
                    NetconfSessionMinaImplTest.GET_REPLY_PATTERN.matcher(session3.get(SAMPLE_REQUEST, null)).matches());

        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF get test failed: " + e.getMessage());
        }
        log.info("Finishing get async");
    }

    @Test
    public void testLockRequest() {
        log.info("Starting lock async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF lock request failed", session1.lock());
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF lock test failed: " + e.getMessage());
        }
        log.info("Finishing lock async");
    }

    @Test
    public void testLockRequestWithChunkedFraming() {
        log.info("Starting lock async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF lock request failed", session3.lock());
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF lock test failed: " + e.getMessage());
        }
        log.info("Finishing lock async");
    }

    @Test
    public void testUnLockRequest() {
        log.info("Starting unlock async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF unlock request failed", session1.unlock());
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF unlock test failed: " + e.getMessage());
        }
        log.info("Finishing unlock async");
    }

    @Test
    public void testUnLockRequestWithChunkedFraming() {
        log.info("Starting unlock async");
        assertNotNull("Incorrect sessionId", session3.getSessionId());
        try {
            assertTrue("NETCONF unlock request failed", session3.unlock());
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF unlock test failed: " + e.getMessage());
        }
        log.info("Finishing unlock async");
    }


    @Test
    public void testConcurrentSameSessionAccess() throws InterruptedException {
        NCCopyConfigCallable testCopyConfig1 = new NCCopyConfigCallable(session1, RUNNING, "candidate");
        NCCopyConfigCallable testCopyConfig2 = new NCCopyConfigCallable(session1, RUNNING, "startup");

        FutureTask<Boolean> futureCopyConfig1 = new FutureTask<>(testCopyConfig1);
        FutureTask<Boolean> futureCopyConfig2 = new FutureTask<>(testCopyConfig2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        log.info("Starting concurrent execution of copy-config through same session");
        executor.execute(futureCopyConfig1);
        executor.execute(futureCopyConfig2);

        int count = 0;
        while (count < 10) {
            if (futureCopyConfig1.isDone() && futureCopyConfig2.isDone()) {
                executor.shutdown();
                log.info("Finished concurrent same session execution");
                return;
            }
            Thread.sleep(100L);
            count++;
        }
        fail("NETCONF test failed to complete.");
    }

    @Test
    public void testConcurrentSameSessionAccessWithChunkedFraming() throws InterruptedException {
        NCCopyConfigCallable testCopyConfig1 = new NCCopyConfigCallable(session3, RUNNING, "candidate");
        NCCopyConfigCallable testCopyConfig2 = new NCCopyConfigCallable(session3, RUNNING, "startup");

        FutureTask<Boolean> futureCopyConfig1 = new FutureTask<>(testCopyConfig1);
        FutureTask<Boolean> futureCopyConfig2 = new FutureTask<>(testCopyConfig2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        log.info("Starting concurrent execution of copy-config through same session");
        executor.execute(futureCopyConfig1);
        executor.execute(futureCopyConfig2);

        int count = 0;
        while (count < 10) {
            if (futureCopyConfig1.isDone() && futureCopyConfig2.isDone()) {
                executor.shutdown();
                log.info("Finished concurrent same session execution");
                return;
            }
            Thread.sleep(100L);
            count++;
        }
        fail("NETCONF test failed to complete.");
    }

    @Test
    public void test2SessionAccess() throws InterruptedException {
        NCCopyConfigCallable testCopySession1 = new NCCopyConfigCallable(session1, RUNNING, "candidate");
        NCCopyConfigCallable testCopySession2 = new NCCopyConfigCallable(session2, RUNNING, "candidate");

        FutureTask<Boolean> futureCopySession1 = new FutureTask<>(testCopySession1);
        FutureTask<Boolean> futureCopySession2 = new FutureTask<>(testCopySession2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        log.info("Starting concurrent execution of copy-config through 2 different sessions");
        executor.execute(futureCopySession1);
        executor.execute(futureCopySession2);

        int count = 0;
        while (count < 10) {
            if (futureCopySession1.isDone() && futureCopySession2.isDone()) {
                executor.shutdown();
                log.info("Finished concurrent 2 session execution");
                return;
            }
            Thread.sleep(100L);
            count++;
        }
        fail("NETCONF test failed to complete.");
    }

    @Test
    public void test2SessionAccessWithChunkedFraming() throws InterruptedException {
        NCCopyConfigCallable testCopySession1 = new NCCopyConfigCallable(session3, RUNNING, "candidate");
        NCCopyConfigCallable testCopySession2 = new NCCopyConfigCallable(session4, RUNNING, "candidate");

        FutureTask<Boolean> futureCopySession1 = new FutureTask<>(testCopySession1);
        FutureTask<Boolean> futureCopySession2 = new FutureTask<>(testCopySession2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        log.info("Starting concurrent execution of copy-config through 2 different sessions");
        executor.execute(futureCopySession1);
        executor.execute(futureCopySession2);

        int count = 0;
        while (count < 10) {
            if (futureCopySession1.isDone() && futureCopySession2.isDone()) {
                executor.shutdown();
                log.info("Finished concurrent 2 session execution");
                return;
            }
            Thread.sleep(100L);
            count++;
        }
        fail("NETCONF test failed to complete.");
    }

    @Test
    public void testSessionTimeouts() {
        assertTrue("SSH Client wrong", session1 instanceof NetconfSessionImpl);
        assertEquals("Timeout wrong", 5, session1.timeoutConnectSec());
        assertEquals("Timeout wrong", 5, session1.timeoutReplySec());
        assertEquals("Timeout wrong", 5, session1.timeoutIdleSec());

        assertTrue("SSH Client wrong", session2 instanceof NetconfSessionMinaImpl);
        assertEquals("Timeout wrong", 11, session2.timeoutConnectSec());
        assertEquals("Timeout wrong", 10, session2.timeoutReplySec());
        assertEquals("Timeout wrong", 12, session2.timeoutIdleSec());
    }

    public static String getTestHelloReply(Optional<Long> sessionId, boolean useChunkedFraming) {
        if (useChunkedFraming) {
            return getTestHelloReply(NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES_1_1, sessionId);
        } else {
            return getTestHelloReply(NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES, sessionId);
        }
    }

    public static String getTestHelloReply(Collection<String> capabilities, Optional<Long> sessionId) {
        StringBuffer sb = new StringBuffer();

        sb.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        sb.append("<capabilities>");
        capabilities.forEach(capability -> {
            sb.append("<capability>").append(capability).append("</capability>");
        });
        sb.append("</capabilities>");
        if (sessionId.isPresent()) {
            sb.append("<session-id>");
            sb.append(sessionId.get().toString());
            sb.append("</session-id>");
        }
        sb.append("</hello>");

        return sb.toString();
    }

    public static String getOkReply(Optional<Integer> messageId) {
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ");
        if (messageId.isPresent()) {
            sb.append("message-id=\"");
            sb.append(String.valueOf(messageId.get()));
            sb.append("\">");
        }
        sb.append("<ok/>");
        sb.append("</rpc-reply>");
        return sb.toString();
    }

    public static String getGetReply(Optional<Integer> messageId) {
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ");
        if (messageId.isPresent()) {
            sb.append("message-id=\"");
            sb.append(String.valueOf(messageId.get()));
            sb.append("\">");
        }
        sb.append("<data>\n");
        sb.append(SAMPLE_REQUEST);
        sb.append("</data>\n");
        sb.append("</rpc-reply>");
        return sb.toString();
    }

    public static final Pattern HELLO_REQ_PATTERN =
            Pattern.compile("(<\\?xml).*"
                            + "(<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                            + "( *)(<capabilities>)\\R?"
                            + "( *)(<capability>urn:ietf:params:netconf:base:1.0</capability>)\\R?"
                            + "( *)(</capabilities>)\\R?"
                            + "(</hello>)\\R? *",
                    Pattern.DOTALL);

    public static final Pattern HELLO_REQ_PATTERN_1_1 =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                            + "(<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                            + "( *)(<capabilities>)\\R?"
                            + "( *)(<capability>urn:ietf:params:netconf:base:1.0</capability>)\\R?"
                            + "( *)(<capability>urn:ietf:params:netconf:base:1.1</capability>)\\R?"
                            + "( *)(</capabilities>)\\R?"
                            + "(</hello>)\\R? *",
                    Pattern.DOTALL);

    public static final Pattern EDIT_CONFIG_REQ_PATTERN =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<rpc message-id=\")[0-9]*(\") *(xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<edit-config>)\\R?"
                    + "(<target>\\R?((<" + DatastoreId.CANDIDATE.toString() + "/>)|"
                    + "(<" + DatastoreId.RUNNING.toString() + "/>)|"
                    + "(<" + DatastoreId.STARTUP.toString() + "/>))\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + ".*"
                    + "(</config>)\\R?(</edit-config>)\\R?(</rpc>)\\R?", Pattern.DOTALL);


    public static final Pattern LOCK_REQ_PATTERN =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" "
                    + "message-id=\")[0-9]*(\">)\\R?"
                    + "(<lock>)\\R?"
                    + "(<target>\\R?((<" + DatastoreId.CANDIDATE.toString() + "/>)|"
                    + "(<" + DatastoreId.RUNNING.toString() + "/>)|"
                    + "(<" + DatastoreId.STARTUP.toString() + "/>))\\R?</target>)\\R?"
                    + "(</lock>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public static final Pattern UNLOCK_REQ_PATTERN =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" "
                    + "message-id=\")[0-9]*(\">)\\R?"
                    + "(<unlock>)\\R?"
                    + "(<target>\\R?((<" + DatastoreId.CANDIDATE.toString() + "/>)|"
                    + "(<" + DatastoreId.RUNNING.toString() + "/>)|"
                    + "(<" + DatastoreId.STARTUP.toString() + "/>))\\R?</target>)\\R?"
                    + "(</unlock>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public static final Pattern COPY_CONFIG_REQ_PATTERN =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\")[0-9]*(\">)\\R?"
                    + "(<copy-config>)\\R?"
                    + "(<target>\\R?"
                    + "("
                    + "(<" + DatastoreId.CANDIDATE.toString() + "/>)|"
                    + "(<" + DatastoreId.RUNNING.toString() + "/>)|"
                    + "(<" + DatastoreId.STARTUP.toString() + "/>)"
                    + ")\\R?"
                    + "</target>)\\R?"
                    + "(<source>)\\R?"
                    + "("
                    + "(<config>)(.*)(</config>)|"
                    + "(<" + DatastoreId.CANDIDATE.toString() + "/>)|"
                    + "(<" + DatastoreId.RUNNING.toString() + "/>)|"
                    + "(<" + DatastoreId.STARTUP.toString() + "/>)"
                    + ")\\R?"
                    + "(</source>)\\R?"
                    + "(</copy-config>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public static final Pattern GET_CONFIG_REQ_PATTERN =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<rpc message-id=\")[0-9]*(\"  xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<get-config>)\\R?" + "(<source>)\\R?((<"
                    + DatastoreId.CANDIDATE.toString()
                    + "/>)|(<" + DatastoreId.RUNNING.toString()
                    + "/>)|(<" + DatastoreId.STARTUP.toString()
                    + "/>))\\R?(</source>)\\R?"
                    + "(<filter type=\"subtree\">).*(</filter>)\\R?"
                    + "(</get-config>)\\R?(</rpc>)\\R?", Pattern.DOTALL);



    public static final Pattern GET_REQ_PATTERN =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<rpc message-id=\")[0-9]*(\"  xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<get>)\\R?"
                    + "(<filter type=\"subtree\">).*(</filter>)\\R?"
                    + "(</get>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public class NCCopyConfigCallable implements Callable<Boolean> {
        private NetconfSession session;
        private DatastoreId target;
        private String source;

        public NCCopyConfigCallable(NetconfSession session, DatastoreId target, String source) {
            this.session = session;
            this.target = target;
            this.source = source;
        }

        @Override
        public Boolean call() throws Exception {
            return session.copyConfig(target, source);
        }
    }
}
