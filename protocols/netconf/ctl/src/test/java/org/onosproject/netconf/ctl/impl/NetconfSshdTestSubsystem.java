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


import org.apache.commons.lang3.tuple.Pair;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.threads.ThreadUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.ctl.impl.NetconfStreamThread.NetconfMessageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * Mocks a NETCONF Device to test the NETCONF Southbound Interface etc.
 *
 * Implements the 'netconf' subsystem on Apache SSH (Mina).
 * See SftpSubsystem for an example of another subsystem
 */
public class NetconfSshdTestSubsystem extends Thread implements Command, Runnable, SessionAware {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static class Factory implements NamedFactory<Command> {

        public static final String NAME = "netconf";

        private final ExecutorService   executors;
        private final boolean shutdownExecutor;

        public Factory() {
            this(null);
        }

        /**
         * @param executorService The {@link ExecutorService} to be used by
         *                        the {@link SftpSubsystem} command when starting execution. If
         *                        {@code null} then a single-threaded ad-hoc service is used.
         *                        <B>Note:</B> the service will <U>not</U> be shutdown when the
         *                        subsystem is closed - unless it is the ad-hoc service, which will be
         *                        shutdown regardless
         * @see #Factory(ExecutorService, boolean)
         */
        public Factory(ExecutorService executorService) {
            this(executorService, false);
        }

        /**
         * @param executorService The {@link ExecutorService} to be used by
         *                        the {@link SftpSubsystem} command when starting execution. If
         *                        {@code null} then a single-threaded ad-hoc service is used.
         * @param shutdownOnExit  If {@code true} the {@link ExecutorService#shutdownNow()}
         *                        will be called when subsystem terminates - unless it is the ad-hoc
         *                        service, which will be shutdown regardless
         */
        public Factory(ExecutorService executorService, boolean shutdownOnExit) {
            executors = executorService;
            shutdownExecutor = shutdownOnExit;
        }

        public ExecutorService getExecutorService() {
            return executors;
        }

        public boolean isShutdownOnExit() {
            return shutdownExecutor;
        }

        @Override
        public Command create() {
            return new NetconfSshdTestSubsystem(getExecutorService(), isShutdownOnExit());
        }

        @Override
        public String getName() {
            return NAME;
        }
    }

    /**
     * Properties key for the maximum of available open handles per session.
     */
    private static final String CLOSE_SESSION = "<close-session";
    private static final String END_PATTERN = "]]>]]>";
    private static final String HASH = "#";
    private static final String LF = "\n";
    private static final String MSGLEN_REGEX_PATTERN = "\n#\\d+\n";
    private static final String MSGLEN_PART_REGEX_PATTERN = "\\d+\n";
    private static final String CHUNKED_END_REGEX_PATTERN = "\n##\n";

    private ExecutorService executors;
    private boolean shutdownExecutor;
    private ExitCallback callback;
    private ServerSession session;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private Environment env;
    private Future<?> pendingFuture;
    private boolean closed = false;
    private NetconfMessageState state;
    private PrintWriter outputStream;

    private static final String SAMPLE_REQUEST =
    "<some-yang-element xmlns=\"some-namespace\">"
            + "<some-child-element/>"
            + "</some-yang-element>";
    public static final Pattern GET_REQ_PATTERN =
    Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
            + "(<rpc message-id=\")[0-9]*(\"  xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
            + "(<get>)\\R?"
            + "(<filter type=\"subtree\">).*(</filter>)\\R?"
            + "(</get>)\\R?(</rpc>)\\R?", Pattern.DOTALL);
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
    public static final Pattern UNLOCK_REQ_PATTERN =
    Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
            + "(<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" "
            + "message-id=\")[0-9]*(\">)\\R?"
            + "(<unlock>)\\R?"
            + "(<target>\\R?((<" + DatastoreId.CANDIDATE.toString() + "/>)|"
            + "(<" + DatastoreId.RUNNING.toString() + "/>)|"
            + "(<" + DatastoreId.STARTUP.toString() + "/>))\\R?</target>)\\R?"
            + "(</unlock>)\\R?(</rpc>)\\R?", Pattern.DOTALL);
    public static final Pattern LOCK_REQ_PATTERN =
    Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
            + "(<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" "
            + "message-id=\")[0-9]*(\">)\\R?"
            + "(<lock>)\\R?"
            + "(<target>\\R?((<" + DatastoreId.CANDIDATE.toString() + "/>)|"
            + "(<" + DatastoreId.RUNNING.toString() + "/>)|"
            + "(<" + DatastoreId.STARTUP.toString() + "/>))\\R?</target>)\\R?"
            + "(</lock>)\\R?(</rpc>)\\R?", Pattern.DOTALL);
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
    public static final Pattern HELLO_REQ_PATTERN_1_1 =
    Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "( *)(<capabilities>)\\R?"
                    + "( *)(<capability>urn:ietf:params:netconf:base:1.0</capability>)\\R?"
                    + "( *)(<capability>urn:ietf:params:netconf:base:1.1</capability>)\\R?"
                    + "( *)(</capabilities>)\\R?"
                    + "(</hello>)\\R? *",
            Pattern.DOTALL);
    public static final Pattern HELLO_REQ_PATTERN =
    Pattern.compile("(<\\?xml).*"
                    + "(<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "( *)(<capabilities>)\\R?"
                    + "( *)(<capability>urn:ietf:params:netconf:base:1.0</capability>)\\R?"
                    + "( *)(</capabilities>)\\R?"
                    + "(</hello>)\\R? *",
            Pattern.DOTALL);

    public NetconfSshdTestSubsystem() {
        this(null);
    }

    /**
     * @param executorService The {@link ExecutorService} to be used by
     *                        the {@link SftpSubsystem} command when starting execution. If
     *                        {@code null} then a single-threaded ad-hoc service is used.
     *                        <b>Note:</b> the service will <U>not</U> be shutdown when the
     *                        subsystem is closed - unless it is the ad-hoc service
     * @see #SftpSubsystem(ExecutorService, boolean)
     */
    public NetconfSshdTestSubsystem(ExecutorService executorService) {
        this(executorService, false);
    }

    /**
     * @param executorService The {@link ExecutorService} to be used by
     *                        the {@link SftpSubsystem} command when starting execution. If
     *                        {@code null} then a single-threaded ad-hoc service is used.
     * @param shutdownOnExit  If {@code true} the {@link ExecutorService#shutdownNow()}
     *                        will be called when subsystem terminates - unless it is the ad-hoc
     *                        service, which will be shutdown regardless
     * @see ThreadUtils#newSingleThreadExecutor(String)
     */
    public NetconfSshdTestSubsystem(ExecutorService executorService, boolean shutdownOnExit) {
        executors = executorService;
        if (executorService == null) {
            executors = ThreadUtils.newSingleThreadExecutor(getClass().getSimpleName());
            shutdownExecutor = true;    // we always close the ad-hoc executor service
        } else {
            shutdownExecutor = shutdownOnExit;
        }
    }

    @Override
    public void setSession(ServerSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        BufferedReader bufferReader = new BufferedReader(new InputStreamReader(in));
        boolean socketClosed = false;
        try {
            StringBuilder deviceRequestBuilder = new StringBuilder();
            while (!socketClosed) {
                int cInt = bufferReader.read();
                if (cInt == -1) {
                    log.info("Netconf client sent error");
                    socketClosed = true;
                }
                char c = (char) cInt;
                state = state.evaluateChar(c);
                deviceRequestBuilder.append(c);
                if (state == NetconfMessageState.END_PATTERN) {
                    String deviceRequest = deviceRequestBuilder.toString();
                    if (deviceRequest.equals(END_PATTERN)) {
                        socketClosed = true;
                        this.interrupt();
                    } else {
                        deviceRequest = deviceRequest.replace(END_PATTERN, "");
                        Optional<Integer> messageId = NetconfStreamThread.getMsgId(deviceRequest);
                        log.info("Client Request on session {}. MsgId {}: {}",
                                session.getSessionId(), messageId, deviceRequest);
                        synchronized (outputStream) {

                            if (HELLO_REQ_PATTERN.matcher(deviceRequest).matches()) {

                                String helloReply =
                                        getTestHelloReply(Optional.of(ByteBuffer.wrap(
                                                session.getSessionId()).asLongBuffer().get()), false);
                                outputStream.write(helloReply + END_PATTERN);
                                outputStream.flush();
                            } else if (HELLO_REQ_PATTERN_1_1.matcher(deviceRequest).matches()) {

                                String helloReply =
                                        getTestHelloReply(Optional.of(ByteBuffer.wrap(
                                                session.getSessionId()).asLongBuffer().get()), true);
                                outputStream.write(helloReply + END_PATTERN);
                                outputStream.flush();
                            } else {
                                Pair<String, Boolean> replyClosedPair = dealWithRequest(deviceRequest, messageId);
                                String reply = replyClosedPair.getLeft();
                                if (reply != null) {
                                    Boolean newSockedClosed = replyClosedPair.getRight();
                                    socketClosed = newSockedClosed.booleanValue();
                                    outputStream.write(reply + END_PATTERN);
                                    outputStream.flush();
                                }
                            }
                        }
                        deviceRequestBuilder.setLength(0);
                    }
                } else if (state == NetconfMessageState.END_CHUNKED_PATTERN) {
                    String deviceRequest = deviceRequestBuilder.toString();
                    if (!validateChunkedFraming(deviceRequest)) {
                        log.error("Netconf client send badly framed message {}",
                                deviceRequest);
                    } else {
                        deviceRequest = deviceRequest.replaceAll(MSGLEN_REGEX_PATTERN, "");
                        deviceRequest = deviceRequest.replaceAll(CHUNKED_END_REGEX_PATTERN, "");
                        Optional<Integer> messageId = NetconfStreamThread.getMsgId(deviceRequest);
                        log.info("Client Request on session {}. MsgId {}: {}",
                                session.getSessionId(), messageId, deviceRequest);

                        synchronized (outputStream) {

                            if (HELLO_REQ_PATTERN.matcher(deviceRequest).matches()) {
                                String helloReply =
                                        getTestHelloReply(Optional.of(ByteBuffer.wrap(
                                                session.getSessionId()).asLongBuffer().get()), true);
                                outputStream.write(helloReply + END_PATTERN);
                                outputStream.flush();
                            } else {
                                Pair<String, Boolean> replyClosedPair = dealWithRequest(deviceRequest, messageId);
                                String reply = replyClosedPair.getLeft();
                                if (reply != null) {
                                    Boolean newSockedClosed = replyClosedPair.getRight();
                                    socketClosed = newSockedClosed.booleanValue();
                                    outputStream.write(formatChunkedMessage(reply));
                                    outputStream.flush();
                                }
                            }
                        }

                    }
                    deviceRequestBuilder.setLength(0);
                }
            }
        } catch (Throwable t) {
            if (!socketClosed && !(t instanceof EOFException)) { // Ignore
                log.error("Exception caught in NETCONF Server subsystem", t.getMessage());
            }
        } finally {
            try {
                bufferReader.close();
            } catch (IOException ioe) {
                log.error("Could not close DataInputStream", ioe);
            }

            callback.onExit(0);
        }
    }

    private boolean validateChunkedFraming(String reply) {
        String[] strs = reply.split(LF + HASH);
        int strIndex = 0;
        while (strIndex < strs.length) {
            String str = strs[strIndex];
            if ((str.equals(HASH + LF))) {
                return true;
            }
            if (!str.equals("")) {
                try {
                    if (str.equals(LF)) {
                        return false;
                    }
                    int len = Integer.parseInt(str.split(LF)[0]);
                    if (str.split(MSGLEN_PART_REGEX_PATTERN)[1].getBytes("UTF-8").length != len) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            strIndex++;
        }
        return true;
    }

    private Pair<String, Boolean> dealWithRequest(String deviceRequest, Optional<Integer> messageId) {
        if (EDIT_CONFIG_REQ_PATTERN.matcher(deviceRequest).matches()
                || COPY_CONFIG_REQ_PATTERN.matcher(deviceRequest).matches()
                || LOCK_REQ_PATTERN.matcher(deviceRequest).matches()
                || UNLOCK_REQ_PATTERN.matcher(deviceRequest).matches()) {
            return Pair.of(getOkReply(messageId), false);

        } else if (GET_CONFIG_REQ_PATTERN.matcher(deviceRequest).matches()
                || GET_REQ_PATTERN.matcher(deviceRequest).matches()) {
            return Pair.of(getGetReply(messageId), false);
        } else if (deviceRequest.contains(CLOSE_SESSION)) {
            return Pair.of(getOkReply(messageId), true);
        } else {
            log.error("Unexpected NETCONF message structure on session {} : {}",
                    ByteBuffer.wrap(
                            session.getSessionId()).asLongBuffer().get(), deviceRequest);
            return null;
        }
    }

    private String formatChunkedMessage(String message) {
        if (message.endsWith(END_PATTERN)) {
            message = message.split(END_PATTERN)[0];
        }
        if (!message.startsWith(LF + HASH)) {
            try {
                message = LF + HASH + message.getBytes("UTF-8").length + LF + message + LF + HASH + HASH + LF;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return message;
    }


    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        this.env = env;
        state = NetconfMessageState.NO_MATCHING_PATTERN;
        outputStream = new PrintWriter(out, false);
        try {
            pendingFuture = executors.submit(this);
        } catch (RuntimeException e) {    // e.g., RejectedExecutionException
            log.error("Failed (" + e.getClass().getSimpleName() + ") to start command: " + e.getMessage(), e);
            throw new IOException(e);
        }
    }

    @Override
    public void interrupt() {
        destroy();
    }

    @Override
    public void destroy() {
        // if thread has not completed, cancel it
        if ((pendingFuture != null) && (!pendingFuture.isDone())) {
            boolean result = pendingFuture.cancel(true);
            // TODO consider waiting some reasonable (?) amount of time for cancellation
            if (log.isDebugEnabled()) {
                log.debug("destroy() - cancel pending future=" + result);
            }
        }

        pendingFuture = null;

        if ((executors != null) && shutdownExecutor) {
            Collection<Runnable> runners = executors.shutdownNow();
            if (log.isDebugEnabled()) {
                log.debug("destroy() - shutdown executor service - runners count=" +
                                  runners.size());
            }
        }

        executors = null;

        if (!closed) {
            if (log.isDebugEnabled()) {
                log.debug("destroy() - mark as closed");
            }

            closed = true;
        }
        outputStream.close();
    }

    protected void process(Buffer buffer) throws IOException {
        log.warn("Received buffer:" + buffer);
    }

    public static String getTestHelloReply(Collection<String> capabilities, Optional<Long> sessionId) {
        StringBuilder sb = new StringBuilder();

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

    public static String getTestHelloReply(Optional<Long> sessionId, boolean useChunkedFraming) {
        if (useChunkedFraming) {
            return getTestHelloReply(NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES_1_1, sessionId);
        } else {
            return getTestHelloReply(NetconfSessionMinaImplTest.DEFAULT_CAPABILITIES, sessionId);
        }
    }

    public static String getGetReply(Optional<Integer> messageId) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
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

    public static String getOkReply(Optional<Integer> messageId) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
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
}
