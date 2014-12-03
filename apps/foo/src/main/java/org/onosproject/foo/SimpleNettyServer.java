/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.foo;

import org.onlab.netty.NettyMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to measure Messaging performance.
 */
    public final class SimpleNettyServer {
        private static Logger log = LoggerFactory.getLogger(SimpleNettyServer.class);

            private SimpleNettyServer() {}

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String... args) throws Exception {
                startStandalone(args);
                System.exit(0);
            }

    /**
     * Start standalone server.
     *
     * @param args the args
     * @throws Exception the exception
     */
    public static void startStandalone(String[] args) throws Exception {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
            NettyMessagingService server = new NettyMessagingService(port);
            server.activate();
            server.registerHandler("simple", new NettyNothingHandler());
            server.registerHandler("echo", new NettyEchoHandler());
            log.info("Netty Server server on port " + port);
        }
    }

