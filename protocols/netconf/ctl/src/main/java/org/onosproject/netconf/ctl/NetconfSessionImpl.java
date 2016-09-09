/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.google.common.base.Preconditions;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Implementation of a NETCONF session to talk to a device.
 */
public class NetconfSessionImpl implements NetconfSession {

    private static final Logger log = LoggerFactory
            .getLogger(NetconfSessionImpl.class);


    private static final int CONNECTION_TIMEOUT = 0;
    private static final String ENDPATTERN = "]]>]]>";
    private static final String MESSAGE_ID_STRING = "message-id";
    private static final String HELLO = "<hello";
    private static final String NEW_LINE = "\n";
    private static final String END_OF_RPC_OPEN_TAG = "\">";
    private static final String EQUAL = "=";
    private static final String NUMBER_BETWEEN_QUOTES_MATCHER = "\"+([0-9]+)+\"";
    private static final String RPC_OPEN = "<rpc ";
    private static final String RPC_CLOSE = "</rpc>";
    private static final String GET_OPEN = "<get>";
    private static final String GET_CLOSE = "</get>";
    private static final String WITH_DEFAULT_OPEN = "<with-defaults ";
    private static final String WITH_DEFAULT_CLOSE = "</with-defaults>";
    private static final String DEFAULT_OPERATION_OPEN = "<default-operation>";
    private static final String DEFAULT_OPERATION_CLOSE = "</default-operation>";
    private static final String SUBTREE_FILTER_OPEN = "<filter type=\"subtree\">";
    private static final String SUBTREE_FILTER_CLOSE = "</filter>";
    private static final String EDIT_CONFIG_OPEN = "<edit-config>";
    private static final String EDIT_CONFIG_CLOSE = "</edit-config>";
    private static final String TARGET_OPEN = "<target>";
    private static final String TARGET_CLOSE = "</target>";
    private static final String CONFIG_OPEN = "<config>";
    private static final String CONFIG_CLOSE = "</config>";
    private static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String NETCONF_BASE_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"";
    private static final String NETCONF_WITH_DEFAULTS_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-with-defaults\"";
    private static final String SUBSCRIPTION_SUBTREE_FILTER_OPEN =
            "<filter xmlns:base10=\"urn:ietf:params:xml:ns:netconf:base:1.0\" base10:type=\"subtree\">";

    private final AtomicInteger messageIdInteger = new AtomicInteger(0);
    private Connection netconfConnection;
    private NetconfDeviceInfo deviceInfo;
    private Session sshSession;
    private boolean connectionActive;
    private List<String> deviceCapabilities =
            Collections.singletonList("urn:ietf:params:netconf:base:1.0");
    private String serverCapabilities;
    private NetconfStreamHandler streamHandler;
    private Map<Integer, CompletableFuture<String>> replies;
    private List<String> errorReplies;
    private boolean subscriptionConnected = false;


    public NetconfSessionImpl(NetconfDeviceInfo deviceInfo) throws NetconfException {
        this.deviceInfo = deviceInfo;
        this.netconfConnection = null;
        this.sshSession = null;
        connectionActive = false;
        replies = new HashMap<>();
        errorReplies = new ArrayList<>();
        startConnection();
    }

    private void startConnection() throws NetconfException {
        if (!connectionActive) {
            netconfConnection = new Connection(deviceInfo.ip().toString(), deviceInfo.port());
            try {
                netconfConnection.connect(null, CONNECTION_TIMEOUT, 5000);
            } catch (IOException e) {
                throw new NetconfException("Cannot open a connection with device" + deviceInfo, e);
            }
            boolean isAuthenticated;
            try {
                if (deviceInfo.getKeyFile() != null) {
                    isAuthenticated = netconfConnection.authenticateWithPublicKey(
                            deviceInfo.name(), deviceInfo.getKeyFile(),
                            deviceInfo.password());
                } else {
                    log.debug("Authenticating to device {} with username {}",
                              deviceInfo.getDeviceId(), deviceInfo.name());
                    isAuthenticated = netconfConnection.authenticateWithPassword(
                            deviceInfo.name(), deviceInfo.password());
                }
            } catch (IOException e) {
                log.error("Authentication connection to device {} failed: {} ",
                          deviceInfo.getDeviceId(), e.getMessage());
                throw new NetconfException("Authentication connection to device " +
                                                   deviceInfo.getDeviceId() + " failed", e);
            }

            connectionActive = true;
            Preconditions.checkArgument(isAuthenticated,
                                        "Authentication to device %s with username " +
                                                "%s failed",
                                        deviceInfo.getDeviceId(), deviceInfo.name());
            startSshSession();
        }
    }

    private void startSshSession() throws NetconfException {
        try {
            sshSession = netconfConnection.openSession();
            sshSession.startSubSystem("netconf");
            streamHandler = new NetconfStreamThread(sshSession.getStdout(), sshSession.getStdin(),
                                                    sshSession.getStderr(), deviceInfo,
                                                    new NetconfSessionDelegateImpl());
            this.addDeviceOutputListener(new NetconfDeviceOutputEventListenerImpl(deviceInfo));
            sendHello();
        } catch (IOException e) {
            log.error("Failed to create ch.ethz.ssh2.Session session." + e.getMessage());
            throw new NetconfException("Failed to create ch.ethz.ssh2.Session session with device" +
                                               deviceInfo, e);
        }
    }


    @Beta
    private void startSubscriptionConnection(String filterSchema) throws NetconfException {
        if (!serverCapabilities.contains("interleave")) {
            throw new NetconfException("Device" + deviceInfo + "does not support interleave");
        }
        String reply = sendRequest(createSubscriptionString(filterSchema));
        if (!checkReply(reply)) {
            throw new NetconfException("Subscription not successful with device "
                                               + deviceInfo + " with reply " + reply);
        }
        subscriptionConnected = true;
    }

    @Override
    public void startSubscription() throws NetconfException {
        if (!subscriptionConnected) {
            startSubscriptionConnection(null);
        }
        streamHandler.setEnableNotifications(true);
    }

    @Beta
    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
        if (!subscriptionConnected) {
            startSubscriptionConnection(filterSchema);
        }
        streamHandler.setEnableNotifications(true);
    }

    @Beta
    private String createSubscriptionString(String filterSchema) {
        StringBuilder subscriptionbuffer = new StringBuilder();
        subscriptionbuffer.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        subscriptionbuffer.append("  <create-subscription\n");
        subscriptionbuffer.append("xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\">\n");
        // FIXME Only subtree filtering supported at the moment.
        if (filterSchema != null) {
            subscriptionbuffer.append("    ");
            subscriptionbuffer.append(SUBSCRIPTION_SUBTREE_FILTER_OPEN).append(NEW_LINE);
            subscriptionbuffer.append(filterSchema).append(NEW_LINE);
            subscriptionbuffer.append("    ");
            subscriptionbuffer.append(SUBTREE_FILTER_CLOSE).append(NEW_LINE);
        }
        subscriptionbuffer.append("  </create-subscription>\n");
        subscriptionbuffer.append("</rpc>\n");
        subscriptionbuffer.append(ENDPATTERN);
        return subscriptionbuffer.toString();
    }

    @Override
    public void endSubscription() throws NetconfException {
        if (subscriptionConnected) {
            streamHandler.setEnableNotifications(false);
        } else {
            throw new NetconfException("Subscription does not exist.");
        }
    }

    private void sendHello() throws NetconfException {
        serverCapabilities = sendRequest(createHelloString());
    }

    private String createHelloString() {
        StringBuilder hellobuffer = new StringBuilder();
        hellobuffer.append(XML_HEADER);
        hellobuffer.append("\n");
        hellobuffer.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        hellobuffer.append("  <capabilities>\n");
        deviceCapabilities.forEach(
                cap -> hellobuffer.append("    <capability>")
                        .append(cap)
                        .append("</capability>\n"));
        hellobuffer.append("  </capabilities>\n");
        hellobuffer.append("</hello>\n");
        hellobuffer.append(ENDPATTERN);
        return hellobuffer.toString();

    }

    private void checkAndRestablishSession() throws NetconfException {
        if (sshSession.getState() != 2) {
            try {
                startSshSession();
            } catch (IOException e) {
                log.debug("The connection with {} had to be reopened", deviceInfo.getDeviceId());
                try {
                    startConnection();
                } catch (IOException e2) {
                    log.error("No connection {} for device", netconfConnection, e2);
                    throw new NetconfException("Cannot re-open the connection with device" + deviceInfo, e);
                }
            }
        }
    }

    @Override
    public String requestSync(String request) throws NetconfException {
        if (!request.contains(ENDPATTERN)) {
            request = request + NEW_LINE + ENDPATTERN;
        }
        String reply = sendRequest(request);
        checkReply(reply);
        return reply;
    }

    @Override
    public CompletableFuture<String> request(String request) {
        CompletableFuture<String> ftrep = streamHandler.sendMessage(request);
        replies.put(messageIdInteger.get(), ftrep);
        return ftrep;
    }

    private String sendRequest(String request) throws NetconfException {
        checkAndRestablishSession();
        request = formatRequestMessageId(request);
        request = formatXmlHeader(request);
        CompletableFuture<String> futureReply = request(request);
        messageIdInteger.incrementAndGet();
        int replyTimeout = NetconfControllerImpl.netconfReplyTimeout;
        String rp;
        try {
            rp = futureReply.get(replyTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new NetconfException("No matching reply for request " + request, e);
        }
        log.debug("Result {} from request {} to device {}", rp, request, deviceInfo);
        return rp.trim();
    }

    private String formatRequestMessageId(String request) {
        if (request.contains(MESSAGE_ID_STRING)) {
            //FIXME if application provieds his own counting of messages this fails that count
            request = request.replaceFirst(MESSAGE_ID_STRING + EQUAL + NUMBER_BETWEEN_QUOTES_MATCHER,
                                           MESSAGE_ID_STRING + EQUAL + "\"" + messageIdInteger.get() + "\"");
        } else if (!request.contains(MESSAGE_ID_STRING) && !request.contains(HELLO)) {
            //FIXME find out a better way to enforce the presence of message-id
            request = request.replaceFirst(END_OF_RPC_OPEN_TAG, "\" " + MESSAGE_ID_STRING + EQUAL + "\""
                    + messageIdInteger.get() + "\"" + ">");
        }
        return request;
    }

    private String formatXmlHeader(String request) {
        if (!request.contains(XML_HEADER)) {
            //FIXME if application provieds his own XML header of different type there is a clash
            request = XML_HEADER + "\n" + request;
        }
        return request;
    }

    @Override
    public String doWrappedRpc(String request) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(request);
        rpc.append(RPC_CLOSE).append(NEW_LINE);
        rpc.append(ENDPATTERN);
        String reply = sendRequest(rpc.toString());
        checkReply(reply);
        return reply;
    }

    @Override
    public String get(String request) throws NetconfException {
        return requestSync(request);
    }

    @Override
    public String get(String filterSchema, String withDefaultsMode) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(GET_OPEN).append(NEW_LINE);
        if (filterSchema != null) {
            rpc.append(SUBTREE_FILTER_OPEN).append(NEW_LINE);
            rpc.append(filterSchema).append(NEW_LINE);
            rpc.append(SUBTREE_FILTER_CLOSE).append(NEW_LINE);
        }
        if (withDefaultsMode != null) {
            rpc.append(WITH_DEFAULT_OPEN).append(NETCONF_WITH_DEFAULTS_NAMESPACE).append(">");
            rpc.append(withDefaultsMode).append(WITH_DEFAULT_CLOSE).append(NEW_LINE);
        }
        rpc.append(GET_CLOSE).append(NEW_LINE);
        rpc.append(RPC_CLOSE).append(NEW_LINE);
        rpc.append(ENDPATTERN);
        String reply = sendRequest(rpc.toString());
        checkReply(reply);
        return reply;
    }

    @Override
    public String getConfig(String targetConfiguration) throws NetconfException {
        return getConfig(targetConfiguration, null);
    }

    @Override
    public String getConfig(String targetConfiguration, String configurationSchema) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc ");
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append("xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<get-config>\n");
        rpc.append("<source>\n");
        rpc.append("<").append(targetConfiguration).append("/>");
        rpc.append("</source>");
        if (configurationSchema != null) {
            rpc.append("<filter type=\"subtree\">\n");
            rpc.append(configurationSchema).append("\n");
            rpc.append("</filter>\n");
        }
        rpc.append("</get-config>\n");
        rpc.append("</rpc>\n");
        rpc.append(ENDPATTERN);
        String reply = sendRequest(rpc.toString());
        return checkReply(reply) ? reply : "ERROR " + reply;
    }

    @Override
    public boolean editConfig(String newConfiguration) throws NetconfException {
        newConfiguration = newConfiguration + ENDPATTERN;
        return checkReply(sendRequest(newConfiguration));
    }

    @Override
    public boolean editConfig(String targetConfiguration, String mode, String newConfiguration)
            throws NetconfException {
        newConfiguration = newConfiguration.trim();
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(EDIT_CONFIG_OPEN).append("\n");
        rpc.append(TARGET_OPEN);
        rpc.append("<").append(targetConfiguration).append("/>");
        rpc.append(TARGET_CLOSE).append("\n");
        if (mode != null) {
            rpc.append(DEFAULT_OPERATION_OPEN);
            rpc.append(mode);
            rpc.append(DEFAULT_OPERATION_CLOSE).append("\n");
        }
        rpc.append(CONFIG_OPEN).append("\n");
        rpc.append(newConfiguration);
        rpc.append(CONFIG_CLOSE).append("\n");
        rpc.append(EDIT_CONFIG_CLOSE).append("\n");
        rpc.append(RPC_CLOSE);
        rpc.append(ENDPATTERN);
        log.debug(rpc.toString());
        String reply = sendRequest(rpc.toString());
        return checkReply(reply);
    }

    @Override
    public boolean copyConfig(String targetConfiguration, String newConfiguration)
            throws NetconfException {
        newConfiguration = newConfiguration.trim();
        if (!newConfiguration.startsWith("<configuration>")) {
            newConfiguration = "<configuration>" + newConfiguration
                    + "</configuration>";
        }
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc>");
        rpc.append("<copy-config>");
        rpc.append("<target>");
        rpc.append("<").append(targetConfiguration).append("/>");
        rpc.append("</target>");
        rpc.append("<source>");
        rpc.append("<").append(newConfiguration).append("/>");
        rpc.append("</source>");
        rpc.append("</copy-config>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        return checkReply(sendRequest(rpc.toString()));
    }

    @Override
    public boolean deleteConfig(String targetConfiguration) throws NetconfException {
        if (targetConfiguration.equals("running")) {
            log.warn("Target configuration for delete operation can't be \"running\"",
                     targetConfiguration);
            return false;
        }
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc>");
        rpc.append("<delete-config>");
        rpc.append("<target>");
        rpc.append("<").append(targetConfiguration).append("/>");
        rpc.append("</target>");
        rpc.append("</delete-config>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        return checkReply(sendRequest(rpc.toString()));
    }

    @Override
    public boolean lock(String configType) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<lock>");
        rpc.append("<target>");
        rpc.append("<");
        rpc.append(configType);
        rpc.append("/>");
        rpc.append("</target>");
        rpc.append("</lock>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        String lockReply = sendRequest(rpc.toString());
        return checkReply(lockReply);
    }

    @Override
    public boolean unlock(String configType) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<unlock>");
        rpc.append("<target>");
        rpc.append("<");
        rpc.append(configType);
        rpc.append("/>");
        rpc.append("</target>");
        rpc.append("</unlock>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        String unlockReply = sendRequest(rpc.toString());
        return checkReply(unlockReply);
    }

    @Override
    public boolean lock() throws NetconfException {
        return lock("running");
    }

    @Override
    public boolean unlock() throws NetconfException {
        return unlock("running");
    }

    @Override
    public boolean close() throws NetconfException {
        return close(false);
    }

    private boolean close(boolean force) throws NetconfException {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        if (force) {
            rpc.append("<kill-session/>");
        } else {
            rpc.append("<close-session/>");
        }
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        return checkReply(sendRequest(rpc.toString())) || close(true);
    }

    @Override
    public String getSessionId() {
        if (serverCapabilities.contains("<session-id>")) {
            String[] outer = serverCapabilities.split("<session-id>");
            Preconditions.checkArgument(outer.length != 1,
                                        "Error in retrieving the session id");
            String[] value = outer[1].split("</session-id>");
            Preconditions.checkArgument(value.length != 1,
                                        "Error in retrieving the session id");
            return value[0];
        } else {
            return String.valueOf(-1);
        }
    }

    @Override
    public String getServerCapabilities() {
        return serverCapabilities;
    }

    @Override
    public void setDeviceCapabilities(List<String> capabilities) {
        deviceCapabilities = capabilities;
    }

    @Override
    public void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        streamHandler.addDeviceEventListener(listener);
    }

    @Override
    public void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        streamHandler.removeDeviceEventListener(listener);
    }

    private boolean checkReply(String reply) throws NetconfException {
        if (reply != null) {
            if (!reply.contains("<rpc-error>")) {
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            } else if (reply.contains("<ok/>")
                    || (reply.contains("<rpc-error>")
                    && reply.contains("warning"))) {
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            }
        }
        log.warn("Device {} has error in reply {}", deviceInfo, reply);
        return false;
    }

    public class NetconfSessionDelegateImpl implements NetconfSessionDelegate {

        @Override
        public void notify(NetconfDeviceOutputEvent event)  {
            Optional<Integer> messageId = event.getMessageID();

            if (!messageId.isPresent()) {
                errorReplies.add(event.getMessagePayload());
                log.error("Device {} sent error reply {}",
                          event.getDeviceInfo(), event.getMessagePayload());
                return;
            }
            CompletableFuture<String> completedReply =
                    replies.get(messageId.get());
            if (completedReply != null) {
                completedReply.complete(event.getMessagePayload());
            }
        }
    }
}
