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
package org.onosproject.provider.netconf.flow.impl;

import static org.onlab.util.Tools.delay;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

import com.tailf.jnc.Capabilities;
import com.tailf.jnc.JNCException;
import com.tailf.jnc.SSHConnection;
import com.tailf.jnc.SSHSession;

/**
 * This is to carry necessary information to connect and execute NETCONF
 * operations.
 */
public class NetconfOperation {
    private final Logger log = getLogger(NetconfOperation.class);
    private static final int EVENTINTERVAL = 2000;
    private static final int CONNECTION_CHECK_INTERVAL = 3;
    private static final String INPUT_HELLO_XML_MSG = new StringBuilder(
                                                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">")
            .append("<capabilities><capability>urn:ietf:params:netconf:base:1.0</capability>")
            .append("</capabilities></hello>").toString();

    /**
     * This will send a Xml message to the device.
     * @param xmlMsg XML to send
     * @param username user name
     * @param password pass word
     * @param deviceIp ip address of the device
     * @param devicePort port on the device
     */
    protected void sendXmlMessage(String xmlMsg, String username,
                                  String password, String deviceIp,
                                  Integer devicePort) {
        SSHSession ssh = null;
        try {
            SSHConnection sshConnection = getConnection(username, password,
                                                        deviceIp, devicePort);
            ssh = new SSHSession(sshConnection);
            executeMessage(ssh, INPUT_HELLO_XML_MSG);
            /*
             * execute acl message
             */
            executeMessage(ssh, xmlMsg);

        } catch (IOException e) {
            log.error("Unable to send Hello Message to the device: ", e);
        } catch (JNCException e) {
            log.error("Authentication fail while sending Hello Message to the device: ",
                      e);
        } catch (Exception e) {
            log.error("Unable to send Hello Message to the device: ", e);
        } finally {
            log.debug("Closing the session after successful execution");
            ssh.close();
        }
    }

    private void executeMessage(SSHSession ssh, String xmlMsg)
            throws IOException, JNCException {
        String helloRequestXML = xmlMsg.trim();

        log.debug("Sending Hello");
        ssh.print(helloRequestXML);
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

                log.debug("Reading Capabilities: "
                        + ssh.getSSHConnection().getGanymedConnection()
                                .getHostname());
            }
        }
    }

    /**
     * To establish SSH Connection.
     *
     * @param username user name
     * @param password pass word
     * @param sshHost host
     * @param sshPort port
     * @return new SSH connection
     * @throws IOException if connection fails
     * @throws JNCException if connection causes an error
     */
    public SSHConnection getConnection(String username, String password,
                                       String sshHost, Integer sshPort)
            throws IOException, JNCException {
        SSHConnection sshConnection;
        try {
            sshConnection = new SSHConnection(sshHost, sshPort);
            sshConnection.authenticateWithPassword(username, password);
        } catch (IOException e) {
            log.error("Unable to create a connection to the device: ");
            throw e;
        } catch (JNCException e) {
            log.error("Failed to connect to the device: ");
            throw e;
        }
        return sshConnection;
    }

}
