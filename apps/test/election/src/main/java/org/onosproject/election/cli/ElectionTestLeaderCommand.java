/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.election.cli;

import org.onosproject.cluster.NodeId;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.LeadershipService;

/**
 * CLI command to get the current leader for the Election test application.
 */
@Command(scope = "onos", name = "election-test-leader",
        description = "Get the current leader for the Election test application")
public class ElectionTestLeaderCommand extends AbstractShellCommand {

    private NodeId leader;
    private static final String ELECTION_APP = "org.onosproject.election";

    @Override
    protected void execute() {
        LeadershipService service = get(LeadershipService.class);

        //print the current leader
        leader = service.getLeader(ELECTION_APP);
        printLeader(leader);
    }

    /**
     * Prints the leader.
     *
     * @param leader the leader to print
     */
    private void printLeader(NodeId leader) {
        if (leader != null) {
            print("The current leader for the Election app is %s.", leader);
        } else {
            print("There is currently no leader elected for the Election app");
        }
    }
}
