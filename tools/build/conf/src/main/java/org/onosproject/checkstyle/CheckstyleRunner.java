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
package org.onosproject.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.onosproject.buckdaemon.BuckTask;
import org.onosproject.buckdaemon.BuckTaskContext;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Buck task for executing checkstyle on the specified project files.
 */
public class CheckstyleRunner implements BuckTask {

    private final Configuration config;

    public CheckstyleRunner(String configLocation, String suppressionLocation) {
        try {
            // create a configuration
            DefaultConfiguration config = (DefaultConfiguration) ConfigurationLoader
                    .loadConfiguration(configLocation, new PropertiesExpander(System.getProperties()));

            // add the suppression file to the configuration
            DefaultConfiguration suppressions = new DefaultConfiguration("SuppressionFilter");
            suppressions.addAttribute("file", suppressionLocation);
            config.addChild(suppressions);

            this.config = config;
        } catch (CheckstyleException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(BuckTaskContext context) {
        List<String> input = context.input();
        if (input.size() < 3 || input.get(2).length() == 0) {
            return;
        }
        String project = input.get(0);
        String baseDir = input.get(1);

        // create a listener for output
        StringAuditor listener = new StringAuditor(context);
        listener.setProjectName(project);
        listener.setBaseDir(baseDir);

        // create Checker object and run it
        final Checker checker = new Checker();
        final ClassLoader moduleClassLoader = Checker.class.getClassLoader();
        checker.setModuleClassLoader(moduleClassLoader);

        try {
            checker.configure(config);
            checker.addListener(listener);

            // run Checker
            List<File> fileList = input.subList(2, input.size()).stream()
                    .filter(s -> !s.contains("/:"))  // Yes, fighting a hack with a hack.
                    .map(File::new)
                    .collect(Collectors.toList());
            int errorCounter = checker.process(fileList);
            if (errorCounter > 0) {
                context.output("CHECKSTYLE ERROR");
            }

            listener.await();
        } catch (CheckstyleException | InterruptedException e) {
            e.printStackTrace(); //dump exeception to stderr
            throw new RuntimeException(e);
        } finally {
            checker.destroy();
        }

    }

    static class StringAuditor implements AuditListener {

        private final BuckTaskContext context;
        private CountDownLatch finishedLatch = new CountDownLatch(1);
        private String baseDir = "";
        private String project = "";

        StringAuditor(BuckTaskContext context) {
            this.context = context;
        }

        public void setBaseDir(String base) {
            this.baseDir = base;
        }

        public void setProjectName(String projectName) {
            this.project = projectName;
        }

        public void await() throws InterruptedException {
            finishedLatch.await();
        }

        @Override
        public void auditStarted(AuditEvent evt) {
        }

        @Override
        public void auditFinished(AuditEvent evt) {
            finishedLatch.countDown();
        }

        @Override
        public void fileStarted(AuditEvent evt) {
        }

        @Override
        public void fileFinished(AuditEvent evt) {
        }

        @Override
        public void addError(AuditEvent evt) {
            switch (evt.getSeverityLevel()) {
                case ERROR:
                    StringBuilder output = new StringBuilder();
                    String fileName = evt.getFileName();
                    if (!isNullOrEmpty(baseDir)) {
                        int index = fileName.indexOf(baseDir);
                        if (index >= 0) {
                            fileName = fileName.substring(index + baseDir.length() + 1);
                            if (!isNullOrEmpty(project)) {
                                output.append(project).append(':');
                            }
                        }
                    }
                    output.append(fileName).append(':').append(evt.getLine());
                    if (evt.getColumn() > 0) {
                        output.append(':').append(evt.getColumn());
                    }
                    output.append(": ").append(evt.getMessage());
                    context.output(output.toString());
                    break;
                case IGNORE:
                case INFO:
                case WARNING:
                default:
                    break;
            }
        }

        @Override
        public void addException(AuditEvent evt, Throwable throwable) {
            addError(evt);
            context.output(throwable.getMessage());
        }
    }

}
