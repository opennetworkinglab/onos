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
package org.onosproject.artemis.impl.bgpspeakers;

import org.apache.commons.net.telnet.TelnetClient;
import org.onosproject.routing.bgp.BgpInfoService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Quagga interface to connect and announce prefixes.
 */
public class QuaggaBgpSpeakers extends BgpSpeakers {
    private static final String PASSWORD = "sdnip";

    private TelnetClient telnet = new TelnetClient();
    private InputStream in;
    private PrintStream out;

    public QuaggaBgpSpeakers(BgpInfoService bgpInfoService) {
        super(bgpInfoService);
    }

    @Override
    public void announceSubPrefixes(String[] prefixes) {
        bgpSessions.forEach((session) -> {
            String peerIp = session.remoteInfo().ip4Address().toString(),
                    localAs = String.valueOf(session.remoteInfo().as4Number());
            assert peerIp != null;

            try {
                telnet.connect(peerIp, 2605);
                in = telnet.getInputStream();
                out = new PrintStream(telnet.getOutputStream());

                readUntil("Password: ");
                write(PASSWORD);
                readUntil("> ");

                // we user remote AS as local because he is iBGP neighbor.
                announcePrefix(prefixes, localAs);

                disconnect();

                log.info("Announced " + prefixes[0] + " and " + prefixes[1] + " at " + peerIp);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        });
    }

    /**
     * Read telnet buffer until a pattern is met.
     *
     * @param pattern string pattern to match in terminal
     * @return matched string
     */
    private String readUntil(String pattern) {
        try {
            char lastChar = pattern.charAt(pattern.length() - 1);
            StringBuffer sb = new StringBuffer();
            char ch = (char) in.read();
            while (true) {
                sb.append(ch);
                if (ch == lastChar) {
                    if (sb.toString().endsWith(pattern)) {
                        return sb.toString();
                    }
                }
                ch = (char) in.read();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return null;
    }

    /**
     * Write to the telnet client.
     *
     * @param value string to write
     */
    private void write(String value) {
        try {
            out.println(value);
            out.flush();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    /**
     * Configure terminal and announce prefix inside the Quagga router.
     *
     * @param prefixes prefixes to announce
     * @param localAs ASN of BGP Speaker
     */
    private void announcePrefix(String[] prefixes, String localAs) {
        write("en");
        readUntil("# ");
        write("configure terminal");
        readUntil("(config)# ");
        write("router bgp " + localAs);
        readUntil("(config-router)# ");
        Arrays.stream(prefixes).forEach((prefix) -> {
            write("network " + prefix);
            readUntil("(config-router)# ");
        });
        write("end");
    }

    /**
     * Disconnect from the telnet session.
     */
    private void disconnect() {
        try {
            telnet.disconnect();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
