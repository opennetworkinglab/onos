/*
 * Copyright 2015 Open Networking Laboratory
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

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.google.common.base.Preconditions;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of a NETCONF session to talk to a device.
 */
public class NetconfSessionImpl implements NetconfSession {

    public static final Logger log = LoggerFactory
            .getLogger(NetconfSessionImpl.class);
    private static final int CONNECTION_TIMEOUT = 0;


    private Connection netconfConnection;
    private NetconfDeviceInfo deviceInfo;
    private Session sshSession;
    private boolean connectionActive;
    private BufferedReader bufferReader = null;
    private PrintWriter out = null;
    private int messageID = 0;
    //TODO inject these capabilites from yang model provided by app
    private List<String> deviceCapabilities =
            Collections.singletonList("urn:ietf:params:netconf:base:1.0");
    private String serverCapabilities;
    private String endpattern = "]]>]]>";


    public NetconfSessionImpl(NetconfDeviceInfo deviceInfo) throws IOException {
        this.deviceInfo = deviceInfo;
        connectionActive = false;
        startConnection();
    }


    private void startConnection() throws IOException {
        if (!connectionActive) {
            netconfConnection = new Connection(deviceInfo.ip().toString(), deviceInfo.port());
            netconfConnection.connect(null, CONNECTION_TIMEOUT, 0);
            boolean isAuthenticated;
            try {
                if (deviceInfo.getKeyFile() != null) {
                    isAuthenticated = netconfConnection.authenticateWithPublicKey(
                            deviceInfo.name(), deviceInfo.getKeyFile(),
                            deviceInfo.password());
                } else {
                    log.info("authenticate with username {} and password {}",
                             deviceInfo.name(), deviceInfo.password());
                    isAuthenticated = netconfConnection.authenticateWithPassword(
                            deviceInfo.name(), deviceInfo.password());
                }
            } catch (IOException e) {
                throw new IOException("Authentication connection failed:" +
                                              e.getMessage());
            }

            connectionActive = true;
            Preconditions.checkArgument(isAuthenticated,
                                        "Authentication password and username failed");
            startSshSession();
        }
    }

    private void startSshSession() throws IOException {
        try {
            sshSession = netconfConnection.openSession();
            sshSession.startSubSystem("netconf");
            bufferReader = new BufferedReader(new InputStreamReader(new StreamGobbler(
                    sshSession.getStdout())));
            out = new PrintWriter(sshSession.getStdin());
            sendHello();
        } catch (IOException e) {
            throw new IOException("Failed to create ch.ethz.ssh2.Session session:" +
                                          e.getMessage());
        }
    }

    private void sendHello() throws IOException {
        serverCapabilities = doRequest(createHelloString());
    }

    private String createHelloString() {
        StringBuilder hellobuffer = new StringBuilder();
        hellobuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        hellobuffer.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        hellobuffer.append("  <capabilities>\n");
        deviceCapabilities.forEach(
                cap -> hellobuffer.append("    <capability>" + cap + "</capability>\n"));
        hellobuffer.append("  </capabilities>\n");
        hellobuffer.append("</hello>\n");
        hellobuffer.append(endpattern);
        return hellobuffer.toString();

    }

    @Override
    public String doRPC(String request) throws IOException {
        String reply = doRequest(request);
        return checkReply(reply) ? reply : "ERROR " + reply;
    }

    private String doRequest(String request) throws IOException {
        //log.info("sshState " + sshSession.getState() + "request" + request);
        checkAndRestablishSession();
        //log.info("sshState after" + sshSession.getState());
        out.print(request);
        out.flush();
        messageID++;
        return readOne();
    }

    private void checkAndRestablishSession() throws IOException {
        if (sshSession.getState() != 2) {
            try {
                startSshSession();
            } catch (IOException e) {
                log.info("the connection had to be reopened");
                try {
                    startConnection();
                } catch (IOException e2) {
                    log.error("No connection {} for device, exception {}", netconfConnection, e2);
                    throw new IOException(e.getMessage());
                    //TODO remove device from ONOS
                }
            }
        }
    }

    @Override
    public String get(String request) throws IOException {
        return doRPC(request);
    }

    @Override
    public String getConfig(String targetConfiguration) throws IOException {
        return getConfig(targetConfiguration, null);
    }

    @Override
    public String getConfig(String targetConfiguration, String configurationSchema) throws IOException {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        rpc.append("<rpc message-id=\"" + messageID + "\"  "
                           + "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<get-config>\n");
        rpc.append("<source>\n");
        rpc.append("<" + targetConfiguration + "/>");
        rpc.append("</source>");
        if (configurationSchema != null) {
            rpc.append("<filter type=\"subtree\">\n");
            rpc.append(configurationSchema + "\n");
            rpc.append("</filter>\n");
        }
        rpc.append("</get-config>\n");
        rpc.append("</rpc>\n");
        rpc.append(endpattern);
        String reply = doRequest(rpc.toString());
        return checkReply(reply) ? reply : "ERROR " + reply;
    }

    @Override
    public boolean editConfig(String newConfiguration) throws IOException {
        newConfiguration = newConfiguration + endpattern;
        return checkReply(doRequest(newConfiguration));
    }

    @Override
    public boolean copyConfig(String targetConfiguration, String newConfiguration)
            throws IOException {
        newConfiguration = newConfiguration.trim();
        if (!newConfiguration.startsWith("<configuration>")) {
            newConfiguration = "<configuration>" + newConfiguration
                    + "</configuration>";
        }
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" " +
                                                      "encoding=\"UTF-8\"?>");
        rpc.append("<rpc>");
        rpc.append("<copy-config>");
        rpc.append("<target>");
        rpc.append("<" + targetConfiguration + "/>");
        rpc.append("</target>");
        rpc.append("<source>");
        rpc.append("<" + newConfiguration + "/>");
        rpc.append("</source>");
        rpc.append("</copy-config>");
        rpc.append("</rpc>");
        rpc.append(endpattern);
        return checkReply(doRequest(rpc.toString()));
    }

    @Override
    public boolean deleteConfig(String targetConfiguration) throws IOException {
        if (targetConfiguration.equals("running")) {
            log.warn("Target configuration for delete operation can't be \"running\"",
                     targetConfiguration);
            return false;
        }
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" " +
                                                      "encoding=\"UTF-8\"?>");
        rpc.append("<rpc>");
        rpc.append("<delete-config>");
        rpc.append("<target>");
        rpc.append("<" + targetConfiguration + "/>");
        rpc.append("</target>");
        rpc.append("</delete-config>");
        rpc.append("</rpc>");
        rpc.append(endpattern);
        return checkReply(doRequest(rpc.toString()));
    }

    @Override
    public boolean lock() throws IOException {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" " +
                                                      "encoding=\"UTF-8\"?>");
        rpc.append("<rpc>");
        rpc.append("<lock>");
        rpc.append("<target>");
        rpc.append("<candidate/>");
        rpc.append("</target>");
        rpc.append("</lock>");
        rpc.append("</rpc>");
        rpc.append(endpattern);
        return checkReply(doRequest(rpc.toString()));
    }

    @Override
    public boolean unlock() throws IOException {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" " +
                                                      "encoding=\"UTF-8\"?>");
        rpc.append("<rpc>");
        rpc.append("<unlock>");
        rpc.append("<target>");
        rpc.append("<candidate/>");
        rpc.append("</target>");
        rpc.append("</unlock>");
        rpc.append("</rpc>");
        rpc.append(endpattern);
        return checkReply(doRequest(rpc.toString()));
    }

    @Override
    public boolean close() throws IOException {
        return close(false);
    }

    private boolean close(boolean force) throws IOException {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc>");
        if (force) {
            rpc.append("<kill-configuration/>");
        } else {
            rpc.append("<close-configuration/>");
        }
        rpc.append("<close-configuration/>");
        rpc.append("</rpc>");
        rpc.append(endpattern);
        return checkReply(doRequest(rpc.toString())) || close(true);
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

    private boolean checkReply(String reply) {
        if (reply != null) {
            if (!reply.contains("<rpc-error>")) {
                return true;
            } else if (reply.contains("<ok/>")
                    || (reply.contains("<rpc-error>")
                    && reply.contains("warning"))) {
                return true;
            }
        }
        return false;
    }

    private String readOne() throws IOException {
        //TODO try a simple string
        final StringWriter reply = new StringWriter();
        while (true) {
            int charRead = bufferReader.read();
            if (charRead == -1) {
                throw new IOException("Session closed");
            }

            for (int i = 0; i < endpattern.length(); i++) {
                if (charRead == endpattern.charAt(i)) {
                    if (i < endpattern.length() - 1) {
                        charRead = bufferReader.read();
                    } else {
                        return reply.getBuffer().toString();
                    }
                } else {
                    String s = endpattern.substring(0, i);
                    for (int j = 0; i < s.length(); j++) {
                        reply.write(s.charAt(j));
                    }
                    reply.write(charRead);
                    break;
                }
            }
        }
    }

}
