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

package org.onosproject.cli;

import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;
import java.util.SortedSet;

/**
 * A completer that can be used as a placeholder for arguments that don't
 * need/want completers.
 */
public class PlaceholderCompleter extends AbstractCompleter {

    @Override
    public int complete(String s, int i, List<String> list) {
        // Populate a string completer with what the user has typed so far
        StringsCompleter delegate = new StringsCompleter();
        SortedSet<String> strings = delegate.getStrings();
        if (s != null) {
            strings.add(s);
        }
        return delegate.complete(s, i, list);
    }
}
