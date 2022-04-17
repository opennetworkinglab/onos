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
package org.onosproject.workflow.api;

import org.onlab.osgi.DefaultServiceDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class WorkflowLoggerFactory {
    private static final String INFO = "INFO";
    private static final String ERROR = "ERROR";
    private static final String WARN = "WARN";

    protected Logger log;
    protected String contextName;
    protected String className;
    protected WorkflowLogStore logStore = DefaultServiceDirectory.getService(WorkflowLogStore.class);

    public WorkflowLoggerFactory(String ctxtName, String cls) {
        log = LoggerFactory.getLogger(cls);
        contextName = ctxtName;
        className = cls;
    }

    public void debug(String msg) {
        log.debug(msg);
    }

    public void debug(String format, Object arg) {
        log.debug(format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        log.debug(format, arg1, arg2);
    }

    public void debug(String format, Object... arg) {
        log.debug(format, arg);
    }

    public void debug(String format, Throwable e) {
        log.debug(format, e);
    }

    public void info(String msg) {
        logStore.addLog(contextName, msg, className, INFO);
        log.info(msg);
    }

    public void info(String format, Object arg) {
        String msg = MessageFormatter.format(format, arg).getMessage();
        logStore.addLog(contextName, msg, className, INFO);
        log.info(format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        String msg = MessageFormatter.format(format, arg1, arg2).getMessage();
        logStore.addLog(contextName, msg, className, INFO);
        log.info(format, arg1, arg2);
    }

    public void info(String format, Object... arg) {
        String msg = MessageFormatter.arrayFormat(format, arg).getMessage();
        logStore.addLog(contextName, msg, className, INFO);
        log.info(format, arg);
    }

    public void info(String format, Throwable e) {
        logStore.addException(contextName, format, className, INFO, e);
        log.info(format, e);
    }

    public void error(String msg) {
        logStore.addLog(contextName, msg, className, ERROR);
        log.error(msg);
    }

    public void error(String format, Object arg) {
        String msg = MessageFormatter.format(format, arg).getMessage();
        logStore.addLog(contextName, msg, className, ERROR);
        log.error(format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        String msg = MessageFormatter.format(format, arg1, arg2).getMessage();
        logStore.addLog(contextName, msg, className, ERROR);
        log.error(format, arg1, arg2);
    }

    public void error(String format, Object... arg) {
        String msg = MessageFormatter.arrayFormat(format, arg).getMessage();
        logStore.addLog(contextName, msg, className, ERROR);
        log.error(format, arg);
    }

    public void error(String format, Throwable e) {
        logStore.addException(contextName, format, className, ERROR, e);
        log.error(format, e);
    }

    public void warn(String msg) {
        logStore.addLog(contextName, msg, className, WARN);
        log.warn(msg);
    }

    public void warn(String format, Object arg) {
        String msg = MessageFormatter.format(format, arg).getMessage();
        logStore.addLog(contextName, msg, className, WARN);
        log.warn(format, arg);
    }

    public void warn(String format, Object arg1, Object arg2) {
        String msg = MessageFormatter.format(format, arg1, arg2).getMessage();
        logStore.addLog(contextName, msg, className, WARN);
        log.warn(format, arg1, arg2);
    }

    public void warn(String format, Object... arg) {
        String msg = MessageFormatter.arrayFormat(format, arg).getMessage();
        logStore.addLog(contextName, msg, className, WARN);
        log.warn(format, arg);
    }

    public void warn(String format, Throwable e) {
        logStore.addException(contextName, format, className, WARN, e);
        log.warn(format, e);
    }
}
