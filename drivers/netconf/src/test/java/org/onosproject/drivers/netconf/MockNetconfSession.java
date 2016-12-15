/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.drivers.netconf;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockNetconfSession implements NetconfSession {
    private static final Logger log = LoggerFactory
            .getLogger(MockNetconfSession.class);

    private static final String MESSAGE_ID_STRING = "message-id";
    private static final String EQUAL = "=";
    private static final String RPC_OPEN = "<rpc ";
    private static final String RPC_CLOSE = "</rpc>";
    private static final String GET_OPEN = "<get>";
    private static final String GET_CLOSE = "</get>";
    private static final String NEW_LINE = "\n";
    private static final String SUBTREE_FILTER_OPEN = "<filter type=\"subtree\">";
    private static final String SUBTREE_FILTER_CLOSE = "</filter>";
    private static final String WITH_DEFAULT_OPEN = "<with-defaults ";
    private static final String WITH_DEFAULT_CLOSE = "</with-defaults>";
    private static final String EDIT_CONFIG_OPEN = "<edit-config>";
    private static final String EDIT_CONFIG_CLOSE = "</edit-config>";
    private static final String COPY_CONFIG_OPEN = "<copy-config>";
    private static final String COPY_CONFIG_CLOSE = "</copy-config>";
    private static final String DELETE_CONFIG_OPEN = "<delete-config>";
    private static final String DELETE_CONFIG_CLOSE = "</delete-config>";
    private static final String TARGET_OPEN = "<target>";
    private static final String TARGET_CLOSE = "</target>";
    private static final String SOURCE_OPEN = "<source>";
    private static final String SOURCE_CLOSE = "</source>";
    private static final String DEFAULT_OPERATION_OPEN = "<default-operation>";
    private static final String DEFAULT_OPERATION_CLOSE = "</default-operation>";
    private static final String CONFIG_OPEN = "<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String CONFIG_CLOSE = "</config>";

    private static final String ENDPATTERN = "]]>]]>";
    private static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String NETCONF_BASE_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"";
    private static final String NETCONF_WITH_DEFAULTS_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-with-defaults\"";

    private Pattern simpleGetConfig =
            Pattern.compile("(<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>)\\R?"
                    + "(<rpc message-id=\")[0-9]*(\"  xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<get-config>)\\R?"
                    + "(<source>)\\R?(<running/>)\\R?(</source>)\\R?"
                    + "(</get-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private static final String SAMPLE_MSEAEVCUNI_REPLY_INIT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n"
            + "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
            + "</data>\n"
            + "</rpc-reply>";



    private NetconfDeviceInfo deviceInfo;

    private final AtomicInteger messageIdInteger = new AtomicInteger(0);

    public MockNetconfSession(NetconfDeviceInfo deviceInfo) throws NetconfException {
        this.deviceInfo = deviceInfo;
    }

    @Override
    public CompletableFuture<String> request(String request) throws NetconfException {
        throw new NetconfException("Should be calling a higher level command or one that sets the message id");
    }

    @Override
    public String get(String request) throws NetconfException {

        return sendRequest(request);
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
    public String requestSync(String request) throws NetconfException {
        if (!request.contains(ENDPATTERN)) {
            request = request + NEW_LINE + ENDPATTERN;
        }
        String reply = sendRequest(request);
        checkReply(reply);
        return reply;
    }


    @Override
    public String getConfig(String targetConfiguration, String configurationSchema) throws NetconfException {
        return getConfig(TargetConfig.valueOf(targetConfiguration));
    }

    @Override
    public String getConfig(String targetConfiguration) throws NetconfException {
        return getConfig(TargetConfig.valueOf(targetConfiguration), null);
    }

    @Override
    public String getConfig(TargetConfig netconfTargetConfig)
            throws NetconfException {
        return getConfig(netconfTargetConfig, null);
    }

    @Override
    public String getConfig(TargetConfig netconfTargetConfig, String configurationFilterSchema)
            throws NetconfException {
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
        if (netconfTargetConfig != null) {
            rpc.append("<filter type=\"subtree\">\n");
            rpc.append(configurationFilterSchema).append("\n");
            rpc.append("</filter>\n");
        }
        rpc.append("</get-config>\n");
        rpc.append("</rpc>\n");
        rpc.append(ENDPATTERN);
        String reply = sendRequest(rpc.toString());
        return checkReply(reply) ? reply : "ERROR " + reply;
    }

    @Override
    public boolean editConfig(TargetConfig netconfTargetConfig, String mode, String newConfiguration)
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
    public boolean editConfig(String newConfiguration) throws NetconfException {
        return editConfig(TargetConfig.RUNNING, null, newConfiguration);
    }

    @Override
    public boolean editConfig(String targetConfiguration, String mode, String newConfiguration)
            throws NetconfException {
        return editConfig(TargetConfig.valueOf(targetConfiguration), mode, newConfiguration);
    }

    @Override
    public boolean copyConfig(TargetConfig netconfTargetConfig, String newConfiguration) throws NetconfException {
        newConfiguration = newConfiguration.trim();
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(COPY_CONFIG_OPEN).append("\n");
        rpc.append(TARGET_OPEN);
        rpc.append("<").append(netconfTargetConfig).append("/>");
        rpc.append(TARGET_CLOSE).append("\n");
        rpc.append(SOURCE_OPEN);
        rpc.append("<").append(newConfiguration).append("/>");
        rpc.append(SOURCE_CLOSE).append("\n");
        rpc.append(COPY_CONFIG_CLOSE).append("\n");
        rpc.append(RPC_CLOSE);
        rpc.append(ENDPATTERN);
        log.debug(rpc.toString());
        String reply = sendRequest(rpc.toString());
        return checkReply(reply);
    }

    @Override
    public boolean copyConfig(String targetConfiguration, String newConfiguration) throws NetconfException {
        return copyConfig(TargetConfig.valueOf(targetConfiguration), newConfiguration);
    }

    @Override
    public boolean deleteConfig(TargetConfig netconfTargetConfig) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(DELETE_CONFIG_OPEN).append("\n");
        rpc.append(TARGET_OPEN);
        rpc.append("<").append(netconfTargetConfig).append("/>");
        rpc.append(TARGET_CLOSE).append("\n");
        rpc.append(DELETE_CONFIG_CLOSE).append("\n");
        rpc.append(RPC_CLOSE);
        rpc.append(ENDPATTERN);
        log.debug(rpc.toString());
        String reply = sendRequest(rpc.toString());
        return checkReply(reply);
    }

    @Override
    public boolean deleteConfig(String targetConfiguration) throws NetconfException {
        return deleteConfig(TargetConfig.valueOf(targetConfiguration));
    }

    @Override
    public void startSubscription() throws NetconfException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endSubscription() throws NetconfException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean lock(String configType) throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unlock(String configType) throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean lock() throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unlock() throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean close() throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getSessionId() {
        return "mockSessionId";
    }

    @Override
    public String getServerCapabilities() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDeviceCapabilities(List<String> capabilities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        // TODO Auto-generated method stub

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

    private String sendRequest(String request) throws NetconfException {
        log.info("Mocking NETCONF Session send request: \n" + request);

        if (simpleGetConfig.matcher(request).matches()) {
            return SAMPLE_MSEAEVCUNI_REPLY_INIT;

        } else {
            throw new NetconfException("MocknetconfSession. No sendRequest() case for query: " +
                    request);
        }
    }
}
