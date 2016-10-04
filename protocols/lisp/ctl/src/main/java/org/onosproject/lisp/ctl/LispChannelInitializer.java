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
package org.onosproject.lisp.ctl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a ChannelInitializer for a server-side LISP channel.
 */
public final class LispChannelInitializer extends ChannelInitializer<Channel> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private IdleStateHandler idleHandler;
    private ReadTimeoutHandler readTimeoutHandler;

    private static final int READER_IDLE_TIME_SECOND = 20;
    private static final int WRITER_IDLE_TIME_SECOND = 25;
    private static final int ALL_IDLE_TIME_SECOND = 0;
    private static final int READ_TIMEOUT_SECOND = 30;

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        LispChannelHandler handler = new LispChannelHandler();

        idleHandler = new IdleStateHandler(READER_IDLE_TIME_SECOND,
                WRITER_IDLE_TIME_SECOND, ALL_IDLE_TIME_SECOND);
        readTimeoutHandler = new ReadTimeoutHandler(READ_TIMEOUT_SECOND);

        pipeline.addLast("lispmessagedecoder", new LispMessageDecoder());
        pipeline.addLast("lispmessageencoder", new LispMessageEncoder());
        pipeline.addLast("idle", idleHandler);
        pipeline.addLast("readTimeout", readTimeoutHandler);
        pipeline.addLast("handler", handler);
    }
}
