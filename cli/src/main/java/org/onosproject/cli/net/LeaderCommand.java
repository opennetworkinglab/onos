/*
 * Copyright 2014-2015 Open Networking Laboratory
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

    private static final String FMT = "%-30s | %-15s | %-6s | %-10s |";
    private static final String FMT_C = "%-30s | %-15s | %-19s |";
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
    private Comparator<Leadership> leadershipComparator =
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

    /**
     * Displays text representing the leaders.
     *
     * @param leaderBoard map of leaders
     */
    private void displayLeaders(Map<String, Leadership> leaderBoard) {
        print("------------------------------------------------------------------------");
        print(FMT, "Topic", "Leader", "Epoch", "Elected");
        print("------------------------------------------------------------------------");

        leaderBoard.values()
                .stream()
                .filter(l -> allTopics || pattern.matcher(l.topic()).matches())
                .sorted(leadershipComparator)
                .forEach(l -> print(FMT,
                        l.topic(),
                        l.leader(),
                        l.epoch(),
                        Tools.timeAgo(l.electedTime())));
        print("------------------------------------------------------------------------");
    }

    private void displayCandidates(Map<String, Leadership> leaderBoard,
            Map<String, List<NodeId>> candidates) {
        print("------------------------------------------------------------------------");
        print(FMT_C, "Topic", "Leader", "Candidates");
        print("------------------------------------------------------------------------");
         candidates
                .entrySet()
                .stream()
                .filter(es -> allTopics || pattern.matcher(es.getKey()).matches())
                .forEach(es -> {
                        List<NodeId> list = es.getValue();
                        if (list == null || list.isEmpty()) {
                            return;
                        }
                        Leadership l = leaderBoard.get(es.getKey());
                        print(FMT_C,
                            es.getKey(),
                            l == null ? "null" : l.leader(),
                            // formatting hacks to get it into a table
                            list.get(0).toString());
                            list.subList(1, list.size()).forEach(n -> print(FMT_C, " ", " ", n));
                            print(FMT_C, " ", " ", " ");
                        });
         print("------------------------------------------------------------------------");
    }

    /**
     * Returns JSON node representing the leaders.
     *
     * @param leaderBoard map of leaders
     */
    private JsonNode json(Map<String, Leadership> leaderBoard) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        leaderBoard.values()
                .stream()
                .sorted(leadershipComparator)
                .forEach(l ->
                        result.add(
                            mapper.createObjectNode()
                                .put("topic", l.topic())
                                .put("leader", l.leader().toString())
                                .put("candidates", l.candidates().toString())
                                .put("epoch", l.epoch())
                                .put("electedTime", Tools.timeAgo(l.electedTime()))));

        return result;
    }

    /**
     * Returns JSON node representing the leaders.
     *
     * @param leaderBoard map of leaders
     */
    private JsonNode json(Map<String, Leadership> leaderBoard,
            Map<String, List<NodeId>> candidateBoard) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        candidateBoard.entrySet()
                .stream()
                .forEach(es -> {
                        Leadership l = leaderBoard.get(es.getKey());
                        result.add(
                            mapper.createObjectNode()
                                .put("topic", es.getKey())
                                .put("leader", l == null ? "none" : l.leader().toString())
                                .put("candidates", es.getValue().toString()));
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

        if (showCandidates) {
            Map<String, List<NodeId>> candidates = leaderService
                    .getCandidates();
            if (outputJson()) {
                print("%s", json(leaderBoard, candidates));
            } else {
                displayCandidates(leaderBoard, candidates);
            }
        } else {
            if (outputJson()) {
                print("%s", json(leaderBoard));
            } else {
                displayLeaders(leaderBoard);
            }
        }
    }
}
