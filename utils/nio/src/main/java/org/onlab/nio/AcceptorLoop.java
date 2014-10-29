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
package org.onlab.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Selector loop derivative tailored to acceptConnection inbound connections.
 */
public abstract class AcceptorLoop extends SelectorLoop {

    private SocketAddress listenAddress;
    private ServerSocketChannel socketChannel;

    /**
     * Creates an acceptor loop with the specified selection timeout and
     * accepting connections on the the given address.
     *
     * @param selectTimeout selection timeout; specified in millis
     * @param listenAddress socket address where to listen for connections
     * @throws IOException if the backing selector cannot be opened
     */
    public AcceptorLoop(long selectTimeout, SocketAddress listenAddress)
            throws IOException {
        super(selectTimeout);
        this.listenAddress = checkNotNull(listenAddress, "Address cannot be null");
    }

    /**
     * Hook to accept an inbound connection on the specified socket channel.
     *
     * @param channel socketChannel where an accept operation awaits
     * @throws IOException if the accept operation cannot be processed
     */
    protected abstract void acceptConnection(ServerSocketChannel channel) throws IOException;

    /**
     * Opens a new server socket channel configured in non-blocking mode and
     * bound to the loop's listen address.
     *
     * @throws IOException if unable to open or configure the socket channel
     */
    protected synchronized void openChannel() throws IOException {
        socketChannel = ServerSocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        socketChannel.bind(listenAddress);
    }

    /**
     * Closes the server socket channel.
     *
     * @throws IOException if unable to close the socketChannel
     */
    protected synchronized void closechannel() throws IOException {
        if (socketChannel != null) {
            socketChannel.close();
            socketChannel = null;
        }
    }

    @Override
    public void shutdown() {
        try {
            closechannel();
        } catch (IOException e) {
            log.warn("Unable to close the socketChannel", e);
        }
        super.shutdown();
    }

    @Override
    protected void loop() throws IOException {
        openChannel();
        notifyReady();

        // Keep looping until told otherwise.
        while (isRunning()) {
            // Attempt a selection; if no operations selected or if signalled
            // to shutdown, spin through.
            int count = selector.select(selectTimeout);
            if (count == 0 || !isRunning()) {
                continue;
            }

            // Iterate over all keys selected for an operation and process them.
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                // Fetch the key and remove it from the pending list.
                SelectionKey key = keys.next();
                keys.remove();

                // If the key has a pending acceptConnection operation, process it.
                if (key.isAcceptable()) {
                    acceptConnection((ServerSocketChannel) key.channel());
                }
            }
        }
    }

}

