/*
 * Copyright 2022-present Open Networking Foundation
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
package org.onosproject.workflow.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.onosproject.workflow.api.WorkflowLogStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component(immediate = true)
@Service
public class SimpleWorkflowLogStore implements WorkflowLogStore {
    private static final int LOG_EXPIRATION_TIME = 60;
    private static final String SQ_OPBRACKET = "[";
    private static final String SQ_CLBRACKET = "]";
    private static final String SPACE = " ";

    private Cache<String, List<String>> workflowLogMap;

    @Activate
    public void activate() {
        workflowLogMap = CacheBuilder.newBuilder()
                .expireAfterWrite(LOG_EXPIRATION_TIME, TimeUnit.MINUTES)
                .build();
    }

    @Deactivate
    public void deactivate() {
        workflowLogMap.invalidateAll();
    }

    @Override
    public void addLog(String uuid, String logMsg, String className, String level) {
        workflowLogMap.asMap().putIfAbsent(uuid, new ArrayList<>());
        Objects.requireNonNull(workflowLogMap.getIfPresent(uuid)).add(0, formatLog(logMsg, level, className));
    }

    @Override
    public void addException(String uuid, String logMsg, String className, String level, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String msg = logMsg + sw;
        addLog(uuid, msg, className, level);
    }

    @Override
    public List<String> getLog(String uuid) {
        List<String> logs = workflowLogMap.getIfPresent(uuid);
        if (Objects.isNull(logs)) {
            return new ArrayList<>();
        }
        return logs;
    }

    @Override
    public Map<String, List<String>> asJavaMap() {
        return workflowLogMap.asMap();
    }

    private String formatLog(String msg, String level, String className) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        msg = SQ_OPBRACKET + timestamp + SQ_CLBRACKET + SPACE + SQ_OPBRACKET + level + SQ_CLBRACKET + SPACE +
                SQ_OPBRACKET + className + SQ_CLBRACKET + SPACE + msg;
        return msg;
    }
}
