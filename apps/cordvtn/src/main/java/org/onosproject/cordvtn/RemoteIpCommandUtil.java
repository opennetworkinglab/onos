/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.onlab.packet.IpAddress;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@code RemoteIpCommandUtil} provides methods to help execute Linux IP commands to a remote server.
 * It opens individual exec channels for each command. User can create a session with {@code connect}
 * method and then execute a series commands. After done with all commands, the session must be closed
 * explicitly by calling {@code disconnect}.
 */
public final class RemoteIpCommandUtil {

    protected static final Logger log = getLogger(RemoteIpCommandUtil.class);

    private static final String STRICT_HOST_CHECKING = "StrictHostKeyChecking";
    private static final String DEFAULT_STRICT_HOST_CHECKING = "no";
    private static final int DEFAULT_SESSION_TIMEOUT = 60000; // milliseconds

    private static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private static final String IP_ADDR_SHOW = "sudo ip addr show %s";
    private static final String IP_ADDR_FLUSH = "sudo ip addr flush %s";
    private static final String IP_ADDR_ADD = "sudo ip addr add %s dev %s";
    private static final String IP_ADDR_DELETE = "sudo ip addr delete %s dev %s";
    private static final String IP_LINK_SHOW = "sudo ip link show %s";
    private static final String IP_LINK_UP = "sudo ip link set %s up";

    /**
     * Default constructor.
     */
    private RemoteIpCommandUtil() {
    }

    /**
     * Adds a given IP address to a given device.
     *
     * @param session ssh connection
     * @param ip network address
     * @param device device name to assign the ip address
     * @return true if the command succeeds, or false
     */
    public static boolean addIp(Session session, NetworkAddress ip, String device) {
        if (session == null || !session.isConnected()) {
            return false;
        }

        executeCommand(session, String.format(IP_ADDR_ADD, ip.cidr(), device));
        Set<IpAddress> result = getCurrentIps(session, device);
        return result.contains(ip.ip());
    }

    /**
     * Removes the IP address from a given device.
     *
     * @param session ssh connection
     * @param ip ip address
     * @param device device name
     * @return true if the command succeeds, or false
     */
    public static boolean deleteIp(Session session, IpAddress ip, String device) {
        if (session == null || !session.isConnected()) {
            return false;
        }

        executeCommand(session, String.format(IP_ADDR_DELETE, ip, device));
        Set<IpAddress> result = getCurrentIps(session, device);
        return !result.contains(ip);
    }

    /**
     * Removes all IP address on a given device.
     *
     * @param session ssh connection
     * @param device device name
     * @return true if the command succeeds, or false
     */
    public static boolean flushIp(Session session, String device) {
        if (session == null || !session.isConnected()) {
            return false;
        }

        executeCommand(session, String.format(IP_ADDR_FLUSH, device));
        return getCurrentIps(session, device).isEmpty();
    }

    /**
     * Returns a set of IP address that a given device has.
     *
     * @param session ssh connection
     * @param device device name
     * @return set of IP prefix or empty set
     */
    public static Set<IpAddress> getCurrentIps(Session session, String device) {
        if (session == null || !session.isConnected()) {
            return Sets.newHashSet();
        }

        String output = executeCommand(session, String.format(IP_ADDR_SHOW, device));
        Set<IpAddress> result = Pattern.compile(" |/")
                .splitAsStream(output)
                .filter(s -> s.matches(IP_PATTERN))
                .map(IpAddress::valueOf)
                .collect(Collectors.toSet());

        return result;
    }

    /**
     * Sets link state up for a given device.
     *
     * @param session ssh connection
     * @param device device name
     * @return true if the command succeeds, or false
     */
    public static boolean setInterfaceUp(Session session, String device) {
        if (session == null || !session.isConnected()) {
            return false;
        }

        executeCommand(session, String.format(IP_LINK_UP, device));
        return isInterfaceUp(session, device);
    }

    /**
     * Checks if a given interface is up or not.
     *
     * @param session ssh connection
     * @param device device name
     * @return true if the interface is up, or false
     */
    public static boolean isInterfaceUp(Session session, String device) {
        if (session == null || !session.isConnected()) {
            return false;
        }

        String output = executeCommand(session, String.format(IP_LINK_SHOW, device));
        return output != null && output.contains("UP");
    }

    /**
     * Creates a new session with a given access information.
     *
     * @param sshInfo information to ssh to the remove server
     * @return ssh session, or null
     */
    public static Session connect(SshAccessInfo sshInfo) {
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(sshInfo.privateKey());

            Session session = jsch.getSession(sshInfo.user(),
                                      sshInfo.remoteIp().toString(),
                                      sshInfo.port().toInt());
            session.setConfig(STRICT_HOST_CHECKING, DEFAULT_STRICT_HOST_CHECKING);
            session.connect(DEFAULT_SESSION_TIMEOUT);

            return session;
        } catch (JSchException e) {
            log.debug("Failed to connect to {} due to {}", sshInfo.toString(), e.toString());
            return null;
        }
    }

    /**
     * Closes a connection.
     *
     * @param session session
     */
    public static void disconnect(Session session) {
        if (session.isConnected()) {
            session.disconnect();
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
    private static String executeCommand(Session session, String command) {
        if (session == null || !session.isConnected()) {
            return null;
        }

        log.trace("Execute command {} to {}", command, session.getHost());

        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();

            channel.connect();
            String result = CharStreams.toString(new InputStreamReader(output));
            channel.disconnect();

            return result;
        } catch (JSchException | IOException e) {
            log.debug("Failed to execute command {} due to {}", command, e.toString());
            return null;
        }
    }
}
