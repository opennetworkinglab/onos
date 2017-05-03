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
package org.onosproject.netconf.ctl.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.threads.ThreadUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.onosproject.netconf.ctl.impl.NetconfStreamThread.NetconfMessageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
         * @see Factory(ExecutorService, boolean)}
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
                            if (NetconfSessionImplTest.HELLO_REQ_PATTERN.matcher(deviceRequest).matches()) {
                                String helloReply =
                                        NetconfSessionImplTest.getTestHelloReply(Optional.of(ByteBuffer.wrap(
                                                session.getSessionId()).asLongBuffer().get()));
                                outputStream.write(helloReply + END_PATTERN);
                                outputStream.flush();
                            } else if (NetconfSessionImplTest.EDIT_CONFIG_REQ_PATTERN.matcher(deviceRequest).matches()
                                 || NetconfSessionImplTest.COPY_CONFIG_REQ_PATTERN.matcher(deviceRequest).matches()
                                    || NetconfSessionImplTest.LOCK_REQ_PATTERN.matcher(deviceRequest).matches()
                                    || NetconfSessionImplTest.UNLOCK_REQ_PATTERN.matcher(deviceRequest).matches()) {
                                outputStream.write(NetconfSessionImplTest.getOkReply(messageId) + END_PATTERN);
                                outputStream.flush();
                            } else if (NetconfSessionImplTest.GET_CONFIG_REQ_PATTERN.matcher(deviceRequest).matches()
                                    || NetconfSessionImplTest.GET_REQ_PATTERN.matcher(deviceRequest).matches()) {
                                outputStream.write(NetconfSessionImplTest.getGetReply(messageId) + END_PATTERN);
                                outputStream.flush();
                            } else if (deviceRequest.contains(CLOSE_SESSION)) {
                                socketClosed = true;
                                outputStream.write(NetconfSessionImplTest.getOkReply(messageId) + END_PATTERN);
                                outputStream.flush();
                            } else {
                                log.error("Unexpected NETCONF message structure on session {} : {}",
                                          ByteBuffer.wrap(
                                                  session.getSessionId()).asLongBuffer().get(), deviceRequest);
                            }
                        }
                        deviceRequestBuilder.setLength(0);
                    }
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
        // if thread has not completed, cancel it
        if ((pendingFuture != null) && (!pendingFuture.isDone())) {
            boolean result = pendingFuture.cancel(true);
            // TODO consider waiting some reasonable (?) amount of time for cancellation
            if (log.isDebugEnabled()) {
                log.debug("interrupt() - cancel pending future=" + result);
            }
        }

        pendingFuture = null;

        if ((executors != null) && shutdownExecutor) {
            Collection<Runnable> runners = executors.shutdownNow();
            if (log.isDebugEnabled()) {
                log.debug("interrupt() - shutdown executor service - runners count=" +
                        runners.size());
            }
        }

        executors = null;

        if (!closed) {
            if (log.isDebugEnabled()) {
                log.debug("interrupt() - mark as closed");
            }

            closed = true;
        }
        outputStream.close();
    }

    @Override
    public void destroy() {
        //Handled by interrupt
    }

    protected void process(Buffer buffer) throws IOException {
        log.warn("Receieved buffer:" + buffer);
    }
}
