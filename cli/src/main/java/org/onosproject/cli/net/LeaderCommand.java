/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Prints the leader for every topic.
 */
@Command(scope = "onos", name = "leaders",
        description = "Finds the leader for particular topic.")
public class LeaderCommand extends AbstractShellCommand {

    private static final String FMT = "%-30s | %-15s | %-5s | %-10s |";
    private static final String FMT_C = "%-30s | %-15s | %-5s | %-10s | %-19s |";
    private boolean allTopics;
    private Pattern pattern;

    @Argument(index = 0, name = "topic", description = "A leadership topic. Can be a regex",
            required = false, multiValued = false)
    String topicPattern = null;

    @Option(name = "-c", aliases = "--candidates",
            description = "List candidate Nodes for each topic's leadership race",
            required = false, multiValued = false)
    private boolean showCandidates = false;

    /**
     * Compares leaders, sorting by toString() output.
     */
    private Comparator<Leadership> leadershipComparator = (l1, l2) ->
        String.valueOf(l1.leaderNodeId()).compareTo(String.valueOf(l2.leaderNodeId()));

    /**
     * Displays text representing the leaders.
     *
     * @param leaderBoard map of leaders
     */
    private void displayLeaders(Map<String, Leadership> leaderBoard) {
        print("------------------------------------------------------------------------");
        print(FMT, "Topic", "Leader", "Term", "Elected");
        print("------------------------------------------------------------------------");

        leaderBoard.values()
                .stream()
                .filter(l -> allTopics || pattern.matcher(l.topic()).matches())
                .filter(l -> l.leader() != null)
                .sorted(leadershipComparator)
                .forEach(l -> print(FMT,
                        l.topic(),
                        l.leaderNodeId(),
                        l.leader().term(),
                        Tools.timeAgo(l.leader().termStartTime())));
        print("------------------------------------------------------------------------");
    }

    private void displayCandidates(Map<String, Leadership> leaderBoard) {
        print("--------------------------------------------------------------------------------------------");
        print(FMT_C, "Topic", "Leader", "Term", "Elected", "Candidates");
        print("--------------------------------------------------------------------------------------------");
         leaderBoard.entrySet()
                .stream()
                .filter(es -> allTopics || pattern.matcher(es.getKey()).matches())
                .sorted((a, b) -> leadershipComparator.compare(a.getValue(), b.getValue()))
                .forEach(es -> {
                        Leadership l = es.getValue();
                        List<NodeId> candidateList = l.candidates();
                        if (candidateList == null || candidateList.isEmpty()) {
                            return;
                        }
                        print(FMT_C,
                            es.getKey(),
                            String.valueOf(l.leaderNodeId()),
                            l.leader().term(),
                            Tools.timeAgo(l.leader().termStartTime()),
                            // formatting hacks to get it into a table
                            candidateList.get(0).toString());
                            candidateList.subList(1, candidateList.size())
                                         .forEach(n -> print(FMT_C, " ", " ", " ", " ", n));
                            print(FMT_C, " ", " ", " ", " ", " ");
                        });
         print("--------------------------------------------------------------------------------------------");
    }

    /**
     * Returns JSON node representing the leaders and candidates.
     *
     * @param leaderBoard map of leaders
     */
    private JsonNode json(Map<String, Leadership> leaderBoard) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        leaderBoard.forEach((topic, leadership) -> {
                    result.add(
                            mapper.createObjectNode()
                                .put("topic", topic)
                                .put("leader", leadership.leaderNodeId() == null ?
                                        "none" : leadership.leaderNodeId().toString())
                                .put("term", leadership.leader() != null ?
                                    leadership.leader().term() : 0)
                                .put("termStartTime", leadership.leader() != null ?
                                    leadership.leader().termStartTime() : 0)
                                .put("candidates", leadership.candidates().toString()));
        });
        return result;
    }

    @Override
    protected void execute() {
        LeadershipService leaderService = get(LeadershipService.class);
        Map<String, Leadership> leaderBoard = leaderService.getLeaderBoard();
        if (topicPattern == null) {
            allTopics = true;
        } else {
            allTopics = false;
            pattern = Pattern.compile(topicPattern);
        }

        if (outputJson()) {
            print("%s", json(leaderBoard));
            return;
        }

        if (showCandidates) {
            displayCandidates(leaderBoard);
        } else {
            displayLeaders(leaderBoard);
        }
    }
}
