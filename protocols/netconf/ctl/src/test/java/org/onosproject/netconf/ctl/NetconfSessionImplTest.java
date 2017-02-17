/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.netconf.ctl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.packet.Ip4Address;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for NetconfSession.
 *
 * Sets up an SSH Server with Apache SSHD and connects to it using 2 clients
 * Truly verifies that the NETCONF flows are compliant with a NETCONF server.
 */
public class NetconfSessionImplTest {
    private static final Logger log = LoggerFactory
            .getLogger(NetconfStreamThread.class);

    private static final int PORT_NUMBER = TestTools.findAvailablePort(50830);
    private static final String TEST_USERNAME = "netconf";
    private static final String TEST_PASSWORD = "netconf123";
    private static final String TEST_HOSTNAME = "127.0.0.1";
    private static final String TEST_SERFILE =
            System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "testkey.ser";

    private static final String SAMPLE_REQUEST =
            "<some-yang-element xmlns=\"some-namespace\">"
            + "<some-child-element/>"
            + "</some-yang-element>";

    private static NetconfSession session1;
    private static NetconfSession session2;
    private static SshServer sshServerNetconf;

    @BeforeClass
    public static void setUp() throws Exception {
        sshServerNetconf = SshServer.setUpDefaultServer();
        List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
        userAuthFactories.add(new UserAuthPassword.Factory());
        sshServerNetconf.setUserAuthFactories(userAuthFactories);
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
        sshServerNetconf.setPort(PORT_NUMBER);
        sshServerNetconf.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(TEST_SERFILE));
        sshServerNetconf.setSubsystemFactories(
                Arrays.<NamedFactory<Command>>asList(new NetconfSshdTestSubsystem.Factory()));
        sshServerNetconf.open();
        log.info("SSH Server opened on port {}", PORT_NUMBER);

        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(
                TEST_USERNAME, TEST_PASSWORD, Ip4Address.valueOf(TEST_HOSTNAME), PORT_NUMBER);

        session1 = new NetconfSessionImpl(deviceInfo);
        log.info("Started NETCONF Session {} with test SSHD server in Unit Test", session1.getSessionId());
        assertTrue("Incorrect sessionId", !session1.getSessionId().equalsIgnoreCase("-1"));
        assertTrue("Incorrect sessionId", !session1.getSessionId().equalsIgnoreCase("0"));

        session2 = new NetconfSessionImpl(deviceInfo);
        log.info("Started NETCONF Session {} with test SSHD server in Unit Test", session2.getSessionId());
        assertTrue("Incorrect sessionId", !session2.getSessionId().equalsIgnoreCase("-1"));
        assertTrue("Incorrect sessionId", !session2.getSessionId().equalsIgnoreCase("0"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (session1 != null) {
            session1.close();
        }
        if (session2 != null) {
            session2.close();
        }

        sshServerNetconf.stop();
    }

    @Test
    public void testEditConfigRequest() {
        log.info("Starting edit-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF edit-config command failed", session1.editConfig("running", null, SAMPLE_REQUEST));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF edit-config test failed: " + e.getMessage());
        }
        log.info("Finishing edit-config async");
    }

    @Test
    public void testCopyConfigRequest() {
        log.info("Starting copy-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF edit-config command failed", session1.copyConfig("running", "candidate"));
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF edit-config test failed: " + e.getMessage());
        }
        log.info("Finishing copy-config async");
    }

    @Test
    public void testGetConfigRequest() {
        log.info("Starting get-config async");
        assertNotNull("Incorrect sessionId", session1.getSessionId());
        try {
            assertTrue("NETCONF get-config running command failed. ",
                    GET_REPLY_PATTERN.matcher(session1.getConfig("running", SAMPLE_REQUEST)).matches());

            assertTrue("NETCONF get-config candidate command failed. ",
                    GET_REPLY_PATTERN.matcher(session1.getConfig("candidate", SAMPLE_REQUEST)).matches());

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
                    GET_REPLY_PATTERN.matcher(session1.get(SAMPLE_REQUEST, null)).matches());

        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF get test failed: " + e.getMessage());
        }
        log.info("Finishing get async");
    }

    @Test
    public void testConcurrentSameSessionAccess() throws InterruptedException {
        NCCopyConfigCallable testCopyConfig1 = new NCCopyConfigCallable(session1, "running", "candidate");
        NCCopyConfigCallable testCopyConfig2 = new NCCopyConfigCallable(session1, "candidate", "startup");

        FutureTask<Boolean> futureCopyConfig1 = new FutureTask<Boolean>(testCopyConfig1);
        FutureTask<Boolean> futureCopyConfig2 = new FutureTask<Boolean>(testCopyConfig2);

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
        NCCopyConfigCallable testCopySession1 = new NCCopyConfigCallable(session1, "running", "candidate");
        NCCopyConfigCallable testCopySession2 = new NCCopyConfigCallable(session2, "running", "candidate");

        FutureTask<Boolean> futureCopySession1 = new FutureTask<Boolean>(testCopySession1);
        FutureTask<Boolean> futureCopySession2 = new FutureTask<Boolean>(testCopySession2);

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


    public static String getTestHelloReply(Optional<Long> sessionId) {
        StringBuffer sb = new StringBuffer();

        sb.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        sb.append("<capabilities>");
        sb.append("<capability>urn:ietf:params:netconf:base:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:base:1.1</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:candidate:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:startup:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:interleave:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:notification:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:validate:1.0</capability>");
        sb.append("<capability>urn:ietf:params:netconf:capability:validate:1.1</capability>");
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

    public static final Pattern EDIT_CONFIG_REQ_PATTERN =
            Pattern.compile("(<\\?xml).*"
                    + "(<rpc message-id=\")[0-9]*(\") *(xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<edit-config>)\\R?"
                    + "(<target>\\R?((<candidate/>)|(<running/>)|(<startup/>))\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + ".*"
                    + "(</config>)\\R?(</edit-config>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public static final Pattern COPY_CONFIG_REQ_PATTERN =
            Pattern.compile("(<\\?xml).*"
                    + "(<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\")[0-9]*(\">)\\R?"
                    + "(<copy-config>)\\R?"
                    + "(<target>\\R?((<candidate/>)|(<running/>)|(<startup/>))\\R?</target>)\\R?"
                    + "(<source>)\\R?(<config>)((candidate)|(running)|(startup))(</config>)\\R?(</source>)\\R?"
                    + "(</copy-config>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public static final Pattern GET_CONFIG_REQ_PATTERN =
            Pattern.compile("(<\\?xml).*"
                    + "(<rpc message-id=\")[0-9]*(\"  xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<get-config>)\\R?"
                    + "(<source>)\\R?((<candidate/>)|(<running/>)|(<startup/>))\\R?(</source>)\\R?"
                    + "(<filter type=\"subtree\">).*(</filter>)\\R?"
                    + "(</get-config>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public static final Pattern GET_REPLY_PATTERN =
            Pattern.compile("(<\\?xml).*"
                    + "(<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\")[0-9]*(\">)\\R?"
                    + "(<data>).*(</data>)\\R?"
                    + "(</rpc-reply>)\\R?", Pattern.DOTALL);

    public static final Pattern GET_REQ_PATTERN =
            Pattern.compile("(<\\?xml).*"
                    + "(<rpc message-id=\")[0-9]*(\"  xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<get>)\\R?"
                    + "(<filter type=\"subtree\">).*(</filter>)\\R?"
                    + "(</get>)\\R?(</rpc>)\\R?", Pattern.DOTALL);

    public class NCCopyConfigCallable implements Callable<Boolean> {
        private NetconfSession session;
        private String target;
        private String source;

        public NCCopyConfigCallable(NetconfSession session, String target, String source) {
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
