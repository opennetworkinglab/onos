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

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.SharedExecutors;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.netconf.AbstractNetconfSession;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEvent.Type;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfSessionFactory;
import org.onosproject.netconf.NetconfTransportException;
import org.slf4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a NETCONF session to talk to a device.
 */
public class NetconfSessionMinaImpl extends AbstractNetconfSession {

    private static final Logger log = getLogger(NetconfSessionMinaImpl.class);

    /**
     * NC 1.0, RFC4742 EOM sequence.
     */
    private static final String ENDPATTERN = "]]>]]>";
    private static final String MESSAGE_ID_STRING = "message-id";
    private static final String HELLO = "<hello";
    private static final String NEW_LINE = "\n";
    private static final String END_OF_RPC_OPEN_TAG = "\">";
    private static final String EQUAL = "=";
    private static final String NUMBER_BETWEEN_QUOTES_MATCHER = "\"+([0-9]+)+\"";
    private static final String SUBTREE_FILTER_CLOSE = "</filter>";
    // FIXME hard coded namespace nc
    private static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    // FIXME hard coded namespace base10
    private static final String SUBSCRIPTION_SUBTREE_FILTER_OPEN =
            "<filter xmlns:base10=\"urn:ietf:params:xml:ns:netconf:base:1.0\" base10:type=\"subtree\">";

    private static final String INTERLEAVE_CAPABILITY_STRING = "urn:ietf:params:netconf:capability:interleave:1.0";

    private static final String CAPABILITY_REGEX = "<capability>\\s*(.*?)\\s*</capability>";
    private static final Pattern CAPABILITY_REGEX_PATTERN = Pattern.compile(CAPABILITY_REGEX);

    private static final String SESSION_ID_REGEX = "<session-id>\\s*(.*?)\\s*</session-id>";
    private static final Pattern SESSION_ID_REGEX_PATTERN = Pattern.compile(SESSION_ID_REGEX);
    private static final String HASH = "#";
    private static final String LF = "\n";
    private static final String MSGLEN_REGEX_PATTERN = "\n#\\d+\n";
    private static final String NETCONF_10_CAPABILITY = "urn:ietf:params:netconf:base:1.0";
    private static final String NETCONF_11_CAPABILITY = "urn:ietf:params:netconf:base:1.1";
    private static final String NETCONF_CLIENT_CAPABILITY = "netconfClientCapability";
    private static final String NOTIFICATION_STREAM = "notificationStream";
    private static final String EMPTY_STRING = "";

    private static ServiceDirectory directory = new DefaultServiceDirectory();

    private String sessionID;
    private final AtomicInteger messageIdInteger = new AtomicInteger(1);
    protected final NetconfDeviceInfo deviceInfo;
    private Iterable<String> onosCapabilities =
            ImmutableList.of(NETCONF_10_CAPABILITY, NETCONF_11_CAPABILITY);

    private final Set<String> deviceCapabilities = new LinkedHashSet<>();
    private NetconfStreamHandler streamHandler;
    // FIXME ONOS-7019 key type should be revised to a String, see RFC6241
    /**
     * Message-ID and corresponding Future waiting for response.
     */
    private Map<Integer, CompletableFuture<String>> replies;
    private List<String> errorReplies; // Not sure why we need this?
    private boolean subscriptionConnected = false;
    private String notificationFilterSchema = null;

    private final Collection<NetconfDeviceOutputEventListener> primaryListeners =
            new CopyOnWriteArrayList<>();
    private final Collection<NetconfSession> children =
            new CopyOnWriteArrayList<>();

    private int connectTimeout;
    private int replyTimeout;
    private int idleTimeout;

    private ClientChannel channel = null;
    private ClientSession session = null;
    private SshClient client = null;

    private boolean disconnected = false;

    public NetconfSessionMinaImpl(NetconfDeviceInfo deviceInfo) throws NetconfException {
        this.deviceInfo = deviceInfo;
        replies = new ConcurrentHashMap<>();
        errorReplies = new ArrayList<>();
        Set<String> capabilities = getClientCapabilites(deviceInfo.getDeviceId());
        if (!capabilities.isEmpty()) {
            capabilities.addAll(Sets.newHashSet(onosCapabilities));
            setOnosCapabilities(capabilities);
        }
        // FIXME should not immediately start session on construction
        // setOnosCapabilities() is useless due to this behavior
        startConnection();
    }

    public NetconfSessionMinaImpl(NetconfDeviceInfo deviceInfo, List<String> capabilities) throws NetconfException {
        this.deviceInfo = deviceInfo;
        replies = new ConcurrentHashMap<>();
        errorReplies = new ArrayList<>();
        setOnosCapabilities(capabilities);
        // FIXME should not immediately start session on construction
        // setOnosCapabilities() is useless due to this behavior
        startConnection();
    }

    /**
     * Get the list of the netconf client capabilities from device driver property.
     *
     * @param deviceId the deviceID for which to recover the capabilities from the driver.
     * @return the String list of clientCapability property, or null if it is not configured
     */
    public Set<String> getClientCapabilites(DeviceId deviceId) {
        Set<String> capabilities = new LinkedHashSet<>();
        DriverService driverService = directory.get(DriverService.class);
        try {
            Driver driver = driverService.getDriver(deviceId);
            if (driver == null) {
                return capabilities;
            }
            String clientCapabilities = driver.getProperty(NETCONF_CLIENT_CAPABILITY);
            if (clientCapabilities == null) {
                return capabilities;
            }
            String[] textStr = clientCapabilities.split("\\|");
            capabilities.addAll(Arrays.asList(textStr));
            return capabilities;
        } catch (ItemNotFoundException e) {
            log.warn("Driver for device {} currently not available", deviceId);
            return capabilities;
        }
    }

    private void startConnection() throws NetconfException {
        connectTimeout = deviceInfo.getConnectTimeoutSec().orElse(
                NetconfControllerImpl.netconfConnectTimeout);
        replyTimeout = deviceInfo.getReplyTimeoutSec().orElse(
                NetconfControllerImpl.netconfReplyTimeout);
        idleTimeout = deviceInfo.getIdleTimeoutSec().orElse(
                NetconfControllerImpl.netconfIdleTimeout);
        log.info("Connecting to {} with timeouts C:{}, R:{}, I:{}", deviceInfo,
                connectTimeout, replyTimeout, idleTimeout);

        try {
            startClient();
        } catch (Exception e) {
            stopClient();
            throw new NetconfException("Failed to establish SSH with device " + deviceInfo, e);
        }
    }

    private void startClient() throws IOException {
        log.info("Creating NETCONF session to {}",
                deviceInfo.getDeviceId());

        client = SshClient.setUpDefaultClient();
        if (idleTimeout != NetconfControllerImpl.netconfIdleTimeout) {
            client.getProperties().putIfAbsent(FactoryManager.IDLE_TIMEOUT,
                    TimeUnit.SECONDS.toMillis(idleTimeout));
            client.getProperties().putIfAbsent(FactoryManager.NIO2_READ_TIMEOUT,
                    TimeUnit.SECONDS.toMillis(idleTimeout + 15L));
        }
        client.start();
        client.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        startSession();

        disconnected = false;
    }

    //TODO: Remove the default methods already implemented in NetconfSession

    // FIXME blocking
    @Deprecated
    private void startSession() throws IOException {
        final ConnectFuture connectFuture;
        connectFuture = client.connect(deviceInfo.name(),
                deviceInfo.ip().toString(),
                deviceInfo.port())
                .verify(connectTimeout, TimeUnit.SECONDS);
        session = connectFuture.getSession();
        //Using the onos private ssh key at path NetconfControllerImpl.sshKeyPath
        String sshKeyPath = NetconfControllerImpl.sshKeyPath;
        if (deviceInfo.password().equals(EMPTY_STRING)) {
            try (PEMParser pemParser = new PEMParser(new FileReader(sshKeyPath))) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
                try {
                    KeyPair kp = converter.getKeyPair((PEMKeyPair) pemParser.readObject());
                    session.addPublicKeyIdentity(kp);
                } catch (IOException e) {
                    throw new NetconfException("Failed to authenticate session. Please check if ssk key is generated" +
" on ONOS host machine at path " + sshKeyPath + " : ", e);
                }
            }
        } else {
            session.addPasswordIdentity(deviceInfo.password());
        }
        session.auth().verify(connectTimeout, TimeUnit.SECONDS);
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

    // FIXME blocking
    @Deprecated
    private void openChannel() throws IOException {
        channel = session.createSubsystemChannel("netconf");
        OpenFuture channelFuture = channel.open();
        if (channelFuture.await(connectTimeout, TimeUnit.SECONDS)) {
            if (channelFuture.isOpened()) {
                streamHandler = new NetconfStreamThread(channel.getInvertedOut(), channel.getInvertedIn(),
                        channel.getInvertedErr(), deviceInfo,
                        new NetconfSessionDelegateImpl(), replies);
                primaryListeners.forEach(l -> streamHandler.addDeviceEventListener(l));
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
        DriverService driverService = directory.get(DriverService.class);
        Driver driver = driverService.getDriver(deviceInfo.getDeviceId());
        if (driver != null) {
            String stream = driver.getProperty(NOTIFICATION_STREAM);
            if (stream != null) {
                subscriptionbuffer.append("    <stream>");
                subscriptionbuffer.append(stream);
                subscriptionbuffer.append("</stream>\n");
            }
        }
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

    private void stopClient() {
        if (session != null) {
            try {
                session.close();
            } catch (IOException ex) {
                log.warn("Cannot close session {} {}", sessionID, deviceInfo, ex);
            }
        }

        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ex) {
                log.warn("Cannot close channel {} {}", sessionID, deviceInfo, ex);
            }
        }

        if (client != null) {
            try {
                client.close();
            } catch (IOException ex) {
                log.warn("Cannot close client {} {}", sessionID, deviceInfo, ex);
            }

            client.stop();
        }
    }

    private void sendHello() throws NetconfException {
        String serverHelloResponse = sendRequest(createHelloString(), true);
        Matcher capabilityMatcher = CAPABILITY_REGEX_PATTERN.matcher(serverHelloResponse);
        while (capabilityMatcher.find()) {
            deviceCapabilities.add(capabilityMatcher.group(1));
        }
        sessionID = String.valueOf(-1);
        Matcher sessionIDMatcher = SESSION_ID_REGEX_PATTERN.matcher(serverHelloResponse);
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
        if (disconnected) {
            log.warn("Can't reopen connection for device because of disconnected {}", deviceInfo.getDeviceId());
            throw new NetconfException("Can't reopen connection for device because of disconnected " + deviceInfo);
        }

        try {
            if (client.isClosed() || client.isClosing()) {
                log.debug("Trying to restart the whole SSH connection with {}", deviceInfo.getDeviceId());
                cleanUp();
                startConnection();
            } else if (session.isClosed() || session.isClosing()) {
                log.debug("Trying to restart the session {} with {}", session, deviceInfo.getDeviceId());
                cleanUp();
                startSession();
            } else if (channel.isClosed() || channel.isClosing()) {
                log.debug("Trying to reopen the channel with {}", deviceInfo.getDeviceId());
                cleanUp();
                openChannel();
            } else {
                return;
            }
            if (subscriptionConnected) {
                log.debug("Restarting subscription with {}", deviceInfo.getDeviceId());
                subscriptionConnected = false;
                startSubscription(notificationFilterSchema);
            }
        } catch (IOException | IllegalStateException e) {
            log.error("Can't reopen connection for device {}", e.getMessage());
            throw new NetconfException("Cannot re-open the connection with device" + deviceInfo, e);
        }
    }

    private void cleanUp() {
        //makes sure everything is at a clean state.
        replies.clear();
        if (streamHandler != null) {
            streamHandler.close();
        }
    }

    @Override
    public String requestSync(String request) throws NetconfException {
        return requestSync(request, replyTimeout);
    }

    @Override
    public String requestSync(String request, int timeout) throws NetconfException {
        String reply = sendRequest(request, timeout);
        if (!checkReply(reply)) {
            throw new NetconfException("Request not successful with device "
                    + deviceInfo + " with reply " + reply);
        }
        return reply;
    }


    // FIXME rename to align with what it actually do

    /**
     * Validate and format netconf message.
     * - NC1.0 if no EOM sequence present on {@code message}, append.
     * - NC1.1 chunk-encode given message unless it already is chunk encoded
     *
     * @param message to format
     * @return formated message
     */
    private String formatNetconfMessage(String message) {
        if (deviceCapabilities.contains(NETCONF_11_CAPABILITY)) {
            message = formatChunkedMessage(message);
        } else {
            if (!message.endsWith(ENDPATTERN)) {
                message = message + NEW_LINE + ENDPATTERN;
            }
        }
        return message;
    }

    /**
     * Validate and format message according to chunked framing mechanism.
     *
     * @param message to format
     * @return formated message
     */
    private String formatChunkedMessage(String message) {
        if (message.endsWith(ENDPATTERN)) {
            // message given had Netconf 1.0 EOM pattern -> remove
            message = message.substring(0, message.length() - ENDPATTERN.length());
        }
        if (!message.startsWith(LF + HASH)) {
            // chunk encode message
            message = LF + HASH + message.getBytes(UTF_8).length + LF + message + LF + HASH + HASH + LF;
        }
        return message;
    }

    @Override
    @Deprecated
    public CompletableFuture<String> request(String request) {
        return streamHandler.sendMessage(request);
    }

    /**
     * {@inheritDoc}
     * <p>
     * FIXME Note: as of 1.12.0
     * {@code request} must not include message-id, this method will assign
     * and insert message-id on it's own.
     * Will require ONOS-7019 to remove this limitation.
     */
    @Override
    public CompletableFuture<String> rpc(String request) {

        String rpc = request;
        //  - assign message-id
        int msgId = messageIdInteger.getAndIncrement();
        //  - re-write request to insert message-id
        // FIXME avoid using formatRequestMessageId
        rpc = formatRequestMessageId(rpc, msgId);
        //  - ensure it contains XML header
        rpc = formatXmlHeader(rpc);
        //  - use chunked framing if talking to NC 1.1 device
        // FIXME avoid using formatNetconfMessage
        rpc = formatNetconfMessage(rpc);

        // TODO session liveness check & recovery

        log.debug("Sending {} to {}", rpc, this.deviceInfo.getDeviceId());
        return streamHandler.sendMessage(rpc, msgId)
                .handleAsync((reply, t) -> {
                    if (t != null) {
                        // secure transport-layer error
                        // cannot use NetconfException, which is
                        // checked Exception.
                        throw new NetconfTransportException(t);
                    } else {
                        // FIXME avoid using checkReply, error handling is weird
                        if (!checkReply(reply)) {
                            throw new NetconfTransportException("rpc-request not successful with device "
                                    + deviceInfo + " with reply " + reply);
                        }
                        return reply;
                    }
                }, SharedExecutors.getPoolThreadExecutor());
    }

    @Override
    public int timeoutConnectSec() {
        return connectTimeout;
    }

    @Override
    public int timeoutReplySec() {
        return replyTimeout;
    }

    @Override
    public int timeoutIdleSec() {
        return idleTimeout;
    }

    private CompletableFuture<String> request(String request, int messageId) {
        return streamHandler.sendMessage(request, messageId);
    }

    private String sendRequest(String request, boolean isHello) throws NetconfException {
        return sendRequest(request, isHello, replyTimeout);
    }

    private String sendRequest(String request) throws NetconfException {
        // FIXME probably chunk-encoding too early
        request = formatNetconfMessage(request);
        return sendRequest(request, false, replyTimeout);
    }

    private String sendRequest(String request, int timeout) throws NetconfException {
        // FIXME probably chunk-encoding too early
        request = formatNetconfMessage(request);
        return sendRequest(request, false, timeout);
    }

    private String sendRequest(String request, boolean isHello, int timeout) throws NetconfException {
        checkAndReestablish();
        int messageId = -1;
        if (!isHello) {
            messageId = messageIdInteger.getAndIncrement();
        }
        // FIXME potentially re-writing chunked encoded String?
        request = formatXmlHeader(request);
        request = formatRequestMessageId(request, messageId);
        int useTimeout = timeout > 0 ? timeout : replyTimeout;
        log.debug("Sending request to NETCONF with timeout {} for {}",
                  useTimeout, deviceInfo.name());
        CompletableFuture<String> futureReply = request(request, messageId);
        String rp;
        try {
            rp = futureReply.get(useTimeout, TimeUnit.SECONDS);
            replies.remove(messageId); // Why here???
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetconfException("Interrupted waiting for reply for request" + request, e);
        } catch (TimeoutException e) {
            throw new NetconfException("Timed out waiting for reply for request " +
                    request + " after " + useTimeout + " sec.", e);
        } catch (ExecutionException e) {
            log.warn("Closing session {} for {} due to unexpected Error", sessionID, deviceInfo, e);
            stopClient();
            NetconfDeviceOutputEvent event = new NetconfDeviceOutputEvent(
                    NetconfDeviceOutputEvent.Type.SESSION_CLOSED,
                    null, "Closed due to unexpected error " + e.getCause(),
                    Optional.of(-1), deviceInfo);
            publishEvent(event);
            errorReplies.clear(); // move to cleanUp()?
            cleanUp();

            throw new NetconfException("Closing session " + sessionID + " for " + deviceInfo +
                    " for request " + request, e);
        }
        log.debug("Result {} from request {} to device {}", rp, request, deviceInfo);
        return rp.trim();
    }

    private String formatRequestMessageId(String request, int messageId) {
        if (request.contains(MESSAGE_ID_STRING)) {
            //FIXME if application provides his own counting of messages this fails that count
            // FIXME assumes message-id is integer. RFC6241 allows anything as long as it is allowed in XML
            request = request.replaceFirst(MESSAGE_ID_STRING + EQUAL + NUMBER_BETWEEN_QUOTES_MATCHER,
                    MESSAGE_ID_STRING + EQUAL + "\"" + messageId + "\"");
        } else if (!request.contains(MESSAGE_ID_STRING) && !request.contains(HELLO)) {
            //FIXME find out a better way to enforce the presence of message-id
            request = request.replaceFirst(END_OF_RPC_OPEN_TAG, "\" " + MESSAGE_ID_STRING + EQUAL + "\""
                    + messageId + "\"" + ">");
        }
        request = updateRequestLength(request);
        return request;
    }

    private String updateRequestLength(String request) {
        if (request.contains(LF + HASH + HASH + LF)) {
            int oldLen = Integer.parseInt(request.split(HASH)[1].split(LF)[0]);
            String rpcWithEnding = request.substring(request.indexOf('<'));
            String firstBlock = request.split(MSGLEN_REGEX_PATTERN)[1].split(LF + HASH + HASH + LF)[0];
            int newLen = 0;
            newLen = firstBlock.getBytes(UTF_8).length;
            if (oldLen != newLen) {
                return LF + HASH + newLen + LF + rpcWithEnding;
            }
        }
        return request;
    }

    /**
     * Ensures xml start directive/declaration appears in the {@code request}.
     *
     * @param request RPC request message
     * @return XML RPC message
     */
    private String formatXmlHeader(String request) {
        if (!request.contains(XML_HEADER)) {
            //FIXME if application provides his own XML header of different type there is a clash
            if (request.startsWith(LF + HASH)) {
                request = request.split("<")[0] + XML_HEADER + request.substring(request.split("<")[0].length());
            } else {
                request = XML_HEADER + "\n" + request;
            }
        }
        return request;
    }

    @Override
    public String getSessionId() {
        return sessionID;
    }

    @Override
    public Set<String> getDeviceCapabilitiesSet() {
        return Collections.unmodifiableSet(deviceCapabilities);
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

    @Override
    protected boolean checkReply(String reply) {
        // Overridden to record error logs
        if (reply != null) {
            if (!reply.contains("<rpc-error>")) {
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            } else if (reply.contains("<ok/>")
                    || (reply.contains("<rpc-error>")
                    && reply.contains("warning"))) {
                // FIXME rpc-error with a warning is considered same as Ok??
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            }
        }
        log.warn("Device {} has error in reply {}", deviceInfo, reply);
        return false;
    }

    @Override
    public boolean close() throws NetconfException {
        try {
            if (client != null && (client.isClosed() || client.isClosing())) {
                return true;
            }

            return super.close();
        } catch (IOException ioe) {
            throw new NetconfException(ioe.getMessage());
        } finally {
            disconnected = true;
            stopClient();
        }
    }

    protected void publishEvent(NetconfDeviceOutputEvent event) {
        primaryListeners.forEach(lsnr -> {
            if (lsnr.isRelevant(event)) {
                lsnr.event(event);
            }
        });
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
            publishEvent(event);
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
            // Remove the message as it has been processed.
            CompletableFuture<String> completedReply = replies.remove(messageId.get());
            if (completedReply != null) {
                completedReply.complete(event.getMessagePayload());
            }
        }
    }

    /**
     * @deprecated in 1.14.0
     */
    @Deprecated
    public static class MinaSshNetconfSessionFactory implements NetconfSessionFactory {

        @Override
        public NetconfSession createNetconfSession(NetconfDeviceInfo netconfDeviceInfo,
                                                   NetconfController netconfController) throws NetconfException {
            return new NetconfSessionMinaImpl(netconfDeviceInfo);
        }
    }
}
