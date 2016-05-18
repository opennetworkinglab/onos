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

import com.google.common.io.ByteStreams;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CheckstyleRunner {

    private final Configuration config;

    public CheckstyleRunner(String configLocation, String suppressionLocation)
            throws CheckstyleException {
        // create a configuration
        DefaultConfiguration config = (DefaultConfiguration) ConfigurationLoader.loadConfiguration(
                configLocation, new PropertiesExpander(System.getProperties()));

        // add the suppression file to the configuration
        DefaultConfiguration suppressions = new DefaultConfiguration("SuppressionFilter");
        suppressions.addAttribute("file", suppressionLocation);
        config.addChild(suppressions);

        this.config = config;
    }

    public Runnable checkClass(Socket socket) {
        return () -> {
            try {
                String input = new String(ByteStreams.toByteArray(socket.getInputStream()));
                String output = checkClass(input);
                socket.getOutputStream().write(output.getBytes());
                socket.getOutputStream().flush();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CheckstyleException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    public String checkClass(String input) throws CheckstyleException, InterruptedException {
        String[] split = input.split("\n", 2);
        if (split.length < 2 || split[1].length() == 0) {
            return "";
        }
        String base = split[0];
        String files = split[1];

        // create a listener for output
        StringAuditor listener = new StringAuditor();
        listener.setBase(base);

        // create Checker object and run it
        final Checker checker = new Checker();
        final ClassLoader moduleClassLoader = Checker.class.getClassLoader();
        checker.setModuleClassLoader(moduleClassLoader);

        try {

            checker.configure(config);
            checker.addListener(listener);

            // run Checker
            List<File> fileList = Stream.of(files.split("\n"))
                                        .map(File::new)
                                        .collect(Collectors.toList());
            int errorCounter = checker.process(fileList);
            if (errorCounter > 0) {
                listener.append("CHECKSTYLE ERROR\n");
            }
        } finally {
            checker.destroy();
        }

        return listener.getAudit();
    }
}

class StringAuditor implements AuditListener {

    private CountDownLatch finishedLatch = new CountDownLatch(1);
    private StringBuilder output = new StringBuilder();
    private String base = "";

    public void setBase(String base) {
        this.base = base;
    }

    public void append(String s) {
        output.append(s);
    }

    public String getAudit() throws InterruptedException {
        finishedLatch.await();
        return output.toString();
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
                String fileName = evt.getFileName();
                int index = fileName.indexOf(base);
                if (index >= 0) {
                    fileName = fileName.substring(index + base.length() + 1);
                }
                output.append(fileName).append(':').append(evt.getLine());
                if (evt.getColumn() > 0) {
                    output.append(':').append(evt.getColumn());
                }
                output.append(": ").append(evt.getMessage());
                output.append('\n');
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
        output.append(throwable.getMessage());
    }
}