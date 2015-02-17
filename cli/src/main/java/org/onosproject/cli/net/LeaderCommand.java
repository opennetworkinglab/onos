/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipService;

import java.util.Comparator;
import java.util.Map;

/**
 * Prints the leader for every topic.
 */
@Command(scope = "onos", name = "leaders",
        description = "Finds the leader for particular topic.")
public class LeaderCommand extends AbstractShellCommand {

    private static final String FMT = "%-20s: %15s %15s";

    @Override
    protected void execute() {
        LeadershipService leaderService = get(LeadershipService.class);
        Map<String, Leadership> leaderBoard = leaderService.getLeaderBoard();
        print(FMT, "Topic", "Leader", "Epoch");

        Comparator<Leadership> leadershipComparator =
                (e1, e2) -> {
                    if (e1.leader() == null && e2.leader() == null) {
                        return 0;
                    }
                    if (e1.leader() == null) {
                        return 1;
                    }
                    if (e2.leader() == null) {
                        return -1;
                    }
                    return e1.leader().toString().compareTo(e2.leader().toString());
                };

        leaderBoard.values()
                .stream()
                .sorted(leadershipComparator)
                .forEach(l -> print(FMT, l.topic(), l.leader(), l.epoch()));
    }

}
