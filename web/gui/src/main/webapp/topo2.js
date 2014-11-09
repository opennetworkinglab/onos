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

/*
 ONOS network topology viewer - version 1.1

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    // shorter names for library APIs
    var d3u = onos.lib.d3util;

    // configuration data
    var config = {
        useLiveData: true,
        debugOn: false,
        debug: {
            showNodeXY: true,
            showKeyHandler: false
        },
        options: {
            layering: true,
            collisionPrevention: true,
            showBackground: true
        },
        backgroundUrl: 'img/us-map.png',
        webSockUrl: 'ws/topology',
        data: {
            live: {
                jsonUrl: 'rs/topology/graph',
                detailPrefix: 'rs/topology/graph/',
                detailSuffix: ''
            },
            fake: {
                jsonUrl: 'json/network2.json',
                detailPrefix: 'json/',
                detailSuffix: '.json'
            }
        },
        labels: {
            imgPad: 16,
            padLR: 4,
            padTB: 3,
            marginLR: 3,
            marginTB: 2,
            port: {
                gap: 3,
                width: 18,
                height: 14
            }
        },
        topo: {
            linkInColor: '#66f',
            linkInWidth: 14
        },
        icons: {
            w: 28,
            h: 28,
            xoff: -12,
            yoff: -8
        },
        iconUrl: {
            device: 'img/device.png',
            host: 'img/host.png',
            pkt: 'img/pkt.png',
            opt: 'img/opt.png'
        },
        force: {
            note: 'node.class or link.class is used to differentiate',
            linkDistance: {
                infra: 200,
                host: 40
            },
            linkStrength: {
                infra: 1.0,
                host: 1.0
            },
            charge: {
                device: -400,
                host: -100
            },
            pad: 20,
            translate: function() {
                return 'translate(' +
                    config.force.pad + ',' +
                    config.force.pad + ')';
            }
        }
    };

    // radio buttons
    var btnSet = [
        { text: 'All Layers', cb: showAllLayers },
        { text: 'Packet Only', cb: showPacketLayer },
        { text: 'Optical Only', cb: showOpticalLayer }
    ];

    // key bindings
    var keyDispatch = {
        M: testMe,                  // TODO: remove (testing only)
        S: injectStartupEvents,     // TODO: remove (testing only)
        space: injectTestEvent,     // TODO: remove (testing only)

        B: toggleBg,                // TODO: do we really need this?
        L: cycleLabels,
        P: togglePorts,
        U: unpin,

        X: requestPath
    };

    // state variables
    var network = {
            view: null,     // view token reference
            nodes: [],
            links: [],
            lookup: {}
        },
        webSock,
        labelIdx = 0,

        selectOrder = [],
        selections = {},

        highlighted = null,
        hovered = null,
        viewMode = 'showAll',
        portLabelsOn = false;

    // D3 selections
    var svg,
        bgImg,
        topoG,
        nodeG,
        linkG,
        node,
        link;

    // ==============================
    // For Debugging / Development

    var eventPrefix = 'json/eventTest_',
        eventNumber = 0,
        alertNumber = 0;

    function note(label, msg) {
        console.log('NOTE: ' + label + ': ' + msg);
    }

    function debug(what) {
        return config.debugOn && config.debug[what];
    }


    // ==============================
    // Key Callbacks

    function testMe(view) {
        view.alert('test');
    }

    function injectTestEvent(view) {
        if (config.useLiveData) {
            view.alert("Sorry, currently using live data..");
            return;
        }

        eventNumber++;
        var eventUrl = eventPrefix + eventNumber + '.json';

        d3.json(eventUrl, function(err, data) {
            if (err) {
                view.dataLoadError(err, eventUrl);
            } else {
                handleServerEvent(data);
            }
        });
    }

    function injectStartupEvents(view) {
        if (config.useLiveData) {
            view.alert("Sorry, currently using live data..");
            return;
        }

        var lastStartupEvent = 32;
        while (eventNumber < lastStartupEvent) {
            injectTestEvent(view);
        }
    }

    function toggleBg() {
        var vis = bgImg.style('visibility');
        bgImg.style('visibility', (vis === 'hidden') ? 'visible' : 'hidden');
    }

    function cycleLabels() {
        labelIdx = (labelIdx === network.deviceLabelCount - 1) ? 0 : labelIdx + 1;
        network.nodes.forEach(function (d) {
            var idx = (labelIdx < d.labels.length) ? labelIdx : 0,
                node = d3.select('#' + safeId(d.id)),
                box;

            node.select('text')
                .text(d.labels[idx])
                .style('opacity', 0)
                .transition()
                .style('opacity', 1);

            box = adjustRectToFitText(node);

            node.select('rect')
                .transition()
                .attr(box);

            node.select('image')
                .transition()
                .attr('x', box.x + config.icons.xoff)
                .attr('y', box.y + config.icons.yoff);
        });
    }

    function togglePorts(view) {
        view.alert('togglePorts() callback')
    }

    function unpin(view) {
        view.alert('unpin() callback')
    }

    function requestPath(view) {
        var payload = {
            one: selections[selectOrder[0]].obj.id,
            two: selections[selectOrder[1]].obj.id
        }
        sendMessage('requestPath', payload);
    }

    // ==============================
    // Radio Button Callbacks

    function showAllLayers() {
//        network.node.classed('inactive', false);
//        network.link.classed('inactive', false);
//        d3.selectAll('svg .port').classed('inactive', false);
//        d3.selectAll('svg .portText').classed('inactive', false);
        // TODO ...
        network.view.alert('showAllLayers() callback');
    }

    function showPacketLayer() {
        showAllLayers();
        // TODO ...
        network.view.alert('showPacketLayer() callback');
    }

    function showOpticalLayer() {
        showAllLayers();
        // TODO ...
        network.view.alert('showOpticalLayer() callback');
    }

    // ==============================
    // Private functions

    function safeId(s) {
        return s.replace(/[^a-z0-9]/gi, '-');
    }

    // set the size of the given element to that of the view (reduced if padded)
    function setSize(el, view, pad) {
        var padding = pad ? pad * 2 : 0;
        el.attr({
            width: view.width() - padding,
            height: view.height() - padding
        });
    }


    // ==============================
    // Event handlers for server-pushed events

    var eventDispatch = {
        addDevice: addDevice,
        updateDevice: updateDevice,
        removeDevice: removeDevice,
        addLink: addLink,
        showPath: showPath
    };

    function addDevice(data) {
        var device = data.payload,
            node = createDeviceNode(device);
        note('addDevice', device.id);

        network.nodes.push(node);
        network.lookup[node.id] = node;
        updateNodes();
        network.force.start();
    }

    function updateDevice(data) {
        var device = data.payload;
        note('updateDevice', device.id);

    }

    function removeDevice(data) {
        var device = data.payload;
        note('removeDevice', device.id);

    }

    function addLink(data) {
        var link = data.payload,
            lnk = createLink(link);

        if (lnk) {
            note('addLink', lnk.id);

            network.links.push(lnk);
            updateLinks();
            network.force.start();
        }
    }

    function showPath(data) {
        network.view.alert(data.event + "\n" + data.payload.links.length);
    }

    // ....

    function unknownEvent(data) {
        network.view.alert('Unknown event type: "' + data.event + '"');
    }

    function handleServerEvent(data) {
        var fn = eventDispatch[data.event] || unknownEvent;
        fn(data);
    }

    // ==============================
    // force layout modification functions

    function translate(x, y) {
        return 'translate(' + x + ',' + y + ')';
    }

    function createLink(link) {
        var type = link.type,
            src = link.src,
            dst = link.dst,
            w = link.linkWidth,
            srcNode = network.lookup[src],
            dstNode = network.lookup[dst],
            lnk;

        if (!(srcNode && dstNode)) {
            // TODO: send warning message back to server on websocket
            network.view.alert('nodes not on map for link\n\n' +
                'src = ' + src + '\ndst = ' + dst);
            return null;
        }

        lnk = {
                id: safeId(src) + '~' + safeId(dst),
                source: srcNode,
                target: dstNode,
                class: 'link',
                svgClass: type ? 'link ' + type : 'link',
                x1: srcNode.x,
                y1: srcNode.y,
                x2: dstNode.x,
                y2: dstNode.y,
                width: w
            };
        return lnk;
    }

    function linkWidth(w) {
        // w is number of links between nodes. Scale appropriately.
        // TODO: use a d3.scale (linear, log, ... ?)
        return w * 1.2;
    }

    function updateLinks() {
        link = linkG.selectAll('.link')
            .data(network.links, function (d) { return d.id; });

        // operate on existing links, if necessary
        // link .foo() .bar() ...

        // operate on entering links:
        var entering = link.enter()
            .append('line')
            .attr({
                id: function (d) { return d.id; },
                class: function (d) { return d.svgClass; },
                x1: function (d) { return d.x1; },
                y1: function (d) { return d.y1; },
                x2: function (d) { return d.x2; },
                y2: function (d) { return d.y2; },
                stroke: config.topo.linkInColor,
                'stroke-width': config.topo.linkInWidth
            })
            .transition().duration(1000)
            .attr({
                'stroke-width': function (d) { return linkWidth(d.width); },
                stroke: '#666'      // TODO: remove explicit stroke, rather...
            });

        // augment links
        // TODO: add src/dst port labels etc.

    }

    function createDeviceNode(device) {
        // start with the object as is
        var node = device,
            type = device.type;

        // Augment as needed...
        node.class = 'device';
        node.svgClass = type ? 'node device ' + type : 'node device';
        positionNode(node);

        // cache label array length
        network.deviceLabelCount = device.labels.length;

        return node;
    }

    function positionNode(node) {
        var meta = node.metaUi,
            x = 0,
            y = 0;

        if (meta) {
            x = meta.x;
            y = meta.y;
        }
        if (x && y) {
            node.fixed = true;
        }
        node.x = x || network.view.width() / 2;
        node.y = y || network.view.height() / 2;
    }


    function iconUrl(d) {
        return 'img/' + d.type + '.png';
    }

    // returns the newly computed bounding box of the rectangle
    function adjustRectToFitText(n) {
        var text = n.select('text'),
            box = text.node().getBBox(),
            lab = config.labels;

        text.attr('text-anchor', 'middle')
            .attr('y', '-0.8em')
            .attr('x', lab.imgPad/2);

        // translate the bbox so that it is centered on [x,y]
        box.x = -box.width / 2;
        box.y = -box.height / 2;

        // add padding
        box.x -= (lab.padLR + lab.imgPad/2);
        box.width += lab.padLR * 2 + lab.imgPad;
        box.y -= lab.padTB;
        box.height += lab.padTB * 2;

        return box;
    }

    function mkSvgClass(d) {
        return d.fixed ? d.svgClass + ' fixed' : d.svgClass;
    }

    function updateNodes() {
        node = nodeG.selectAll('.node')
            .data(network.nodes, function (d) { return d.id; });

        // operate on existing nodes, if necessary
        //node .foo() .bar() ...

        // operate on entering nodes:
        var entering = node.enter()
            .append('g')
            .attr({
                id: function (d) { return safeId(d.id); },
                class: mkSvgClass,
                transform: function (d) { return translate(d.x, d.y); },
                opacity: 0
            })
            .call(network.drag)
            //.on('mouseover', function (d) {})
            //.on('mouseover', function (d) {})
            .transition()
            .attr('opacity', 1);

        // augment device nodes...
        entering.filter('.device').each(function (d) {
            var node = d3.select(this),
                icon = iconUrl(d),
                idx = (labelIdx < d.labels.length) ? labelIdx : 0,
                box;

            node.append('rect')
                .attr({
                    'rx': 5,
                    'ry': 5
                });

            node.append('text')
                .text(d.labels[idx])
                .attr('dy', '1.1em');

            box = adjustRectToFitText(node);

            node.select('rect')
                .attr(box);

            if (icon) {
                var cfg = config.icons;
                node.append('svg:image')
                    .attr({
                        x: box.x + config.icons.xoff,
                        y: box.y + config.icons.yoff,
                        width: cfg.w,
                        height: cfg.h,
                        'xlink:href': icon
                    });
            }

            // debug function to show the modelled x,y coordinates of nodes...
            if (debug('showNodeXY')) {
                node.select('rect').attr('fill-opacity', 0.5);
                node.append('circle')
                    .attr({
                        class: 'debug',
                        cx: 0,
                        cy: 0,
                        r: '3px'
                    });
            }
        });


        // operate on both existing and new nodes, if necessary
        //node .foo() .bar() ...

        // operate on exiting nodes:
        // TODO: figure out how to remove the node 'g' AND its children
        node.exit()
            .transition()
            .duration(750)
            .attr({
                opacity: 0,
                cx: 0,
                cy: 0,
                r: 0
            })
            .remove();
    }


    function tick() {
        node.attr({
            transform: function (d) { return translate(d.x, d.y); }
        });

        link.attr({
            x1: function (d) { return d.source.x; },
            y1: function (d) { return d.source.y; },
            x2: function (d) { return d.target.x; },
            y2: function (d) { return d.target.y; }
        });
    }

    // ==============================
    // Web-Socket for live data

    function webSockUrl() {
        return document.location.toString()
            .replace(/\#.*/, '')
            .replace('http://', 'ws://')
            .replace('https://', 'wss://')
            .replace('index2.html', config.webSockUrl);
    }

    webSock = {
        ws : null,

        connect : function() {
            webSock.ws = new WebSocket(webSockUrl());

            webSock.ws.onopen = function() {
                webSock._send("Hi there!");
            };

            webSock.ws.onmessage = function(m) {
                if (m.data) {
                    console.log(m.data);
                    handleServerEvent(JSON.parse(m.data));
                }
            };

            webSock.ws.onclose = function(m) {
                webSock.ws = null;
            };
        },

        send : function(text) {
            if (text != null) {
                webSock._send(text);
            }
        },

        _send : function(message) {
            if (webSock.ws) {
                webSock.ws.send(message);
            } else {
                network.view.alert('no web socket open');
            }
        }

    };

    var sid = 0;

    function sendMessage(evType, payload) {
        var toSend = {
            event: evType,
            sid: ++sid,
            payload: payload
        };
        webSock.send(JSON.stringify(toSend));
    }


    // ==============================
    // Selection stuff

    function selectObject(obj, el) {
        var n,
            meta = d3.event.sourceEvent.metaKey;

        if (el) {
            n = d3.select(el);
        } else {
            node.each(function(d) {
                if (d == obj) {
                    n = d3.select(el = this);
                }
            });
        }
        if (!n) return;

        if (meta && n.classed('selected')) {
            deselectObject(obj.id);
            //flyinPane(null);
            return;
        }

        if (!meta) {
            deselectAll();
        }

        // TODO: allow for mutli selections
        var selected = {
            obj : obj,
            el  : el
        };

        selections[obj.id] = selected;
        selectOrder.push(obj.id);

        n.classed('selected', true);
        //flyinPane(obj);
    }

    function deselectObject(id) {
        var obj = selections[id];
        if (obj) {
            d3.select(obj.el).classed('selected', false);
            selections[id] = null;
            // TODO: use splice to remove element
        }
        //flyinPane(null);
    }

    function deselectAll() {
        // deselect all nodes in the network...
        node.classed('selected', false);
        selections = {};
        selectOrder = [];
        //flyinPane(null);
    }


    $('#view').on('click', function(e) {
        if (!$(e.target).closest('.node').length) {
            if (!e.metaKey) {
                deselectAll();
            }
        }
    });

    // ==============================
    // View life-cycle callbacks

    function preload(view, ctx) {
        var w = view.width(),
            h = view.height(),
            idBg = view.uid('bg'),
            showBg = config.options.showBackground ? 'visible' : 'hidden',
            fcfg = config.force,
            fpad = fcfg.pad,
            forceDim = [w - 2*fpad, h - 2*fpad];

        // NOTE: view.$div is a D3 selection of the view's div
        svg = view.$div.append('svg');
        setSize(svg, view);

        // add blue glow filter to svg layer
        d3u.appendGlow(svg);

        // load the background image
        bgImg = svg.append('svg:image')
            .attr({
                id: idBg,
                width: w,
                height: h,
                'xlink:href': config.backgroundUrl
            })
            .style({
                visibility: showBg
            });

        // group for the topology
        topoG = svg.append('g')
            .attr('transform', fcfg.translate());

        // subgroups for links and nodes
        linkG = topoG.append('g').attr('id', 'links');
        nodeG = topoG.append('g').attr('id', 'nodes');

        // selection of nodes and links
        link = linkG.selectAll('.link');
        node = nodeG.selectAll('.node');

        function ldist(d) {
            return fcfg.linkDistance[d.class] || 150;
        }
        function lstrg(d) {
            return fcfg.linkStrength[d.class] || 1;
        }
        function lchrg(d) {
            return fcfg.charge[d.class] || -200;
        }

        function selectCb(d, self) {
            selectObject(d, self);
        }

        function atDragEnd(d, self) {
            // once we've finished moving, pin the node in position,
            // if it is a device (not a host)
            if (d.class === 'device') {
                d.fixed = true;
                d3.select(self).classed('fixed', true);
                if (config.useLiveData) {
                    tellServerCoords(d);
                }
            }
        }

        function tellServerCoords(d) {
            sendMessage('updateMeta', {
                id: d.id,
                'class': d.class,
                x: d.x,
                y: d.y
            });
        }

        // set up the force layout
        network.force = d3.layout.force()
            .size(forceDim)
            .nodes(network.nodes)
            .links(network.links)
            .charge(lchrg)
            .linkDistance(ldist)
            .linkStrength(lstrg)
            .on('tick', tick);

        network.drag = d3u.createDragBehavior(network.force, selectCb, atDragEnd);
    }

    function load(view, ctx) {
        // cache the view token, so network topo functions can access it
        network.view = view;

        // set our radio buttons and key bindings
        view.setRadio(btnSet);
        view.setKeys(keyDispatch);

        if (config.useLiveData) {
            webSock.connect();
        }
    }

    function resize(view, ctx) {
        setSize(svg, view);
        setSize(bgImg, view);

        // TODO: hook to recompute layout, perhaps? work with zoom/pan code
        // adjust force layout size
    }


    // ==============================
    // View registration

    onos.ui.addView('topo', {
        preload: preload,
        load: load,
        resize: resize
    });

}(ONOS));
