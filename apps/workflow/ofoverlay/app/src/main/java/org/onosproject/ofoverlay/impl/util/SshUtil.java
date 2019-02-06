/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.ofoverlay.impl.util;

import com.google.common.io.CharStreams;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.onlab.packet.IpAddress;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.model.accessinfo.SshAccessInfo;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.onosproject.workflow.api.CheckCondition.check;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Class for SSH utilities.
 */
public final class SshUtil {

    protected static final Logger log = getLogger(SshUtil.class);

    private static final String STRICT_HOST_CHECKING = "StrictHostKeyChecking";
    private static final String DEFAULT_STRICT_HOST_CHECKING = "no";
    private static final int DEFAULT_SESSION_TIMEOUT = 30000; // milliseconds

    private static final String SPACESEPERATOR = " ";

    /**
     * Default constructor.
     */
    private SshUtil() {
    }

    /**
     * Creates a new session with a given ssh access information.
     *
     * @param sshInfo information to ssh to the remote server
     * @return ssh session, or null
     */
    public static Session connect(SshAccessInfo sshInfo) {
        Session session;

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(sshInfo.privateKey());

            session = jsch.getSession(sshInfo.user(),
                    sshInfo.remoteIp().toString(),
                    sshInfo.port().toInt());
            session.setConfig(STRICT_HOST_CHECKING, DEFAULT_STRICT_HOST_CHECKING);
            session.connect(DEFAULT_SESSION_TIMEOUT);

        } catch (JSchException e) {
            log.warn("Failed to connect to {}", sshInfo.toString(), e);
            session = authUserPwd(sshInfo);
        }
        return session;
    }

    /**
     * Creates a new session with ssh access info.
     *
     * @param sshInfo information to ssh to the remote server
     * @return ssh session, or null
     */
    public static Session authUserPwd(SshAccessInfo sshInfo) {
        log.info("Retrying Session with {}", sshInfo);
        try {
            JSch jsch = new JSch();

            Session session = jsch.getSession(sshInfo.user(),
                    sshInfo.remoteIp().toString(),
                    sshInfo.port().toInt());
            session.setPassword(sshInfo.password());
            session.setConfig(STRICT_HOST_CHECKING, DEFAULT_STRICT_HOST_CHECKING);
            session.connect(DEFAULT_SESSION_TIMEOUT);

            return session;
        } catch (JSchException e) {
            log.warn("Failed to connect to {} due to {}", sshInfo.toString(), e);
            return null;
        }
    }

    /**
     * Closes a connection.
     *
     * @param session session ssh session
     */
    public static void disconnect(Session session) {
        if (session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * Fetches last term after executing command.
     * @param session  ssh session
     * @param command command to execute
     * @return last term, or null
     */
    public static String fetchLastTerm(Session session, String command) {
         if (session == null || !session.isConnected()) {
             log.error("Invalid session({})", session);
             return null;
         }

         log.info("fetchLastTerm: ssh command {} to {}", command, session.getHost());

         try {
             Channel channel = session.openChannel("exec");
             if (channel == null) {
                 log.error("Invalid channel of session({}) for command({})", session, command);
                 return null;
             }

             ((ChannelExec) channel).setCommand(command);
             channel.setInputStream(null);
             InputStream output = channel.getInputStream();
             channel.connect();
             String[] lineList = null;

             try (BufferedReader reader = new BufferedReader(new InputStreamReader(output, StandardCharsets.UTF_8))) {
                 lineList = reader.lines().findFirst().get().split(SPACESEPERATOR);
             } catch (IOException e) {
                 log.error("Exception in fetchLastTerm", e);
             } finally {
                 channel.disconnect();
                 output.close();
             }

             if (lineList.length > 0) {
                 return lineList[lineList.length - 1];
             } else {
                 return null;
             }

         } catch (JSchException | IOException e) {
             log.error("Exception in fetchLastTerm", e);
             return null;
         }
    }

    /**
     * Executes a given command. It opens exec channel for the command and closes
     * the channel when it's done.
     *
     * @param session ssh connection to a remote server
     * @param command command to execute
     * @return command output string if the command succeeds, or null
     */
    public static String executeCommand(Session session, String command) {
        if (session == null || !session.isConnected()) {
            log.error("Invalid session({})", session);
            return null;
        }

        log.info("executeCommand: ssh command {} to {}", command, session.getHost());

        try {
            Channel channel = session.openChannel("exec");

            if (channel == null) {
                log.debug("Invalid channel of session({}) for command({})", session, command);
                return null;
            }

            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();

            channel.connect();
            String result = CharStreams.toString(new InputStreamReader(output, StandardCharsets.UTF_8));
            log.trace("SSH result(on {}): {}", session.getHost(), result);
            channel.disconnect();

            return result;
        } catch (JSchException | IOException e) {
            log.debug("Failed to execute command {} due to {}", command, e);
            return null;
        }
    }

    /**
     * Fetches OVS version information.
     * @param session Jsch session
     * @return OVS version
     * @throws WorkflowException workflow exception
     */
    public static OvsVersion fetchOvsVersion(Session session) throws WorkflowException {

        OvsVersion devOvsVersion;

        String ovsVersionStr = fetchLastTerm(session, "ovs-vswitchd --version");
        if (ovsVersionStr == null) {
            log.error("Failed to get ovs Version String for ssh session:{}", session);
            throw new WorkflowException("Failed to get ovs Version String");
        }

        devOvsVersion = OvsVersion.build(ovsVersionStr);
        if (devOvsVersion == null) {
            log.error("Failed to build OVS version for {}", ovsVersionStr);
            throw new WorkflowException("Failed to build OVS version");
        }

        return devOvsVersion;
    }

    private static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private static boolean isIPv6(String address) {
        boolean isCorrect = true;
        try {
            IpAddress.valueOf(address);
        } catch (IllegalArgumentException e) {
            log.debug("Exception Occurred {}", e.toString());
            isCorrect = false;
        }
        return isCorrect;
    }

    private static boolean isCidr(String s) {
        String[] splits = s.split("/");
        return splits.length == 2 &&
                (splits[0].matches(IP_PATTERN) || isIPv6(splits[0]));
    }

    /**
     * Adds IP address on the interface.
     * @param session SSH session
     * @param ifname interface name
     * @param address network address
     * @throws WorkflowException workflow exception
     */
    public static void addIpAddrOnInterface(Session session, String ifname, NetworkAddress address)
            throws WorkflowException {

        executeCommand(session, String.format("ip addr add %s dev %s", address.cidr(), ifname));

        Set<NetworkAddress> result = getIpAddrOfInterface(session, ifname);
        if (!result.contains(address)) {
            throw new WorkflowException("Failed to set ip(" + address + ") on " + ifname + ",  result: " + result);
        }
    }

    /**
     * Gets IP addresses of interface.
     * @param session SSH session
     * @param ifname interface name
     * @return IP addresses of interface
     */
    public static Set<NetworkAddress> getIpAddrOfInterface(Session session, String ifname) {

        String output = executeCommand(session, String.format("ip addr show %s", ifname));

        if (output == null) {
            return Collections.emptySet();
        }

        Set<NetworkAddress> result = Pattern.compile(" ")
                .splitAsStream(output)
                .filter(SshUtil::isCidr)
                .map(NetworkAddress::valueOf)
                .collect(Collectors.toSet());
        return result;
    }

    /**
     * Returns whether the interface has IP address.
     * @param session SSH session
     * @param ifname interface name
     * @param addr network address
     * @return whether the interface has IP address
     */
    public static boolean hasIpAddrOnInterface(Session session, String ifname, NetworkAddress addr) {

        Set<NetworkAddress> phyBrIps = getIpAddrOfInterface(session, ifname);

        return phyBrIps.stream()
                .anyMatch(ip -> addr.ip().equals(ip.ip()));
    }

    /**
     * Sets IP link UP on the interface.
     * @param session SSH session
     * @param ifname interface name
     * @throws WorkflowException workflow exception
     */
    public static void setIpLinkUpOnInterface(Session session, String ifname)
            throws WorkflowException {

        executeCommand(session, String.format("ip link set %s up", ifname));

        if (!isIpLinkUpOnInterface(session, ifname)) {
            throw new WorkflowException("Failed to set UP on " + ifname);
        }
    }

    /**
     * Returns whether the link of the interface is up.
     * @param session SSH session
     * @param ifname interface name
     * @return whether the link of the interface is up
     */
    public static boolean isIpLinkUpOnInterface(Session session, String ifname) {
        String output = executeCommand(session, String.format("ip link show %s", ifname));

        return output != null && output.contains("UP");
    }

    /**
     * Executes SSH behavior.
     * @param sshAccessInfo SSH Access information
     * @param behavior SSH behavior
     * @param <R> Return type of SSH behavior
     * @return return of SSH behavior
     * @throws WorkflowException workflow exception
     */
    public static <R> R exec(SshAccessInfo sshAccessInfo, SshBehavior<R> behavior)
            throws WorkflowException {

        check(sshAccessInfo != null, "Invalid sshAccessInfo");
        Session session = connect(sshAccessInfo);
        if (session == null || !session.isConnected()) {
            log.error("Failed to get session for ssh:{}", sshAccessInfo);
            throw new WorkflowException("Failed to get session for ssh:" + sshAccessInfo);
        }

        try {
            return behavior.apply(session);
        } finally {
            disconnect(session);
        }
    }

}
