/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core.ctl;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.onosproject.xmpp.core.XmppDeviceFactory;
import org.onosproject.xmpp.core.ctl.handlers.XmppChannelHandler;
import org.onosproject.xmpp.core.ctl.handlers.XmlMerger;
import org.onosproject.xmpp.core.ctl.handlers.XmlStreamDecoder;
import org.onosproject.xmpp.core.ctl.handlers.XmppDecoder;
import org.onosproject.xmpp.core.ctl.handlers.XmppEncoder;


/**
 * Creates pipeline for server-side XMPP channel.
 */
public class XmppChannelInitializer extends ChannelInitializer<SocketChannel> {

    protected XmppDeviceFactory deviceFactory;

    public XmppChannelInitializer(XmppDeviceFactory deviceFactory) {
        this.deviceFactory = deviceFactory;
    }

    /**
     * Initializes pipeline for XMPP channel.
     * @throws Exception if unable to initialize the pipeline
     */
    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        XmppChannelHandler handler = new XmppChannelHandler(deviceFactory);

        pipeline.addLast("xmppencoder", new XmppEncoder());
        pipeline.addLast("xmlstreamdecoder", new XmlStreamDecoder());
        pipeline.addLast("xmlmerger", new XmlMerger());
        pipeline.addLast("xmppdecoder", new XmppDecoder());
        pipeline.addLast("handler", handler);

    }
}
