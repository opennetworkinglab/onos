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

package org.onosproject.buckdaemon;

import com.google.common.io.ByteStreams;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.onosproject.checkstyle.CheckstyleRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardOpenOption.*;

/**
 * Buck daemon process.
 */
public final class BuckDaemon {

    private static long POLLING_INTERVAL = 1000; //ms

    private final Map<String, BuckTask> tasks = new HashMap<>();
    private final String portLock;
    private final String buckPid;

    // Public construction forbidden
    private BuckDaemon(String[] args) {
        portLock = args[0];
        buckPid = args[1];
    }

    /**
     * Main entry point for the daemon.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args)
            throws CheckstyleException, IOException {
        BuckDaemon daemon = new BuckDaemon(args);
        daemon.registerTasks();
        daemon.startServer();
    }

    /**
     * Registers re-entrant tasks by their task name.
     */
    private void registerTasks() {
        tasks.put("checkstyle", new CheckstyleRunner(System.getProperty("checkstyle.config"),
                                                     System.getProperty("checkstyle.suppressions")));
        // tasks.put("swagger", new SwaggerGenerator());
    }

    /**
     * Monitors another PID and exit when that process exits.
     */
    private void watchProcess(String pid) {
        if (pid == null || pid.equals("0")) {
            return;
        }
        Timer timer = new Timer(true); // start as a daemon, so we don't hang shutdown
        timer.scheduleAtFixedRate(new TimerTask() {
            private String cmd = "kill -s 0 " + pid;

            @Override
            public void run() {
                try {
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    if (p.exitValue() != 0) {
                        System.err.println("shutting down...");
                        System.exit(0);
                    }
                } catch (IOException | InterruptedException e) {
                    //no-op
                    e.printStackTrace();
                }
            }
        }, POLLING_INTERVAL, POLLING_INTERVAL);
    }

    /**
     * Initiates a server.
     */
    private void startServer() throws IOException, CheckstyleException {
        // Use a file lock to ensure only one copy of the daemon runs
        Path portLockPath = Paths.get(portLock);
        FileChannel channel = FileChannel.open(portLockPath, WRITE, CREATE);
        FileLock lock = channel.tryLock();
        if (lock == null) {
            System.out.println("Server is already running");
            System.exit(1);
        } //else, hold the lock until the JVM exits

        // Start the server and bind it to a random port
        ServerSocket server = new ServerSocket(0);

        // Monitor the parent buck process
        watchProcess(buckPid);

        // Set up hook to clean up after ourselves
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                channel.truncate(0);
                channel.close();
                System.err.println("tear down...");
                Files.delete(portLockPath);
            } catch (IOException e) {
                //no-op: shutting down
                e.printStackTrace();
            }
        }));

        // Write the bound port to the port file
        int port = server.getLocalPort();
        channel.truncate(0);
        channel.write(ByteBuffer.wrap(Integer.toString(port).getBytes()));

        // Instantiate a Checkstyle runner and executor; serve until exit...
        ExecutorService executor = Executors.newCachedThreadPool();
        while (true) {
            try {
                executor.submit(new BuckTaskRunner(server.accept()));
            } catch (Exception e) {
                e.printStackTrace();
                //no-op
            }
        }
    }

    /**
     * Runnable capable of invoking the appropriate Buck task with input
     * consumed form the specified socket and output produced back to that
     * socket.
     */
    private class BuckTaskRunner implements Runnable {

        private final Socket socket;

        public BuckTaskRunner(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BuckTaskContext context = new BuckTaskContext(socket.getInputStream());
                String taskName = context.taskName();
                if (!taskName.isEmpty()) {
                    BuckTask task = tasks.get(taskName);
                    if (task != null) {
                        System.out.println(String.format("Executing task '%s'", taskName));
                        try {
                            task.execute(context);
                            for (String line : context.output()) {
                                output(socket, line);
                            }
                        // TODO should we catch Exception, RuntimeException, or something specific?
                        } catch (Throwable e) {
                            e.printStackTrace(new PrintStream(socket.getOutputStream()));
                        }
                    } else {
                        String message = String.format("No task named '%s'", taskName);
                        System.out.print(message);
                        output(socket, message);
                    }
                }
                socket.getOutputStream().flush();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void output(Socket socket, String line) throws IOException {
            socket.getOutputStream().write((line + "\n").getBytes());
        }
    }
}
