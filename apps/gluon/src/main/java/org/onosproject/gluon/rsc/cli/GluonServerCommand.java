/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.gluon.rsc.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.gluon.rsc.GluonServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.onosproject.gluon.manager.GluonManager.createServer;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_DEFAULT_PORT;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_HTTP;
import static org.onosproject.gluon.rsc.GluonConstants.INVALID_MODE;
import static org.onosproject.gluon.rsc.GluonConstants.INVALID_RANGE;
import static org.onosproject.gluon.rsc.GluonConstants.KEY_TYPE;
import static org.onosproject.gluon.rsc.GluonConstants.MODE_START;
import static org.onosproject.gluon.rsc.GluonConstants.MODE_STOP;
import static org.onosproject.gluon.rsc.GluonConstants.NO_SERVER_AVAIL;
import static org.onosproject.gluon.rsc.GluonConstants.NO_SERVER_AVAIL_ON_PORT;
import static org.onosproject.gluon.rsc.GluonConstants.PROTON_KEY_SUPPORT;
import static org.onosproject.gluon.rsc.GluonConstants.WRONG_INPUT;
import static org.onosproject.gluon.rsc.GluonConstants.WRONG_INPUT_TYPE;
import static org.onosproject.gluon.rsc.GluonConstants.WRONG_IP_FORMAT;


/**
 * To monitor Gluon etcd server.
 */
@Service
@Command(scope = "onos", name = "gluon",
        description = "Support for reading Gluon data via etcd client")
public class GluonServerCommand extends AbstractShellCommand {

    @Option(name = "-m", aliases = "--mode",
            description = "Gluon server monitoring mode: start; stop",
            required = false, multiValued = false)
    String mode = MODE_START;

    @Option(name = "-i", aliases = "--server-ip",
            description = "Gluon server ip address",
            required = true, multiValued = false)
    String ipAddress = null;

    @Option(name = "-p", aliases = "--port", description = "Gluon server port",
            required = false, multiValued = false)
    String port = GLUON_DEFAULT_PORT;

    @Option(name = "-k", aliases = "--key",
            description = "Proton key : net-l3vpn",
            required = false, multiValued = false)
    String protonKey = KEY_TYPE;

    public String version = null;

    @Override
    protected void doExecute() {
        try {
            if (ipAddress != null && isValidIP(ipAddress) && isValidPort(port)
                    && isValidMode(mode) && isValidProtonKey(protonKey)
                    && isSeverReachable()) {
                String url = GLUON_HTTP + ipAddress + ":" + port;
                if (isEtcdSeverAvailable()) {
                    //Gets gluon server running version
                    version = gluonServerVersion();
                    createServer(url, protonKey, mode, version);
                } else {
                    log.info(NO_SERVER_AVAIL_ON_PORT);
                    return;
                }
            } else {
                log.info(WRONG_INPUT);
            }
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }

    /**
     * Returns boolean if given IP format is valid.
     *
     * @param ipAddr Ip Address
     * @return boolean
     */
    public boolean isValidIP(String ipAddr) {
        boolean isIPaddrValid;
        Pattern pattern = Pattern.compile("^(\\d{1,3})\\" +
                                                  ".(\\d{1,3})\\" +
                                                  ".(\\d{1,3})\\.(\\d{1,3})$");
        Matcher matcher = pattern.matcher(ipAddr);
        if (matcher.find()) {
            isIPaddrValid = true;
        } else {
            print(WRONG_IP_FORMAT);
            isIPaddrValid = false;
        }
        return isIPaddrValid;
    }

    /**
     * Returns boolean if given port value is valid.
     *
     * @param portValue port number
     * @return boolean
     */
    public boolean isValidPort(String portValue) {
        boolean isPortValid = false;
        try {
            Integer portNum = Integer.parseInt(portValue);
            if (portNum >= 0 && portNum <= 65535) {
                isPortValid = true;
            } else {
                print(INVALID_RANGE);
                isPortValid = false;
            }
        } catch (NumberFormatException nfe) {
            print(WRONG_INPUT_TYPE);
        }
        return isPortValid;
    }

    /**
     * Returns boolean if given mode is valid.
     *
     * @param mode server mode
     * @return boolean
     */
    public boolean isValidMode(String mode) {
        boolean isValidMode;
        if (mode.equalsIgnoreCase(MODE_START) ||
                mode.equalsIgnoreCase(MODE_STOP)) {
            isValidMode = true;
        } else {
            print(INVALID_MODE);
            isValidMode = false;
        }
        return isValidMode;
    }

    /**
     * Returns boolean if given mode is valid.
     *
     * @param key key
     * @return boolean
     */
    public boolean isValidProtonKey(String key) {
        boolean isValidProtonKey = true;
        if (!KEY_TYPE.equalsIgnoreCase(key)) {
            print(PROTON_KEY_SUPPORT);
            isValidProtonKey = false;
        }
        return isValidProtonKey;
    }

    /**
     * Returns version of gluon server.
     *
     * @return String
     */

    public String gluonServerVersion() {
        String serverUrl = GLUON_HTTP + this.ipAddress + ":" +
                this.port + "/version";
        GluonServer gluonServer = new GluonServer();
        String gluonversion = gluonServer.getGluonServerVersion(serverUrl);
        String[] versionArray = gluonversion.split("\\.");
        version = versionArray[0];
        return version;
    }

    /**
     * Returns reachability of Gluon server.
     *
     * @return isSeverReachable
     */
    public boolean isSeverReachable() {
        boolean isSeverReachable = false;
        try {
            InetAddress inet = InetAddress.getByName(ipAddress);
            if (inet.isReachable(5000)) {
                isSeverReachable = true;
            } else {
                isSeverReachable = false;
                print(NO_SERVER_AVAIL);
            }
        } catch (IOException e) {
            isSeverReachable = false;
            log.error("Check server process is failed with {} ",
                      e.getMessage());
        }
        return isSeverReachable;
    }

    /**
     * Returns availability of Gluon server.
     *
     * @return isServerAvailable
     */
    public boolean isEtcdSeverAvailable() {
        String serverUrl = GLUON_HTTP + ipAddress + ":" + port;
        boolean isServerAvailable;
        try {
            URL url = new URL(serverUrl);
            URLConnection connection = url.openConnection();
            connection.connect();
            isServerAvailable = true;
        } catch (IOException e) {
            print(NO_SERVER_AVAIL_ON_PORT);
            isServerAvailable = false;
        }
        return isServerAvailable;
    }
}
