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
package org.onosproject.bgp.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.cli.AbstractShellCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;


@Command(scope = "onos", name = "bgp-exception", description = "Displays Exceptions")
public class BgpExceptions extends AbstractShellCommand {
    public static final String ACTIVESESSION = "activesession";
    public static final String CLOSEDSESSION = "closedsession";
    private static final Logger log = LoggerFactory.getLogger(BgpExceptions.class);
    protected BgpController bgpController;
    @Argument(index = 0, name = "name",
            description = "activesession" + "\n" + "closedsession",
            required = true, multiValued = false)
    String name = null;
    @Argument(index = 1, name = "peerIp",
            description = "peerId",
            required = false, multiValued = false)
    String peerId = null;
    private Set<String> activeSessionExceptionkeySet;
    private Set<String> closedSessionExceptionKeySet;

    @Override
    protected void execute() {
        switch (name) {
            case ACTIVESESSION:
                displayActiveSessionException();
                break;
            case CLOSEDSESSION:
                displayClosedSessionException();
                break;
            default:
                System.out.print("Unknown Command");
                break;
        }
    }

    private void displayActiveSessionException() {
        try {
            this.bgpController = get(BgpController.class);
            Map<String, List<String>> activeSessionExceptionMap = bgpController.activeSessionMap();
            activeSessionExceptionkeySet = activeSessionExceptionMap.keySet();
            if (!activeSessionExceptionkeySet.isEmpty()) {
                if (peerId != null) {
                    if (activeSessionExceptionkeySet.contains(peerId)) {
                        for (String peerIdKey : activeSessionExceptionkeySet) {
                            List activeSessionExceptionlist = activeSessionExceptionMap.get(peerIdKey);
                            System.out.println(activeSessionExceptionlist);
                        }
                    } else {
                        System.out.print("Wrong argument");
                    }
                } else {
                    activeSessionExceptionkeySet = activeSessionExceptionMap.keySet();
                    if (!activeSessionExceptionkeySet.isEmpty()) {
                        for (String peerId : activeSessionExceptionkeySet) {
                            print("PeerId = %s, Exception = %s ", peerId, activeSessionExceptionMap.get(peerId));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP exceptions: {}", e.getMessage());
        }
    }

    private void displayClosedSessionException() {
        try {
            this.bgpController = get(BgpController.class);
            Map<String, List<String>> closedSessionExceptionMap = bgpController.closedSessionMap();
            closedSessionExceptionKeySet = closedSessionExceptionMap.keySet();
            if (!closedSessionExceptionKeySet.isEmpty()) {
                if (peerId != null) {
                    if (closedSessionExceptionKeySet.contains(peerId)) {
                        for (String peerIdKey : closedSessionExceptionKeySet) {
                            List closedSessionExceptionlist = closedSessionExceptionMap.get(peerIdKey);
                            print("Exceptions = %s", closedSessionExceptionlist);
                        }
                    } else {
                        System.out.print("Wrong argument");
                    }
                } else {
                    closedSessionExceptionKeySet = closedSessionExceptionMap.keySet();
                    if (!closedSessionExceptionKeySet.isEmpty()) {
                        for (String peerId : closedSessionExceptionKeySet) {
                            print("PeerId = %s, Exception = %s", peerId, closedSessionExceptionMap.get(peerId));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying resons for session closure: {}", e.getMessage());
        }
    }


}


