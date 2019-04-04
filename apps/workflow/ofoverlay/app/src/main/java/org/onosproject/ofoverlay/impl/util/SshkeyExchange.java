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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.onlab.packet.IpAddress;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Class for SSH key exchange.
 */
public class SshkeyExchange {

    protected static final Logger log = getLogger(SshkeyExchange.class);
    private static final String HOME_ENV = System.getProperty("user.home");
    private static final String USER_ENV = System.getProperty("user.name");
    private static final String PUBLIC_KEY = "/.ssh/id_rsa.pub";
    private static final String PRIVATE_KEY = "/.ssh/id_rsa";
    private static final String SFTP_CHANNEL = "sftp";
    private static final String SSH_HOME = "/.ssh/";
    private static final String SSH_AUTH_KEY = "/.ssh/authorized_keys";
    private static final int SFTPPORT = 22;
    private static final int DIR_PER = 448;
    private static final int FILE_PER = 384;
    private static final int KEY_SIZE = 2048;
    private static final int DEFAULT_SESSION_TIMEOUT = 30000; // milliseconds

    private Session getJschSession(String user, String host, String password) {
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        Session session;
        try {
            session = new JSch().getSession(user, host, SFTPPORT);
            session.setPassword(password);
            session.setConfig(config);
        } catch (JSchException e) {
            log.error("Exception in getJschSession", e);
            return null;
        }

        return session;
    }

    private boolean generateKeyPair() {
        KeyPair kpair;
        StringBuilder command = new StringBuilder()
                .append("chmod 600 ")
                .append(HOME_ENV)
                .append(PRIVATE_KEY);
        try {
            kpair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, KEY_SIZE);
            kpair.writePrivateKey(HOME_ENV + PRIVATE_KEY);
            kpair.writePublicKey(HOME_ENV + PUBLIC_KEY, USER_ENV);
            Runtime.getRuntime().exec(command.toString());
            kpair.dispose();
        } catch (JSchException | IOException e) {
            log.error("Exception in generateKeyPair", e);
            return false;
        }
        return true;
    }

    /**
     * Exchanges SSH key.
     * @param host SSH server host
     * @param user user
     * @param password password
     * @return SSH key exchange success or not
     */
    public boolean sshAutoKeyExchange(IpAddress host, String user, String password) {

        Session session = getJschSession(user, host.toString(), password);
        boolean returnFlag;
        File publickeyPath = new File(HOME_ENV + PUBLIC_KEY);
        if (session == null) {
            log.error("Error While establishing SFTP connection with {}", host.toString());
            return false;
        }
        Channel channel;
        String remoteHome;
        ChannelSftp sftp;
        FileInputStream fis = null;
        SftpATTRS attrs = null;
        try {
            session.connect(DEFAULT_SESSION_TIMEOUT);
            channel = session.openChannel(SFTP_CHANNEL);
            if (channel == null) {
                log.error("SFTP channel open failed for {}", host.toString());
                return false;
            }
            channel.connect();
            sftp = (ChannelSftp) channel;
            remoteHome = sftp.getHome();
            // checking key pair existance

            if (!publickeyPath.exists()) {
                File dirs = new File(HOME_ENV + SSH_HOME);
                if (!dirs.exists() && !dirs.mkdirs()) {
                    log.error("{} not exists and unable to create ", dirs.getPath());
                    return false;
                } else if (!generateKeyPair()) {
                    log.error("SSH Key pair generation failed");
                    return false;
                }
            }

            // checking for authenticate_keys file existance
            fis = new FileInputStream(publickeyPath);
            try {
                sftp.lstat(remoteHome + SSH_HOME);
            } catch (SftpException e) {
                sftp.mkdir(remoteHome + SSH_HOME);
                sftp.chmod(700, remoteHome + SSH_HOME);
            }
            try {
                attrs = sftp.lstat(remoteHome + SSH_AUTH_KEY);
            } catch (SftpException e) {
                log.info("authorized_keys file does not exist at remote device ,"
                        + "a new file will be created");
            }

            if (attrs != null) {
                sftp.get(remoteHome + SSH_AUTH_KEY, HOME_ENV + "/tempauthorized_keys");

                String pubKey;
                try (Stream<String> st = Files.lines(Paths.get(HOME_ENV + PUBLIC_KEY))) {
                    pubKey = st.collect(Collectors.joining());
                }

                String authKey;
                try (Stream<String> st = Files.lines(Paths.get(HOME_ENV + "/tempauthorized_keys"))) {
                    authKey = st.collect(Collectors.joining());
                }

                if (authKey.contains(pubKey)) {
                    log.info("Skipping key append to server as Key is already added");
                } else {
                    sftp.put(fis, remoteHome + SSH_AUTH_KEY, ChannelSftp.APPEND);
                    log.info("Public key appended to server");
                }
            } else {

                sftp.put(fis, remoteHome + SSH_AUTH_KEY, ChannelSftp.APPEND);
                // Give proper permission to file and directory.
                sftp.chmod(DIR_PER, remoteHome + SSH_HOME);
                sftp.chmod(FILE_PER, remoteHome + SSH_AUTH_KEY);
                log.info("Public key appended to server");
            }

            sftp.exit();
            session.disconnect();
            returnFlag = true;
        } catch (JSchException | SftpException | IOException e) {
            log.error("Exception occured because of {} ", e);
            returnFlag = false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.info("Error closing public key file");
                }
            }
        }
        return returnFlag;

    }
}
