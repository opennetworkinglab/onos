/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.netconf;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.common.annotations.Beta;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Abstract netconf session implements methods common
 * for different implementations of netconf session.
 */
public abstract class AbstractNetconfSession implements NetconfSession {

    private static final Logger log = getLogger(AbstractNetconfSession.class);

    private static final String ENDPATTERN = "]]>]]>";
    private static final String MESSAGE_ID_STRING = "message-id";
    private static final String NEW_LINE = "\n";
    private static final String EQUAL = "=";
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
    // FIXME hard coded namespace nc
    private static final String CONFIG_OPEN = "<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String CONFIG_CLOSE = "</config>";
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String NETCONF_BASE_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"";
    private static final String NETCONF_WITH_DEFAULTS_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-with-defaults\"";

    @Override
    public abstract CompletableFuture<String> request(String request) throws NetconfException;

    @Override
    public abstract CompletableFuture<String> rpc(String request) throws NetconfException;

    @Override
    public CompletableFuture<CharSequence> asyncGetConfig(DatastoreId datastore) throws NetconfException {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<get-config>\n");
        rpc.append("<source>\n");
        rpc.append('<').append(checkNotNull(datastore)).append("/>");
        rpc.append("</source>");
        // filter here
        rpc.append("</get-config>\n");
        rpc.append("</rpc>");

        return rpc(rpc.toString())
                .thenApply(msg -> {
                    // crude way of removing rpc-reply envelope
                    int begin = msg.indexOf("<data>");
                    int end = msg.lastIndexOf("</data>");
                    if (begin != -1 && end != -1) {
                        return msg.subSequence(begin, end + "</data>".length());
                    } else {
                        // FIXME probably should exceptionally fail here.
                        return msg;
                    }
                });
    }

    @Override
    public CompletableFuture<CharSequence> asyncGet() throws NetconfException {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<get>\n");
        // filter here
        rpc.append("</get>\n");
        rpc.append("</rpc>");
        return rpc(rpc.toString())
                .thenApply(msg -> {
                    // crude way of removing rpc-reply envelope
                    int begin = msg.indexOf("<data>");
                    int end = msg.lastIndexOf("</data>");
                    if (begin != -1 && end != -1) {
                        return msg.subSequence(begin, end + "</data>".length());
                    } else {
                        // FIXME probably should exceptionally fail here.
                        return msg;
                    }
                });

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
        //Assign a random integer here, it will be replaced in formatting
        rpc.append(1);
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
        return requestSync(rpc.toString());
    }

    @Override
    public String doWrappedRpc(String request) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        //Assign a random integer here, it will be replaced in formatting
        rpc.append(1);
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(request);
        rpc.append(RPC_CLOSE).append(NEW_LINE);
        rpc.append(ENDPATTERN);
        return requestSync(rpc.toString());
    }

    @Override
    public abstract String requestSync(String request) throws NetconfException;

    @Override
    public String getConfig(DatastoreId netconfTargetConfig) throws NetconfException {
        return getConfig(netconfTargetConfig, null);
    }

    @Override
    public String getConfig(DatastoreId netconfTargetConfig, String configurationFilterSchema) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc ");
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        //Assign a random integer here, it will be replaced in formatting
        rpc.append(1);
        rpc.append("\"  ");
        rpc.append("xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<get-config>\n");
        rpc.append("<source>\n");
        rpc.append("<").append(netconfTargetConfig).append("/>");
        rpc.append("</source>");
        if (configurationFilterSchema != null) {
            rpc.append("<filter type=\"subtree\">\n");
            rpc.append(configurationFilterSchema).append("\n");
            rpc.append("</filter>\n");
        }
        rpc.append("</get-config>\n");
        rpc.append("</rpc>\n");
        rpc.append(ENDPATTERN);
        String reply = requestSync(rpc.toString());
        return checkReply(reply) ? reply : "ERROR " + reply;
    }

    @Override
    public boolean editConfig(String newConfiguration) throws NetconfException {
        if (!newConfiguration.endsWith(ENDPATTERN)) {
            newConfiguration = newConfiguration + ENDPATTERN;
        }
        return checkReply(requestSync(newConfiguration));
    }

    @Override
    public boolean editConfig(DatastoreId netconfTargetConfig,
                              String mode,
                              String newConfiguration) throws NetconfException {
        newConfiguration = newConfiguration.trim();
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        //Assign a random integer here, it will be replaced in formatting
        rpc.append(1);
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
        String reply = requestSync(rpc.toString());
        return checkReply(reply);
    }

    @Override
    public boolean copyConfig(DatastoreId destination, DatastoreId source) throws NetconfException {
        return checkReply(requestSync(bareCopyConfig(destination.asXml(), source.asXml())));
    }

    @Override
    public boolean copyConfig(DatastoreId netconfTargetConfig, String newConfiguration) throws NetconfException {
        return checkReply(requestSync(bareCopyConfig(netconfTargetConfig.asXml(),
                                                     normalizeCopyConfigParam(newConfiguration))));
    }

    @Override
    public boolean copyConfig(String netconfTargetConfig, String newConfiguration) throws NetconfException {
        return checkReply(requestSync(bareCopyConfig(normalizeCopyConfigParam(netconfTargetConfig),
                                                     normalizeCopyConfigParam(newConfiguration))));
    }

    @Override
    public boolean deleteConfig(DatastoreId netconfTargetConfig) throws NetconfException {
        if (netconfTargetConfig.equals(DatastoreId.RUNNING)) {
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
        return checkReply(requestSync(rpc.toString()));
    }

    @Override
    public void startSubscription() throws NetconfException {
        startSubscription(null);
    }

    @Override
    public abstract void startSubscription(String filterSchema) throws NetconfException;

    @Override
    public abstract void endSubscription() throws NetconfException;

    @Override
    public boolean lock(DatastoreId datastore) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<lock>");
        rpc.append("<target>");
        rpc.append("<");
        rpc.append(datastore.id());
        rpc.append("/>");
        rpc.append("</target>");
        rpc.append("</lock>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        String lockReply = requestSync(rpc.toString());
        return checkReply(lockReply);
    }

    @Override
    public boolean unlock(DatastoreId datastore) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<unlock>");
        rpc.append("<target>");
        rpc.append("<");
        rpc.append(datastore.id());
        rpc.append("/>");
        rpc.append("</target>");
        rpc.append("</unlock>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        String unlockReply = requestSync(rpc.toString());
        return checkReply(unlockReply);
    }

    @Override
    public boolean close() throws NetconfException {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<close-session/>");
        rpc.append("</rpc>");
        rpc.append(ENDPATTERN);
        boolean closed = checkReply(requestSync(rpc.toString()));
        if (closed) {
            return closed;
        } else {
            //forcefully kill session if not closed
            rpc = new StringBuilder();
            rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
            rpc.append("<kill-session/>");
            rpc.append("</rpc>");
            rpc.append(ENDPATTERN);
            return checkReply(requestSync(rpc.toString()));
        }
    }

    @Override
    public abstract String getSessionId();

    @Override
    public abstract Set<String> getDeviceCapabilitiesSet();

    @Override
    public abstract void addDeviceOutputListener(NetconfDeviceOutputEventListener listener);

    @Override
    public abstract void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener);

    /**
     * Checks errors in reply from the session.
     * @param reply reply string
     * @return true if no error, else false
     */
    @Beta
    protected boolean checkReply(String reply) {
        if (reply != null) {
            if (!reply.contains("<rpc-error>")) {
                return true;
            } else if (reply.contains("<ok/>")
                    || (reply.contains("<rpc-error>")
                    && reply.contains("warning"))) {
                // FIXME rpc-error with a warning is considered same as Ok??
                return true;
            }
        }
        return false;
    }

    private String bareCopyConfig(CharSequence target,
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
        return rpc.toString();
    }

    /**
     * Normalize String parameter passed to copy-config API.
     * <p>
     * Provided for backward compatibility purpose
     *
     * @param input passed to copyConfig API
     * @return XML likely to be suitable for copy-config source or target
     */
    private CharSequence normalizeCopyConfigParam(String input) {
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
}
