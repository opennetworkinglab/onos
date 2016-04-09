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
 * Abstraction of a completer with preset choices.
 */
public abstract class AbstractChoicesCompleter extends AbstractCompleter {

    protected abstract List<String> choices();

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        SortedSet<String> strings = delegate.getStrings();
        choices().forEach(strings::add);
        return delegate.complete(buffer, cursor, candidates);
    }

}
