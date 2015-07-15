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
package org.onosproject.provider.netconf.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.delay;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;

import com.tailf.jnc.Capabilities;
import com.tailf.jnc.JNCException;
import com.tailf.jnc.SSHConnection;
import com.tailf.jnc.SSHSession;

/**
 * This is a logical representation of actual NETCONF device, carrying all the
 * necessary information to connect and execute NETCONF operations.
 */
public class NetconfDevice {
    private final Logger log = getLogger(NetconfDevice.class);

    /**
     * The Device State is used to determine whether the device is active or
     * inactive. This state infomation will help Device Creator to add or delete
     * the device from the core.
     */
    public static enum DeviceState {
        /* Used to specify Active state of the device */
        ACTIVE,
        /* Used to specify inactive state of the device */
        INACTIVE,
        /* Used to specify invalid state of the device */
        INVALID
    }

    private static final int DEFAULT_SSH_PORT = 22;
    private static final int DEFAULT_CON_TIMEOUT = 0;
    private static final String XML_CAPABILITY_KEY = "capability";
    private static final int EVENTINTERVAL = 2000;
    private static final int CONNECTION_CHECK_INTERVAL = 3;
    private static final String INPUT_HELLO_XML_MSG = new StringBuilder(
                                                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">")
            .append("<capabilities><capability>urn:ietf:params:netconf:base:1.0</capability>")
            .append("</capabilities></hello>").toString();

    private String sshHost;
    private int sshPort = DEFAULT_SSH_PORT;
    private int connectTimeout = DEFAULT_CON_TIMEOUT;
    private String username;
    private String password;
    private boolean reachable = false;

    private List<String> capabilities = new ArrayList<String>();
    private SSHConnection sshConnection = null;

    private DeviceState deviceState = DeviceState.INVALID;

    protected NetconfDevice(String sshHost, int sshPort, String username,
                            String password) {
        this.username = checkNotNull(username,
                                     "Netconf Username Cannot be null");
        this.sshHost = checkNotNull(sshHost, "Netconf Device IP cannot be null");
        this.sshPort = checkNotNull(sshPort,
                                    "Netconf Device SSH port cannot be null");
        this.password = password;
    }

    /**
     * This will try to connect to NETCONF device and find all the capabilities.
     *
     * @throws Exception if unable to connect to the device
     */
    // FIXME: this should not be a generic Exception; perhaps wrap in some RuntimeException
    public void init() throws Exception {
        try {
            if (sshConnection == null) {
                sshConnection = new SSHConnection(sshHost, sshPort, connectTimeout);
                sshConnection.authenticateWithPassword(username, password);
            }
            // Send hello message to retrieve capabilities.
        } catch (IOException e) {
            log.error("Fatal Error while creating connection to the device: "
                    + deviceInfo(), e);
            throw e;
        } catch (JNCException e) {
            log.error("Failed to connect to the device: " + deviceInfo(), e);
            throw e;
        }

        hello();
    }

    private void hello() {
        SSHSession ssh = null;
        try {
            ssh = new SSHSession(sshConnection);
            String helloRequestXML = INPUT_HELLO_XML_MSG.trim();

            log.debug("++++++++++++++++++++++++++++++++++Sending Hello: "
                    + sshConnection.getGanymedConnection().getHostname()
                    + "++++++++++++++++++++++++++++++++++");
            printPrettyXML(helloRequestXML);
            ssh.print(helloRequestXML);
            // ssh.print(endCharSeq);
            ssh.flush();
            String xmlResponse = null;
            int i = CONNECTION_CHECK_INTERVAL;
            while (!ssh.ready() && i > 0) {
                delay(EVENTINTERVAL);
                i--;
            }

            if (ssh.ready()) {
                StringBuffer readOne = ssh.readOne();
                if (readOne == null) {
                    log.error("The Hello Contains No Capabilites");
                    throw new JNCException(
                                           JNCException.SESSION_ERROR,
                                           "server does not support NETCONF base capability: "
                                                   + Capabilities.NETCONF_BASE_CAPABILITY);
                } else {
                    xmlResponse = readOne.toString().trim();

                    log.debug("++++++++++++++++++++++++++++++++++Reading Capabilities: "
                            + sshConnection.getGanymedConnection()
                                    .getHostname()
                            + "++++++++++++++++++++++++++++++++++");

                    printPrettyXML(xmlResponse);
                    processCapabilities(xmlResponse);
                }
            }
            reachable = true;
        } catch (IOException e) {
            log.error("Fatal Error while sending Hello Message to the device: "
                    + deviceInfo(), e);
        } catch (JNCException e) {
            log.error("Fatal Error while sending Hello Message to the device: "
                    + deviceInfo(), e);
        } finally {
            log.debug("Closing the session after successful execution");
            if (ssh != null) {
                ssh.close();
            }
        }
    }

    private void processCapabilities(String xmlResponse) throws JNCException {
        if (xmlResponse.isEmpty()) {
            log.error("The capability response cannot be empty");
            throw new JNCException(
                                   JNCException.SESSION_ERROR,
                                   "server does not support NETCONF base capability: "
                                           + Capabilities.NETCONF_BASE_CAPABILITY);
        }
        try {
            Document doc = new SAXBuilder()
                    .build(new StringReader(xmlResponse));
            Element rootElement = doc.getRootElement();
            processCapabilities(rootElement);
        } catch (Exception e) {
            log.error("ERROR while parsing the XML " + xmlResponse);
        }
    }

    private void processCapabilities(Element rootElement) {
        List<Element> children = rootElement.getChildren();
        if (children.isEmpty()) {
            return;
        }
        for (Element child : children) {

            if (child.getName().equals(XML_CAPABILITY_KEY)) {
                capabilities.add(child.getValue());
            }
            if (!child.getChildren().isEmpty()) {
                processCapabilities(child);
            }
        }
    }

    private void printPrettyXML(String xmlstring) {
        try {
            Document doc = new SAXBuilder().build(new StringReader(xmlstring));
            XMLOutputter xmOut = new XMLOutputter(Format.getPrettyFormat());
            String outputString = xmOut.outputString(doc);
            log.debug(outputString);
        } catch (Exception e) {
            log.error("ERROR while parsing the XML " + xmlstring, e);

        }
    }

    /**
     * This would return host IP and host Port, used by this particular Netconf
     * Device.
     * @return Device Information.
     */
    public String deviceInfo() {
        return new StringBuilder("host: ").append(sshHost).append(". port: ")
                .append(sshPort).toString();
    }

    /**
     * This will terminate the device connection.
     */
    public void disconnect() {
        sshConnection.close();
        reachable = false;
    }

    /**
     * This will list down all the capabilities supported on the device.
     * @return Capability list.
     */
    public List<String> getCapabilities() {
        return capabilities;
    }

    /**
     * This api is intended to know whether the device is connected or not.
     * @return true if connected
     */
    public boolean isReachable() {
        return reachable;
    }

    /**
     * This will return the IP used connect ssh on the device.
     * @return Netconf Device IP
     */
    public String getSshHost() {
        return sshHost;
    }

    /**
     * This will return the SSH Port used connect the device.
     * @return SSH Port number
     */
    public int getSshPort() {
        return sshPort;
    }

    /**
     * The usename used to connect Netconf Device.
     * @return Device Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Retrieve current state of the device.
     * @return Current Device State
     */
    public DeviceState getDeviceState() {
        return deviceState;
    }

    /**
     * This is set the state information for the device.
     * @param deviceState Next Device State
     */
    public void setDeviceState(DeviceState deviceState) {
        this.deviceState = deviceState;
    }

    /**
     * Check whether the device is in Active state.
     * @return true if the device is Active
     */
    public boolean isActive() {
        return deviceState == DeviceState.ACTIVE ? true : false;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
