/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ClosedByInterruptException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread that gets spawned each time a session is established and handles all the input
 * and output from the session's streams to and from the NETCONF device the session is
 * established with.
 */
public class NetconfStreamThread extends Thread implements NetconfStreamHandler {

    private static final Logger log = LoggerFactory
            .getLogger(NetconfStreamThread.class);
    private static final String HELLO = "<hello";
    private static final String END_PATTERN = "]]>]]>";
    private static final String RPC_REPLY = "rpc-reply";
    private static final String RPC_ERROR = "rpc-error";
    private static final String NOTIFICATION_LABEL = "<notification";
    private static final String MESSAGE_ID = "message-id=";
    private static final Pattern MSGID_PATTERN = Pattern.compile(MESSAGE_ID + "\"(\\d+)\"");
    private static final String MSGLEN_REGEX_PATTERN = "\n#\\d+\n";
    // pattern to verify whole Chunked-Message format
    private static final Pattern CHUNKED_FRAMING_PATTERN =
            Pattern.compile("(\\n#([1-9][0-9]*)\\n(.+))+\\n##\\n", Pattern.DOTALL);
    private static final String CHUNKED_END_REGEX_PATTERN = "\n##\n";
    // pattern to parse each chunk-size in ChunkedMessage chunk
    private static final Pattern CHUNKED_SIZE_PATTERN = Pattern.compile("\\n#([1-9][0-9]*)\\n");
    private static final char HASH_CHAR = '#';
    private static final char LF_CHAR = '\n';
    protected static final String ON_REQUEST = "on request";

    private OutputStreamWriter outputStream;
    private final InputStream err;
    private final InputStream in;
    private NetconfDeviceInfo netconfDeviceInfo;
    private NetconfSessionDelegate sessionDelegate;
    private NetconfMessageState state;
    private List<NetconfDeviceOutputEventListener> netconfDeviceEventListeners
            = Lists.newCopyOnWriteArrayList();
    private boolean enableNotifications = true;
    private Map<Integer, CompletableFuture<String>> replies;

    public NetconfStreamThread(final InputStream in, final OutputStream out,
                               final InputStream err, NetconfDeviceInfo deviceInfo,
                               NetconfSessionDelegate delegate,
                               Map<Integer, CompletableFuture<String>> replies) {
        this.in = in;
        this.err = err;
        outputStream = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        netconfDeviceInfo = deviceInfo;
        state = NetconfMessageState.NO_MATCHING_PATTERN;
        sessionDelegate = delegate;
        this.replies = replies;
        log.debug("Stream thread for device {} session started", deviceInfo);
        start();
    }

    @Override
    public CompletableFuture<String> sendMessage(String request) {
        Optional<Integer> messageId = getMsgId(request);
        return sendMessage(request, messageId.get());
    }

    @Override
    public CompletableFuture<String> sendMessage(String request, int messageId) {
        log.debug("Sending message {} to device {}", request, netconfDeviceInfo);
        CompletableFuture<String> cf = new CompletableFuture<>();
        replies.put(messageId, cf);

        synchronized (outputStream) {
            try {
                outputStream.write(request);
                outputStream.flush();
            } catch (IOException e) {
                log.error("Writing to {} failed", netconfDeviceInfo, e);
                cf.completeExceptionally(e);
            }
        }

        return cf;
    }

    public enum NetconfMessageState {

        NO_MATCHING_PATTERN {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == ']') {
                    return FIRST_BRACKET;
                } else if (c == '\n') {
                    return FIRST_LF;
                } else {
                    return this;
                }
            }
        },
        FIRST_BRACKET {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == ']') {
                    return SECOND_BRACKET;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        SECOND_BRACKET {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == '>') {
                    return FIRST_BIGGER;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        FIRST_BIGGER {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == ']') {
                    return THIRD_BRACKET;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        THIRD_BRACKET {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == ']') {
                    return ENDING_BIGGER;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        ENDING_BIGGER {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == '>') {
                    return END_PATTERN;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        FIRST_LF {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == '#') {
                    return FIRST_HASH;
                } else if (c == ']') {
                    return FIRST_BRACKET;
                } else if (c == '\n') {
                    return this;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        FIRST_HASH {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == '#') {
                    return SECOND_HASH;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        SECOND_HASH {
            @Override
            NetconfMessageState evaluateChar(char c) {
                if (c == '\n') {
                    return END_CHUNKED_PATTERN;
                } else {
                    return NO_MATCHING_PATTERN;
                }
            }
        },
        END_CHUNKED_PATTERN {
            @Override
            NetconfMessageState evaluateChar(char c) {
                return NO_MATCHING_PATTERN;
            }
        },
        END_PATTERN {
            @Override
            NetconfMessageState evaluateChar(char c) {
                return NO_MATCHING_PATTERN;
            }
        };

        abstract NetconfMessageState evaluateChar(char c);
    }

    @Override
    public void run() {
        BufferedReader bufferReader = null;
        while (bufferReader == null) {
            try {
                bufferReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        try {
            boolean socketClosed = false;
            StringBuilder deviceReplyBuilder = new StringBuilder();
            while (!socketClosed && !this.isInterrupted()) {
                int cInt = bufferReader.read();
                if (cInt == -1) {
                    log.debug("Netconf device {}  sent error char in session," +
                            " will need to be reopened", netconfDeviceInfo);
                    NetconfDeviceOutputEvent event = new NetconfDeviceOutputEvent(
                            NetconfDeviceOutputEvent.Type.SESSION_CLOSED,
                            null, null, Optional.of(-1), netconfDeviceInfo);
                    netconfDeviceEventListeners.forEach(
                            listener -> listener.event(event));
                    socketClosed = true;
                    log.debug("Netconf device {} ERROR cInt == -1 socketClosed = true", netconfDeviceInfo);
                }
                char c = (char) cInt;
                state = state.evaluateChar(c);
                deviceReplyBuilder.append(c);
                if (state == NetconfMessageState.END_PATTERN) {
                    String deviceReply = deviceReplyBuilder.toString();
                    if (deviceReply.equals(END_PATTERN)) {
                        socketClosed = true;
                        close(deviceReply);
                    } else {
                        deviceReply = deviceReply.replace(END_PATTERN, "");
                        dealWithReply(deviceReply);
                        deviceReplyBuilder.setLength(0);
                    }
                } else if (state == NetconfMessageState.END_CHUNKED_PATTERN) {
                    String deviceReply = deviceReplyBuilder.toString();
                    if (!validateChunkedFraming(deviceReply)) {
                        log.debug("Netconf device {} send badly framed message {}",
                                netconfDeviceInfo, deviceReply);
                        socketClosed = true;
                        close(deviceReply);
                    } else {
                        deviceReply = deviceReply.replaceAll(MSGLEN_REGEX_PATTERN, "");
                        deviceReply = deviceReply.replaceAll(CHUNKED_END_REGEX_PATTERN, "");
                        dealWithReply(deviceReply);
                        deviceReplyBuilder.setLength(0);
                    }
                }
            }
        } catch (ClosedByInterruptException i) {
            log.debug("Connection to device {} was terminated on request", netconfDeviceInfo.toString());
        } catch (IOException e) {
            log.warn("Error in reading from the session for device {} ", netconfDeviceInfo, e);
            throw new IllegalStateException(new NetconfException("Error in reading from the session for device {}" +
                    netconfDeviceInfo, e));
            //TODO should we send a socket closed message to listeners ?
        }
    }

    public void close() {
        close(ON_REQUEST);
    }

    private void close(String deviceReply) {
        log.debug("Netconf device {} socketClosed = true DEVICE_UNREGISTERED {}",
                netconfDeviceInfo, deviceReply);
        if (!deviceReply.equals(ON_REQUEST)) {
            NetconfDeviceOutputEvent event = new NetconfDeviceOutputEvent(
                    NetconfDeviceOutputEvent.Type.DEVICE_UNREGISTERED,
                    null, null, Optional.of(-1), netconfDeviceInfo);
            netconfDeviceEventListeners.forEach(
                    listener -> listener.event(event));
        }
        this.interrupt();
    }

    private void dealWithReply(String deviceReply) {
        if (deviceReply.contains(RPC_REPLY) ||
                deviceReply.contains(RPC_ERROR) ||
                deviceReply.contains(HELLO)) {
            log.debug("Netconf device {} sessionDelegate.notify() DEVICE_REPLY {} {}",
                    netconfDeviceInfo, getMsgId(deviceReply), deviceReply);
            NetconfDeviceOutputEvent event = new NetconfDeviceOutputEvent(
                    NetconfDeviceOutputEvent.Type.DEVICE_REPLY,
                    null, deviceReply, getMsgId(deviceReply), netconfDeviceInfo);
            sessionDelegate.notify(event);
            netconfDeviceEventListeners.forEach(
                    listener -> listener.event(event));
        } else if (deviceReply.contains(NOTIFICATION_LABEL)) {
            log.debug("Netconf device {} DEVICE_NOTIFICATION {} {} {}",
                    netconfDeviceInfo, enableNotifications,
                    getMsgId(deviceReply), deviceReply);
            if (enableNotifications) {
                log.debug("dispatching to {} listeners", netconfDeviceEventListeners.size());
                final String finalDeviceReply = deviceReply;
                netconfDeviceEventListeners.forEach(
                        listener -> listener.event(new NetconfDeviceOutputEvent(
                                NetconfDeviceOutputEvent.Type.DEVICE_NOTIFICATION,
                                null, finalDeviceReply, getMsgId(finalDeviceReply),
                                netconfDeviceInfo)));
            }
        } else {
            log.debug("Error on reply from device {} {}", netconfDeviceInfo, deviceReply);
        }
    }

    static boolean validateChunkedFraming(String reply) {
        Matcher matcher = CHUNKED_FRAMING_PATTERN.matcher(reply);
        if (!matcher.matches()) {
            log.debug("Error on reply {}", reply);
            return false;
        }
        Matcher chunkM = CHUNKED_SIZE_PATTERN.matcher(reply);
        List<MatchResult> chunks = new ArrayList<>();
        String chunkdataStr = "";
        while (chunkM.find()) {
            chunks.add(chunkM.toMatchResult());
            // extract chunk-data (and later) in bytes
            int bytes = Integer.parseInt(chunkM.group(1));
            byte[] chunkdata = reply.substring(chunkM.end()).getBytes(StandardCharsets.UTF_8);
            if (bytes > chunkdata.length) {
                log.debug("Error on reply - wrong chunk size {}", reply);
                return false;
            }
            //check if after chunk-size bytes there is next chunk or message ending
            if (chunkdata[bytes] != LF_CHAR || chunkdata[bytes + 1] != HASH_CHAR) {
                log.debug("Error on reply - wrong chunk size {}", reply);
                return false;
            }
            // convert (only) chunk-data part into String
            chunkdataStr = new String(chunkdata, 0, bytes, StandardCharsets.UTF_8);
            // skip chunk-data part from next match
            chunkM.region(chunkM.end() + chunkdataStr.length(), reply.length());
        }
        if (!"\n##\n".equals(reply.substring(chunks.get(chunks.size() - 1).end() + chunkdataStr.length()))) {
            log.debug("Error on reply {}", reply);
            return false;
        }
        return true;
    }

    protected static Optional<Integer> getMsgId(String reply) {
        Matcher matcher = MSGID_PATTERN.matcher(reply);
        if (matcher.find()) {
            try {
                return Optional.of(Integer.valueOf(matcher.group(1)));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse message-id from {}", matcher.group(), e);
            }
        }
        if (reply.contains(HELLO)) {
            return Optional.of(-1);
        }
        return Optional.empty();
    }

    @Override
    public void addDeviceEventListener(NetconfDeviceOutputEventListener listener) {
        if (!netconfDeviceEventListeners.contains(listener)) {
            netconfDeviceEventListeners.add(listener);
        }
    }

    @Override
    public void removeDeviceEventListener(NetconfDeviceOutputEventListener listener) {
        netconfDeviceEventListeners.remove(listener);
    }

    @Override
    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }
}
