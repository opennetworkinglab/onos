/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.cli.cfg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.SubjectFactory;

/**
 * Network configuration subject key completer.
 *
 * Assumes argument right before the one being completed is SubjectClassKey.
 */
public class SubjectKeyCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        NetworkConfigRegistry service = AbstractShellCommand.get(NetworkConfigRegistry.class);
        String subjectClassKey = commandLine.getArguments()[commandLine.getCursorArgumentIndex() - 1];

        SubjectFactory subjectFactory = service.getSubjectFactory(subjectClassKey);
        if (subjectFactory == null) {
            return Collections.emptyList();
        }
        // get all registered subject objects.
        Set<Object> subjects = service.getSubjects(subjectFactory.subjectClass());
        return subjects.stream().map(subjectFactory::subjectKey).collect(Collectors.toList());
    }

}
