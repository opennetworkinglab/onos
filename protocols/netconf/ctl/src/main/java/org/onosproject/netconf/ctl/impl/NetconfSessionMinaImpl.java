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

package org.onosproject.netconf.ctl.impl;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEvent.Type;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of a NETCONF session to talk to a device.
 */
public class NetconfSessionMinaImpl implements NetconfSession {

    private static final Logger log = LoggerFactory
            .getLogger(NetconfSessionMinaImpl.class);

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
    private static final String CONFIG_OPEN = "<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String CONFIG_CLOSE = "</config>";
    private static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String NETCONF_BASE_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"";
    private static final String NETCONF_WITH_DEFAULTS_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-with-defaults\"";
    private static final String SUBSCRIPTION_SUBTREE_FILTER_OPEN =
            "<filter xmlns:base10=\"urn:ietf:params:xml:ns:netconf:base:1.0\" base10:type=\"subtree\">";

    private static final String INTERLEAVE_CAPABILITY_STRING = "urn:ietf:params:netconf:capability:interleave:1.0";

    private static final String CAPABILITY_REGEX = "<capability>\\s*(.*?)\\s*</capability>";
    private static final Pattern CAPABILITY_REGEX_PATTERN = Pattern.compile(CAPABILITY_REGEX);

    private static final String SESSION_ID_REGEX = "<session-id>\\s*(.*?)\\s*</session-id>";
    private static final Pattern SESSION_ID_REGEX_PATTERN = Pattern.compile(SESSION_ID_REGEX);
    private static final String RSA = "RSA";
    private static final String DSA = "DSA";

    private String sessionID;
    private final AtomicInteger messageIdInteger = new AtomicInteger(1);
    protected final NetconfDeviceInfo deviceInfo;
    private Iterable<String> onosCapabilities =
            Collections.singletonList("urn:ietf:params:netconf:base:1.0");

    /* NOTE: the "serverHelloResponseOld" is deprecated in 1.10.0 and should eventually be removed */
    @Deprecated
    private String serverHelloResponseOld;
    private final Set<String> deviceCapabilities = new LinkedHashSet<>();
    private NetconfStreamHandler streamHandler;
    private Map<Integer, CompletableFuture<String>> replies;
    private List<String> errorReplies;
    private boolean subscriptionConnected = false;
    private String notificationFilterSchema = null;

    private final Collection<NetconfDeviceOutputEventListener> primaryListeners =
            new CopyOnWriteArrayList<>();
    private final Collection<NetconfSession> children =
            new CopyOnWriteArrayList<>();


    private ClientChannel channel = null;
    private ClientSession session = null;
    private SshClient client = null;


    public NetconfSessionMinaImpl(NetconfDeviceInfo deviceInfo) throws NetconfException {
        this.deviceInfo = deviceInfo;
        replies = new ConcurrentHashMap<>();
        errorReplies = new ArrayList<>();
        startConnection();
    }

    private void startConnection() throws NetconfException {
        try {
            startClient();
        } catch (IOException e) {
            throw new NetconfException("Failed to establish SSH with device " + deviceInfo, e);
        }
    }

    private void startClient() throws IOException {
        client = SshClient.setUpDefaultClient();
        client.start();
        client.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        startSession();
    }

    private void startSession() throws IOException {
        final ConnectFuture connectFuture;
        connectFuture = client.connect(deviceInfo.name(),
                                       deviceInfo.ip().toString(),
                                       deviceInfo.port())
                .verify(NetconfControllerImpl.netconfConnectTimeout, TimeUnit.SECONDS);
        session = connectFuture.getSession();
        //Using the device ssh key if possible
        if (deviceInfo.getKey() != null) {
            ByteBuffer buf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(deviceInfo.getKey()));
            byte[] byteKey = new byte[buf.limit()];
            buf.get(byteKey);
            PublicKey key;
            try {
                key = getPublicKey(byteKey, RSA);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                try {
                    key = getPublicKey(byteKey, DSA);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
                    throw new NetconfException("Failed to authenticate session with device " +
                                                       deviceInfo + "check key to be the " +
                                                       "proper DSA or RSA key", e1);
                }
            }
            //privateKye can set tu null because is not used by the method.
            session.addPublicKeyIdentity(new KeyPair(key, null));
        } else {
            session.addPasswordIdentity(deviceInfo.password());
        }
        session.auth().verify(NetconfControllerImpl.netconfConnectTimeout, TimeUnit.SECONDS);
        Set<ClientSession.ClientSessionEvent> event = session.waitFor(
                ImmutableSet.of(ClientSession.ClientSessionEvent.WAIT_AUTH,
                                ClientSession.ClientSessionEvent.CLOSED,
                                ClientSession.ClientSessionEvent.AUTHED), 0);

        if (!event.contains(ClientSession.ClientSessionEvent.AUTHED)) {
            log.debug("Session closed {} {}", event, session.isClosed());
            throw new NetconfException("Failed to authenticate session with device " +
                                               deviceInfo + "check the user/pwd or key");
        }
        openChannel();
    }

    private PublicKey getPublicKey(byte[] keyBytes, String type)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(type);
        return kf.generatePublic(spec);
    }

    private void openChannel() throws IOException {
        channel = session.createSubsystemChannel("netconf");
        OpenFuture channelFuture = channel.open();
        if (channelFuture.await(NetconfControllerImpl.netconfConnectTimeout, TimeUnit.SECONDS)) {
            if (channelFuture.isOpened()) {
                streamHandler = new NetconfStreamThread(channel.getInvertedOut(), channel.getInvertedIn(),
                                                        channel.getInvertedErr(), deviceInfo,
                                                        new NetconfSessionDelegateImpl(), replies);
            } else {
                throw new NetconfException("Failed to open channel with device " +
                                                   deviceInfo);
            }
            sendHello();
        }
    }


    @Beta
    protected void startSubscriptionStream(String filterSchema) throws NetconfException {
        boolean openNewSession = false;
        if (!deviceCapabilities.contains(INTERLEAVE_CAPABILITY_STRING)) {
            log.info("Device {} doesn't support interleave, creating child session", deviceInfo);
            openNewSession = true;

        } else if (subscriptionConnected &&
                notificationFilterSchema != null &&
                !Objects.equal(filterSchema, notificationFilterSchema)) {
            // interleave supported and existing filter is NOT "no filtering"
            // and was requested with different filtering schema
            log.info("Cannot use existing session for subscription {} ({})",
                     deviceInfo, filterSchema);
            openNewSession = true;
        }

        if (openNewSession) {
            log.info("Creating notification session to {} with filter {}",
                     deviceInfo, filterSchema);
            NetconfSession child = new NotificationSession(deviceInfo);

            child.addDeviceOutputListener(new NotificationForwarder());

            child.startSubscription(filterSchema);
            children.add(child);
            return;
        }

        // request to start interleaved notification session
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
            startSubscriptionStream(null);
        }
        streamHandler.setEnableNotifications(true);
    }

    @Beta
    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
        if (!subscriptionConnected) {
            notificationFilterSchema = filterSchema;
            startSubscriptionStream(filterSchema);
        }
        streamHandler.setEnableNotifications(true);
    }

    @Beta
    protected String createSubscriptionString(String filterSchema) {
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
        serverHelloResponseOld = sendRequest(createHelloString(), true);
        Matcher capabilityMatcher = CAPABILITY_REGEX_PATTERN.matcher(serverHelloResponseOld);
        while (capabilityMatcher.find()) {
            deviceCapabilities.add(capabilityMatcher.group(1));
        }
        sessionID = String.valueOf(-1);
        Matcher sessionIDMatcher = SESSION_ID_REGEX_PATTERN.matcher(serverHelloResponseOld);
        if (sessionIDMatcher.find()) {
            sessionID = sessionIDMatcher.group(1);
        } else {
            throw new NetconfException("Missing SessionID in server hello " +
                                               "reponse.");
        }

    }

    private String createHelloString() {
        StringBuilder hellobuffer = new StringBuilder();
        hellobuffer.append(XML_HEADER);
        hellobuffer.append("\n");
        hellobuffer.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        hellobuffer.append("  <capabilities>\n");
        onosCapabilities.forEach(
                cap -> hellobuffer.append("    <capability>")
                        .append(cap)
                        .append("</capability>\n"));
        hellobuffer.append("  </capabilities>\n");
        hellobuffer.append("</hello>\n");
        hellobuffer.append(ENDPATTERN);
        return hellobuffer.toString();

    }

    @Override
    public void checkAndReestablish() throws NetconfException {
        try {
            if (client.isClosed()) {
                log.debug("Trying to restart the whole SSH connection with {}", deviceInfo.getDeviceId());
                cleanUp();
                startConnection();
            } else if (session.isClosed()) {
                log.debug("Trying to restart the session with {}", session, deviceInfo.getDeviceId());
                cleanUp();
                startSession();
            } else if (channel.isClosed()) {
                log.debug("Trying to reopen the channel with {}", deviceInfo.getDeviceId());
                cleanUp();
                openChannel();
            }
            if (subscriptionConnected) {
                log.debug("Restarting subscription with {}", deviceInfo.getDeviceId());
                subscriptionConnected = false;
                startSubscription(notificationFilterSchema);
            }
        } catch (IOException e) {
            log.error("Can't reopen connection for device {}", e.getMessage());
            throw new NetconfException("Cannot re-open the connection with device" + deviceInfo, e);
        }
    }

    private void cleanUp() {
        //makes sure everything is at a clean state.
        replies.clear();
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
    @Deprecated
    public CompletableFuture<String> request(String request) {
        return streamHandler.sendMessage(request);
    }

    private CompletableFuture<String> request(String request, int messageId) {
        return streamHandler.sendMessage(request, messageId);
    }

    private String sendRequest(String request) throws NetconfException {
        return sendRequest(request, false);
    }

    private String sendRequest(String request, boolean isHello) throws NetconfException {
        checkAndReestablish();
        int messageId = -1;
        if (!isHello) {
            messageId = messageIdInteger.getAndIncrement();
        }
        request = formatRequestMessageId(request, messageId);
        request = formatXmlHeader(request);
        CompletableFuture<String> futureReply = request(request, messageId);
        int replyTimeout = NetconfControllerImpl.netconfReplyTimeout;
        String rp;
        try {
            rp = futureReply.get(replyTimeout, TimeUnit.SECONDS);
            replies.remove(messageId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new NetconfException("No matching reply for request " + request, e);
        }
        log.debug("Result {} from request {} to device {}", rp, request, deviceInfo);
        return rp.trim();
    }

    private String formatRequestMessageId(String request, int messageId) {
        if (request.contains(MESSAGE_ID_STRING)) {
            //FIXME if application provides his own counting of messages this fails that count
            request = request.replaceFirst(MESSAGE_ID_STRING + EQUAL + NUMBER_BETWEEN_QUOTES_MATCHER,
                                           MESSAGE_ID_STRING + EQUAL + "\"" + messageId + "\"");
        } else if (!request.contains(MESSAGE_ID_STRING) && !request.contains(HELLO)) {
            //FIXME find out a better way to enforce the presence of message-id
            request = request.replaceFirst(END_OF_RPC_OPEN_TAG, "\" " + MESSAGE_ID_STRING + EQUAL + "\""
                    + messageId + "\"" + ">");
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
    public String getConfig(DatastoreId netconfTargetConfig) throws NetconfException {
        return getConfig(netconfTargetConfig, null);
    }

    @Override
    public String getConfig(DatastoreId netconfTargetConfig,
                            String configurationSchema) throws NetconfException {
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
        rpc.append("<").append(netconfTargetConfig).append("/>");
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
    public boolean editConfig(DatastoreId netconfTargetConfig,
                              String mode,
                              String newConfiguration)
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
        rpc.append("<").append(netconfTargetConfig).append("/>");
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
    public boolean copyConfig(DatastoreId destination,
                              DatastoreId source)
            throws NetconfException {
        return bareCopyConfig(destination.asXml(), source.asXml());
    }

    @Override
    public boolean copyConfig(DatastoreId netconfTargetConfig,
                              String newConfiguration)
            throws NetconfException {
        return bareCopyConfig(netconfTargetConfig.asXml(),
                              normalizeCopyConfigParam(newConfiguration));
    }

    @Override
    public boolean copyConfig(String netconfTargetConfig,
                              String newConfiguration) throws NetconfException {
        return bareCopyConfig(normalizeCopyConfigParam(netconfTargetConfig),
                              normalizeCopyConfigParam(newConfiguration));
    }

    /**
     * Normalize String parameter passed to copy-config API.
     * <p>
     * Provided for backward compatibility purpose
     *
     * @param input passed to copyConfig API
     * @return XML likely to be suitable for copy-config source or target
     */
    private static CharSequence normalizeCopyConfigParam(String input) {
        input = input.trim();
        if (input.startsWith("<url")) {
            return input;
        } else if (!input.startsWith("<")) {
            // assume it is a datastore name
            return DatastoreId.datastore(input).asXml();
        } else if (!input.startsWith("<config>")) {
            return "<config>" + input + "</config>";
        }
        return input;
    }

    private boolean bareCopyConfig(CharSequence target,
                                   CharSequence source)
            throws NetconfException {

        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append("<copy-config>");
        rpc.append("<target>");
        rpc.append(target);
        rpc.append("</target>");
        rpc.append("<source>");
        rpc.append(source);
        rpc.append("</source>");
        rpc.append("</copy-config>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        return checkReply(sendRequest(rpc.toString()));
    }

    @Override
    public boolean deleteConfig(DatastoreId netconfTargetConfig) throws NetconfException {
        if (netconfTargetConfig.equals(DatastoreId.RUNNING)) {
            log.warn("Target configuration for delete operation can't be \"running\"",
                     netconfTargetConfig);
            return false;
        }
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc>");
        rpc.append("<delete-config>");
        rpc.append("<target>");
        rpc.append("<").append(netconfTargetConfig).append("/>");
        rpc.append("</target>");
        rpc.append("</delete-config>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        return checkReply(sendRequest(rpc.toString()));
    }

    @Override
    public boolean lock(DatastoreId configType) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<lock>");
        rpc.append("<target>");
        rpc.append("<");
        rpc.append(configType.id());
        rpc.append("/>");
        rpc.append("</target>");
        rpc.append("</lock>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        String lockReply = sendRequest(rpc.toString());
        return checkReply(lockReply);
    }

    @Override
    public boolean unlock(DatastoreId configType) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<unlock>");
        rpc.append("<target>");
        rpc.append("<");
        rpc.append(configType.id());
        rpc.append("/>");
        rpc.append("</target>");
        rpc.append("</unlock>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        String unlockReply = sendRequest(rpc.toString());
        return checkReply(unlockReply);
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
        return sessionID;
    }

    @Override
    public Set<String> getDeviceCapabilitiesSet() {
        return Collections.unmodifiableSet(deviceCapabilities);
    }

    @Deprecated
    @Override
    public String getServerCapabilities() {
        return serverHelloResponseOld;
    }

    @Deprecated
    @Override
    public void setDeviceCapabilities(List<String> capabilities) {
        onosCapabilities = capabilities;
    }

    @Override
    public void setOnosCapabilities(Iterable<String> capabilities) {
        onosCapabilities = capabilities;
    }


    @Override
    public void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        streamHandler.addDeviceEventListener(listener);
        primaryListeners.add(listener);
    }

    @Override
    public void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        primaryListeners.remove(listener);
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

    static class NotificationSession extends NetconfSessionMinaImpl {

        private String notificationFilter;

        NotificationSession(NetconfDeviceInfo deviceInfo)
                throws NetconfException {
            super(deviceInfo);
        }

        @Override
        protected void startSubscriptionStream(String filterSchema)
                throws NetconfException {

            notificationFilter = filterSchema;
            requestSync(createSubscriptionString(filterSchema));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("deviceInfo", deviceInfo)
                    .add("sessionID", getSessionId())
                    .add("notificationFilter", notificationFilter)
                    .toString();
        }
    }

    /**
     * Listener attached to child session for notification streaming.
     * <p>
     * Forwards all notification event from child session to primary session
     * listeners.
     */
    private final class NotificationForwarder
            implements NetconfDeviceOutputEventListener {

        @Override
        public boolean isRelevant(NetconfDeviceOutputEvent event) {
            return event.type() == Type.DEVICE_NOTIFICATION;
        }

        @Override
        public void event(NetconfDeviceOutputEvent event) {
            primaryListeners.forEach(lsnr -> {
                if (lsnr.isRelevant(event)) {
                    lsnr.event(event);
                }
            });
        }
    }

    public class NetconfSessionDelegateImpl implements NetconfSessionDelegate {

        @Override
        public void notify(NetconfDeviceOutputEvent event) {
            Optional<Integer> messageId = event.getMessageID();
            log.debug("messageID {}, waiting replies messageIDs {}", messageId,
                      replies.keySet());
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

    public static class MinaSshNetconfSessionFactory implements NetconfSessionFactory {

        @Override
        public NetconfSession createNetconfSession(NetconfDeviceInfo netconfDeviceInfo) throws NetconfException {
            return new NetconfSessionMinaImpl(netconfDeviceInfo);
        }
    }
}
