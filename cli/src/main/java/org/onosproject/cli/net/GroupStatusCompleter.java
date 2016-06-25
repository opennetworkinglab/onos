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
package org.onosproject.cli.net;

import com.google.common.collect.Lists;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.net.group.Group;

import java.util.List;

/**
 * Group status completer.
 */
public class GroupStatusCompleter extends AbstractChoicesCompleter {
    @Override
    protected List<String> choices() {
        List<String> strings = Lists.newArrayList();
        for (Group.GroupState groupState : Group.GroupState.values()) {
            strings.add(groupState.toString().toLowerCase());
        }
        strings.add(GroupsListCommand.ANY);
        return strings;
    }
}
