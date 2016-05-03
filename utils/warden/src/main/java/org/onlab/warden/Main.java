/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onlab.warden;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.log.Logger;

/**
 * Main program for executing scenario test warden.
 */
public final class Main {

    // Public construction forbidden
    private Main(String[] args) {
    }

    /**
     * Main entry point for the cell warden.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Main main = new Main(args);
        main.run();
    }

    // Runs the warden processing
    private void run() {
        startWebServer();
    }

    // Initiates a web-server.
    private static void startWebServer() {
        WardenServlet.warden = new Warden();
        org.eclipse.jetty.util.log.Log.setLog(new NullLogger());
        Server server = new Server(4321);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(WardenServlet.class, "/*");
        try {
            server.start();
        } catch (Exception e) {
            print("Warden already active...");
        }
    }

    private static void print(String s) {
        System.out.println(s);
    }

    // Logger to quiet Jetty down
    private static class NullLogger implements Logger {
        @Override
        public String getName() {
            return "quiet";
        }

        @Override
        public void warn(String msg, Object... args) {
        }

        @Override
        public void warn(Throwable thrown) {
        }

        @Override
        public void warn(String msg, Throwable thrown) {
        }

        @Override
        public void info(String msg, Object... args) {
        }

        @Override
        public void info(Throwable thrown) {
        }

        @Override
        public void info(String msg, Throwable thrown) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void setDebugEnabled(boolean enabled) {
        }

        @Override
        public void debug(String msg, Object... args) {
        }

        @Override
        public void debug(Throwable thrown) {
        }

        @Override
        public void debug(String msg, Throwable thrown) {
        }

        @Override
        public Logger getLogger(String name) {
            return this;
        }

        @Override
        public void ignore(Throwable ignored) {
        }
    }

}
