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
    var d3u = onos.lib.d3util,
        trace;

    // configuration data
    var config = {
        useLiveData: true,
        fnTrace: true,
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
            note_for_links: 'link.type is used to differentiate',
            linkDistance: {
                direct: 100,
                optical: 120,
                hostLink: 3
            },
            linkStrength: {
                direct: 1.0,
                optical: 1.0,
                hostLink: 1.0
            },
            note_for_nodes: 'node.class is used to differentiate',
            charge: {
                device: -8000,
                host: -5000
            },
            pad: 20,
            translate: function() {
                return 'translate(' +
                    config.force.pad + ',' +
                    config.force.pad + ')';
            }
        },
        // see below in creation of viewBox on main svg
        logicalSize: 1000
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

        B: toggleBg,
        L: cycleLabels,
        P: togglePorts,
        U: unpin,
        R: resetZoomPan,
        esc: deselectAll
    };

    // state variables
    var network = {
            view: null,     // view token reference
            nodes: [],
            links: [],
            lookup: {}
        },
        scenario = {
            evDir: 'json/ev/',
            evScenario: '/scenario.json',
            evPrefix: '/ev_',
            evOnos: '_onos.json',
            evUi: '_ui.json',
            ctx: null,
            params: {},
            evNumber: 0,
            view: null,
            debug: false
        },
        webSock,
        sid = 0,
        deviceLabelIndex = 0,
        hostLabelIndex = 0,
        selections = {},
        selectOrder = [],
        hovered = null,
        detailPane,
        antTimer = null,
        onosInstances = {},
        onosOrder = [],
        oiBox,

        viewMode = 'showAll',
        portLabelsOn = false;

    // D3 selections
    var svg,
        zoomPanContainer,
        bgImg,
        topoG,
        nodeG,
        linkG,
        node,
        link,
        mask;

    // the projection for the map background
    var geoMapProjection;

    // the zoom function
    var zoom;

    // ==============================
    // For Debugging / Development

    function note(label, msg) {
        console.log('NOTE: ' + label + ': ' + msg);
    }

    function debug(what) {
        return config.debugOn && config.debug[what];
    }

    function fnTrace(msg, id) {
        if (config.fnTrace) {
            console.log('FN: ' + msg + ' [' + id + ']');
        }
    }

    function evTrace(data) {
        fnTrace(data.event, data.payload.id);
    }

    // ==============================
    // Key Callbacks

    function testMe(view) {
        //view.alert('test');
        detailPane.show();
        setTimeout(detailPane.hide, 2000);
        oiBox.show();
        setTimeout(oiBox.hide, 2000);
    }

    function abortIfLive() {
        if (config.useLiveData) {
            network.view.alert("Sorry, currently using live data..");
            return true;
        }
        return false;
    }

    function testDebug(msg) {
        if (scenario.debug) {
            scenario.view.alert(msg);
        }
    }

    function injectTestEvent(view) {
        if (abortIfLive()) { return; }
        var sc = scenario,
            evn = ++sc.evNumber,
            pfx = sc.evDir + sc.ctx + sc.evPrefix + evn,
            onosUrl = pfx + sc.evOnos,
            uiUrl = pfx + sc.evUi,
            stack = [
                { url: onosUrl, cb: handleServerEvent },
                { url: uiUrl, cb: handleUiEvent }
            ];
        recurseFetchEvent(stack, evn);
    }

    function recurseFetchEvent(stack, evn) {
        var v = scenario.view,
            frame;
        if (stack.length === 0) {
            v.alert('Oops!\n\nNo event #' + evn + ' found.');
            return;
        }
        frame = stack.shift();

        d3.json(frame.url, function (err, data) {
            if (err) {
                if (err.status === 404) {
                    // if we didn't find the data, try the next stack frame
                    recurseFetchEvent(stack, evn);
                } else {
                    v.alert('non-404 error:\n\n' + frame.url + '\n\n' + err);
                }
            } else {
                testDebug('loaded: ' + frame.url);
                frame.cb(data);
            }
        });

    }

    function handleUiEvent(data) {
        scenario.view.alert('UI Tx: ' + data.event + '\n\n' +
            JSON.stringify(data));
    }

    function injectStartupEvents(view) {
        var last = scenario.params.lastAuto || 0;
        if (abortIfLive()) { return; }

        while (scenario.evNumber < last) {
            injectTestEvent(view);
        }
    }

    function toggleBg() {
        var vis = bgImg.style('visibility');
        bgImg.style('visibility', (vis === 'hidden') ? 'visible' : 'hidden');
    }

    function cycleLabels() {
        deviceLabelIndex = (deviceLabelIndex === network.deviceLabelCount - 1)
            ? 0 : deviceLabelIndex + 1;

        network.nodes.forEach(function (d) {
            if (d.class === 'device') {
                updateDeviceLabel(d);
            }
        });
    }

    function togglePorts(view) {
        view.alert('togglePorts() callback')
    }

    function unpin() {
        if (hovered) {
            hovered.fixed = false;
            hovered.el.classed('fixed', false);
            network.force.resume();
        }
    }

    // ==============================
    // Radio Button Callbacks

    var layerLookup = {
        host: {
            endstation: 'pkt', // default, if host event does not define type
            bgpSpeaker: 'pkt'
        },
        device: {
            switch: 'pkt',
            roadm: 'opt'
        },
        link: {
            hostLink: 'pkt',
            direct: 'pkt',
            optical: 'opt'
        }
    };

    function inLayer(d, layer) {
        var look = layerLookup[d.class],
            lyr = look && look[d.type];
        return lyr === layer;
    }

    function unsuppressLayer(which) {
        node.each(function (d) {
            var node = d.el;
            if (inLayer(d, which)) {
                node.classed('suppressed', false);
            }
        });

        link.each(function (d) {
            var link = d.el;
            if (inLayer(d, which)) {
                link.classed('suppressed', false);
            }
        });
    }

    function showAllLayers() {
        node.classed('suppressed', false);
        link.classed('suppressed', false);
//        d3.selectAll('svg .port').classed('inactive', false);
//        d3.selectAll('svg .portText').classed('inactive', false);
    }

    function showPacketLayer() {
        node.classed('suppressed', true);
        link.classed('suppressed', true);
        unsuppressLayer('pkt');
    }

    function showOpticalLayer() {
        node.classed('suppressed', true);
        link.classed('suppressed', true);
        unsuppressLayer('opt');
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

    function logicError(msg) {
        // TODO, report logic error to server, via websock, so it can be logged
        network.view.alert('Logic Error:\n\n' + msg);
        console.warn(msg);
    }

    var eventDispatch = {
        addInstance: addInstance,
        addDevice: addDevice,
        addLink: addLink,
        addHost: addHost,

        updateInstance: stillToImplement,
        updateDevice: updateDevice,
        updateLink: updateLink,
        updateHost: updateHost,

        removeInstance: stillToImplement,
        removeDevice: stillToImplement,
        removeLink: removeLink,
        removeHost: removeHost,

        showDetails: showDetails,
        showPath: showPath,
        showTraffic: showTraffic
    };

    function addInstance(data) {
        evTrace(data);
        var inst = data.payload,
            id = inst.id;
        if (onosInstances[id]) {
            logicError('ONOS instance already added: ' + id);
            return;
        }
        onosInstances[id] = inst;
        onosOrder.push(inst);
        updateInstances();
    }

    function addDevice(data) {
        evTrace(data);
        var device = data.payload,
            nodeData = createDeviceNode(device);
        network.nodes.push(nodeData);
        network.lookup[nodeData.id] = nodeData;
        updateNodes();
        network.force.start();
    }

    function addLink(data) {
        evTrace(data);
        var link = data.payload,
            lnk = createLink(link);
        if (lnk) {
            network.links.push(lnk);
            network.lookup[lnk.id] = lnk;
            updateLinks();
            network.force.start();
        }
    }

    function addHost(data) {
        evTrace(data);
        var host = data.payload,
            node = createHostNode(host),
            lnk;
        network.nodes.push(node);
        network.lookup[host.id] = node;
        updateNodes();

        lnk = createHostLink(host);
        if (lnk) {
            node.linkData = lnk;    // cache ref on its host
            network.links.push(lnk);
            network.lookup[host.ingress] = lnk;
            network.lookup[host.egress] = lnk;
            updateLinks();
        }
        network.force.start();
    }

    // TODO: fold updateX(...) methods into one base method; remove duplication
    function updateDevice(data) {
        evTrace(data);
        var device = data.payload,
            id = device.id,
            nodeData = network.lookup[id];
        if (nodeData) {
            $.extend(nodeData, device);
            updateDeviceState(nodeData);
        } else {
            logicError('updateDevice lookup fail. ID = "' + id + '"');
        }
    }

    function updateLink(data) {
        evTrace(data);
        var link = data.payload,
            id = link.id,
            linkData = network.lookup[id];
        if (linkData) {
            $.extend(linkData, link);
            updateLinkState(linkData);
        } else {
            logicError('updateLink lookup fail. ID = "' + id + '"');
        }
    }

    function updateHost(data) {
        evTrace(data);
        var host = data.payload,
            id = host.id,
            hostData = network.lookup[id];
        if (hostData) {
            $.extend(hostData, host);
            updateHostState(hostData);
        } else {
            logicError('updateHost lookup fail. ID = "' + id + '"');
        }
    }

    // TODO: fold removeX(...) methods into base method - remove dup code
    function removeLink(data) {
        evTrace(data);
        var link = data.payload,
            id = link.id,
            linkData = network.lookup[id];
        if (linkData) {
            removeLinkElement(linkData);
        } else {
            logicError('removeLink lookup fail. ID = "' + id + '"');
        }
    }

    function removeHost(data) {
        evTrace(data);
        var host = data.payload,
            id = host.id,
            hostData = network.lookup[id];
        if (hostData) {
            removeHostElement(hostData);
        } else {
            logicError('removeHost lookup fail. ID = "' + id + '"');
        }
    }

    function showDetails(data) {
        evTrace(data);
        populateDetails(data.payload);
        detailPane.show();
    }

    function showPath(data) {
        // TODO: review - making sure we are handling the payload correctly.
        evTrace(data);
        var links = data.payload.links,
            s = [ data.event + "\n" + links.length ];
        links.forEach(function (d, i) {
            s.push(d);
        });
        network.view.alert(s.join('\n'));

        links.forEach(function (d, i) {
            var link = network.lookup[d];
            if (link) {
                link.el.classed('showPath', true);
            }
        });
    }

    function showTraffic(data) {
        evTrace(data);
        var paths = data.payload.paths;

        // Revert any links hilighted previously.
        link.classed('primary secondary animated optical', false);

        // Now hilight all links in the paths payload.
        paths.forEach(function (p) {
            var cls = p.class;
            p.links.forEach(function (id) {
                var lnk = network.lookup[id];
                if (lnk) {
                    lnk.el.classed(cls, true);
                }
            });
        });
    }

    // ...............................

    function stillToImplement(data) {
        var p = data.payload;
        note(data.event, p.id);
        network.view.alert('Not yet implemented: "' + data.event + '"');
    }

    function unknownEvent(data) {
        network.view.alert('Unknown event type: "' + data.event + '"');
    }

    function handleServerEvent(data) {
        var fn = eventDispatch[data.event] || unknownEvent;
        fn(data);
    }

    // ==============================
    // Out-going messages...

    function userFeedback(msg) {
        // for now, use the alert pane as is. Maybe different alert style in
        // the future (centered on view; dismiss button?)
        network.view.alert(msg);
    }

    function nSel() {
        return selectOrder.length;
    }
    function getSel(idx) {
        return selections[selectOrder[idx]];
    }
    function getSelId(idx) {
        return getSel(idx).obj.id;
    }
    function allSelectionsClass(cls) {
        for (var i=0, n=nSel(); i<n; i++) {
            if (getSel(i).obj.class !== cls) {
                return false;
            }
        }
        return true;
    }

    // request details for the selected element
    // invoked from selection of a single node.
    function requestDetails() {
        var data = getSel(0).obj,
            payload = {
                id: data.id,
                class: data.class
            };
        sendMessage('requestDetails', payload);
    }

    function addIntentAction() {
        sendMessage('addHostIntent', {
            one: getSelId(0),
            two: getSelId(1)
        });
    }

    function showTrafficAction() {
        // if nothing is hovered over, and nothing selected, send cancel request
        if (!hovered && nSel() === 0) {
            sendMessage('cancelTraffic', {});
            return;
        }

        // NOTE: hover is only populated if "show traffic on hover" is
        //        toggled on, and the item hovered is a host...
        var hoverId = (trafficHover() && hovered && hovered.class === 'host')
                        ? hovered.id : '';
        sendMessage('requestTraffic', {
            ids: selectOrder,
            hover: hoverId
        });
    }


    // ==============================
    // onos instance panel functions

    function updateInstances() {
        var onoses = oiBox.el.selectAll('.onosInst')
            .data(onosOrder, function (d) { return d.id; });

        // operate on existing onoses if necessary

        var entering = onoses.enter()
            .append('div')
            .attr('class', 'onosInst')
            .classed('online', function (d) { return d.online; })
            .text(function (d) { return d.id; });

        // operate on existing + new onoses here

        // the departed...
        var exiting = onoses.exit()
            .transition()
            .style('opacity', 0)
            .remove();
    }

    // ==============================
    // force layout modification functions

    function translate(x, y) {
        return 'translate(' + x + ',' + y + ')';
    }

    function missMsg(what, id) {
        return '\n[' + what + '] "' + id + '" missing ';
    }

    function linkEndPoints(srcId, dstId) {
        var srcNode = network.lookup[srcId],
            dstNode = network.lookup[dstId],
            sMiss = !srcNode ? missMsg('src', srcId) : '',
            dMiss = !dstNode ? missMsg('dst', dstId) : '';

        if (sMiss || dMiss) {
            logicError('Node(s) not on map for link:\n' + sMiss + dMiss);
            return null;
        }
        return {
            source: srcNode,
            target: dstNode,
            x1: srcNode.x,
            y1: srcNode.y,
            x2: dstNode.x,
            y2: dstNode.y
        };
    }

    function createHostLink(host) {
        var src = host.id,
            dst = host.cp.device,
            id = host.ingress,
            lnk = linkEndPoints(src, dst);

        if (!lnk) {
            return null;
        }

        // Synthesize link ...
        $.extend(lnk, {
            id: id,
            class: 'link',
            type: 'hostLink',
            svgClass: 'link hostLink',
            linkWidth: 1
        });
        return lnk;
    }

    function createLink(link) {
        var lnk = linkEndPoints(link.src, link.dst),
            type = link.type;

        if (!lnk) {
            return null;
        }

        // merge in remaining data
        $.extend(lnk, link, {
            class: 'link',
            svgClass: type ? 'link ' + type : 'link'
        });
        return lnk;
    }

    var widthRatio = 1.4,
        linkScale = d3.scale.linear()
            .domain([1, 12])
            .range([widthRatio, 12 * widthRatio])
            .clamp(true);

    function updateLinkWidth (d) {
        // TODO: watch out for .showPath/.showTraffic classes
        d.el.transition()
            .duration(1000)
            .attr('stroke-width', linkScale(d.linkWidth));
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
                'stroke-width': function (d) { return linkScale(d.linkWidth); },
                stroke: '#666'      // TODO: remove explicit stroke, rather...
            });

        // augment links
        entering.each(function (d) {
            var link = d3.select(this);
            // provide ref to element selection from backing data....
            d.el = link;

            // TODO: add src/dst port labels etc.
        });

        // operate on both existing and new links, if necessary
        //link .foo() .bar() ...

        // operate on exiting links:
        link.exit()
            .attr({
                'stroke-dasharray': '3, 3'
            })
            .style('opacity', 0.4)
            .transition()
            .duration(1500)
            .attr({
                'stroke-dasharray': '3, 12'
            })
            .transition()
            .duration(500)
            .style('opacity', 0.0)
            .remove();
    }

    function createDeviceNode(device) {
        // start with the object as is
        var node = device,
            type = device.type,
            svgCls = type ? 'node device ' + type : 'node device';

        // Augment as needed...
        node.class = 'device';
        node.svgClass = device.online ? svgCls + ' online' : svgCls;
        positionNode(node);

        // cache label array length
        network.deviceLabelCount = device.labels.length;
        return node;
    }

    function createHostNode(host) {
        // start with the object as is
        var node = host;

        // Augment as needed...
        node.class = 'host';
        if (!node.type) {
            node.type = 'endstation';
        }
        node.svgClass = 'node host';
        positionNode(node);

        // cache label array length
        network.hostLabelCount = host.labels.length;
        return node;
    }

    function positionNode(node) {
        var meta = node.metaUi,
            x = meta && meta.x,
            y = meta && meta.y,
            xy;

        // If we have [x,y] already, use that...
        if (x && y) {
            node.fixed = true;
            node.x = x;
            node.y = y;
            return;
        }

        var location = node.location;
        if (location && location.type === 'latlng') {
            var coord = geoMapProjection([location.lng, location.lat]);
            node.fixed = true;
            node.x = coord[0];
            node.y = coord[1];
            return;
        }

        // Note: Placing incoming unpinned nodes at exactly the same point
        //        (center of the view) causes them to explode outwards when
        //        the force layout kicks in. So, we spread them out a bit
        //        initially, to provide a more serene layout convergence.
        //       Additionally, if the node is a host, we place it near
        //        the device it is connected to.

        function spread(s) {
            return Math.floor((Math.random() * s) - s/2);
        }

        function randDim(dim) {
            return dim / 2 + spread(dim * 0.7071);
        }

        function rand() {
            return {
                x: randDim(network.view.width()),
                y: randDim(network.view.height())
            };
        }

        function near(node) {
            var min = 12,
                dx = spread(12),
                dy = spread(12);
            return {
                x: node.x + min + dx,
                y: node.y + min + dy
            };
        }

        function getDevice(cp) {
            var d = network.lookup[cp.device];
            return d || rand();
        }

        xy = (node.class === 'host') ? near(getDevice(node.cp)) : rand();
        $.extend(node, xy);
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

    function hostLabel(d) {
        var idx = (hostLabelIndex < d.labels.length) ? hostLabelIndex : 0;
        return d.labels[idx];
    }
    function deviceLabel(d) {
        var idx = (deviceLabelIndex < d.labels.length) ? deviceLabelIndex : 0;
        return d.labels[idx];
    }
    function niceLabel(label) {
        return (label && label.trim()) ? label : '.';
    }

    function updateDeviceLabel(d) {
        var label = niceLabel(deviceLabel(d)),
            node = d.el,
            box;

        node.select('text')
            .text(label)
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
    }

    function updateHostLabel(d) {
        var label = hostLabel(d),
            host = d.el;

        host.select('text').text(label);
    }

    function updateDeviceState(nodeData) {
        nodeData.el.classed('online', nodeData.online);
        updateDeviceLabel(nodeData);
        // TODO: review what else might need to be updated
    }

    function updateLinkState(linkData) {
        updateLinkWidth(linkData);
        // TODO: review what else might need to be updated
        //  update label, if showing
    }

    function updateHostState(hostData) {
        updateHostLabel(hostData);
        // TODO: review what else might need to be updated
    }

    function nodeMouseOver(d) {
        hovered = d;
        if (trafficHover() && d.class === 'host') {
            showTrafficAction();
        }
    }

    function nodeMouseOut(d) {
        hovered = null;
        if (trafficHover() && d.class === 'host') {
            showTrafficAction();
        }
    }

    function updateNodes() {
        node = nodeG.selectAll('.node')
            .data(network.nodes, function (d) { return d.id; });

        // operate on existing nodes, if necessary
        //  update host labels
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
            .on('mouseover', nodeMouseOver)
            .on('mouseout', nodeMouseOut)
            .transition()
            .attr('opacity', 1);

        // augment device nodes...
        entering.filter('.device').each(function (d) {
            var node = d3.select(this),
                icon = iconUrl(d),
                label = niceLabel(deviceLabel(d)),
                box;

            // provide ref to element from backing data....
            d.el = node;

            node.append('rect')
                .attr({
                    'rx': 5,
                    'ry': 5
                });

            node.append('text')
                .text(label)
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

        // augment host nodes...
        entering.filter('.host').each(function (d) {
            var node = d3.select(this),
                box;

            // provide ref to element from backing data....
            d.el = node;

            node.append('circle')
                .attr('r', 8);     // TODO: define host circle radius

            node.append('text')
                .text(hostLabel)
                .attr('dy', '1.3em')
                .attr('text-anchor', 'middle');

            // debug function to show the modelled x,y coordinates of nodes...
            if (debug('showNodeXY')) {
                node.select('circle').attr('fill-opacity', 0.5);
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
        // Note that the node is removed after 2 seconds.
        // Sub element animations should be shorter than 2 seconds.
        var exiting = node.exit()
            .transition()
            .duration(2000)
            .style('opacity', 0)
            .remove();

        // host node exits....
        exiting.filter('.host').each(function (d) {
            var node = d3.select(this);

            node.select('text')
                .style('opacity', 0.5)
                .transition()
                .duration(1000)
                .style('opacity', 0);
            // note, leave <g>.remove to remove this element

            node.select('circle')
                .style('stroke-fill', '#555')
                .style('fill', '#888')
                .style('opacity', 0.5)
                .transition()
                .duration(1500)
                .attr('r', 0);
            // note, leave <g>.remove to remove this element

        });

        // TODO: device node exits
    }

    function find(id, array) {
        for (var idx = 0, n = array.length; idx < n; idx++) {
            if (array[idx].id === id) {
                return idx;
            }
        }
        return -1;
    }

    function removeLinkElement(linkData) {
        // remove from lookup cache
        delete network.lookup[linkData.id];
        // remove from links array
        var idx = find(linkData.id, network.links);
        network.links.splice(idx, 1);
        // remove from SVG
        updateLinks();
        network.force.resume();
    }

    function removeHostElement(hostData) {
        // first, remove associated hostLink...
        removeLinkElement(hostData.linkData);

        // remove from lookup cache
        delete network.lookup[hostData.id];
        // remove from nodes array
        var idx = find(hostData.id, network.nodes);
        network.nodes.splice(idx, 1);
        // remove from SVG
        updateNodes();
        network.force.resume();
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
                noWebSock(false);
            };

            webSock.ws.onmessage = function(m) {
                if (m.data) {
                    wsTraceRx(m.data);
                    handleServerEvent(JSON.parse(m.data));
                }
            };

            webSock.ws.onclose = function(m) {
                webSock.ws = null;
                noWebSock(true);
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
            } else if (config.useLiveData) {
                network.view.alert('no web socket open\n\n' + message);
            } else {
                console.log('WS Send: ' + JSON.stringify(message));
            }
        }

    };

    function noWebSock(b) {
        mask.style('display',b ? 'block' : 'none');
    }

    // TODO: use cache of pending messages (key = sid) to reconcile responses

    function sendMessage(evType, payload) {
        var toSend = {
                event: evType,
                sid: ++sid,
                payload: payload
            },
            asText = JSON.stringify(toSend);
        wsTraceTx(asText);
        webSock.send(asText);
    }

    function wsTraceTx(msg) {
        wsTrace('tx', msg);
    }
    function wsTraceRx(msg) {
        wsTrace('rx', msg);
    }
    function wsTrace(rxtx, msg) {
        console.log('[' + rxtx + '] ' + msg);
        // TODO: integrate with trace view
        //if (trace) {
        //    trace.output(rxtx, msg);
        //}
    }


    // ==============================
    // Selection stuff

    function selectObject(obj, el) {
        var n,
            srcEv = d3.event.sourceEvent,
            meta = srcEv.metaKey,
            shift = srcEv.shiftKey;

        if ((panZoom() && !meta) || (!panZoom() && meta)) {
            return;
        }

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

        if (shift && n.classed('selected')) {
            deselectObject(obj.id);
            updateDetailPane();
            return;
        }

        if (!shift) {
            deselectAll();
        }

        selections[obj.id] = { obj: obj, el: el };
        selectOrder.push(obj.id);

        n.classed('selected', true);
        updateDetailPane();
    }

    function deselectObject(id) {
        var obj = selections[id],
            idx;
        if (obj) {
            d3.select(obj.el).classed('selected', false);
            delete selections[id];
            idx = $.inArray(id, selectOrder);
            if (idx >= 0) {
                selectOrder.splice(idx, 1);
            }
        }
    }

    function deselectAll() {
        // deselect all nodes in the network...
        node.classed('selected', false);
        selections = {};
        selectOrder = [];
        updateDetailPane();
    }

    // update the state of the detail pane, based on current selections
    function updateDetailPane() {
        var nSel = selectOrder.length;
        if (!nSel) {
            detailPane.hide();
            showTrafficAction();        // sends cancelTraffic event
        } else if (nSel === 1) {
            singleSelect();
        } else {
            multiSelect();
        }
    }

    function singleSelect() {
        requestDetails();
        // NOTE: detail pane will be shown from showDetails event callback
    }

    function multiSelect() {
        populateMultiSelect();
    }

    function addSep(tbody) {
        var tr = tbody.append('tr');
        $('<hr>').appendTo(tr.append('td').attr('colspan', 2));
    }

    function addProp(tbody, label, value) {
        var tr = tbody.append('tr');

        tr.append('td')
            .attr('class', 'label')
            .text(label + ' :');

        tr.append('td')
            .attr('class', 'value')
            .text(value);
    }

    function populateMultiSelect() {
        detailPane.empty();

        var title = detailPane.append("h2"),
            table = detailPane.append("table"),
            tbody = table.append("tbody");

        title.text('Multi-Select...');

        selectOrder.forEach(function (d, i) {
            addProp(tbody, i+1, d);
        });

        addMultiSelectActions();
    }

    function populateDetails(data) {
        detailPane.empty();

        var title = detailPane.append("h2"),
            table = detailPane.append("table"),
            tbody = table.append("tbody");

        $('<img src="img/' + data.type + '.png">').appendTo(title);
        $('<span>').attr('class', 'icon').text(data.id).appendTo(title);

        data.propOrder.forEach(function(p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, data.props[p]);
            }
        });

        addSingleSelectActions();
    }

    function addSingleSelectActions() {
        detailPane.append('hr');
        // always want to allow 'show traffic'
        addAction(detailPane, 'Show Traffic', showTrafficAction);
    }

    function addMultiSelectActions() {
        detailPane.append('hr');
        // always want to allow 'show traffic'
        addAction(detailPane, 'Show Traffic', showTrafficAction);
        // if exactly two hosts are selected, also want 'add host intent'
        if (nSel() === 2 && allSelectionsClass('host')) {
            addAction(detailPane, 'Add Host Intent', addIntentAction);
        }
    }

    function addAction(panel, text, cb) {
        panel.append('div')
            .classed('actionBtn', true)
            .text(text)
            .on('click', cb);
    }


    function zoomPan(scale, translate) {
        zoomPanContainer.attr("transform", "translate(" + translate + ")scale(" + scale + ")");
        // keep the map lines constant width while zooming
        bgImg.style("stroke-width", 2.0 / scale + "px");
    }

    function resetZoomPan() {
        zoomPan(1, [0,0]);
        zoom.scale(1).translate([0,0]);
    }

    function setupZoomPan() {
        function zoomed() {
            if (!panZoom() ^ !d3.event.sourceEvent.metaKey) {
                zoomPan(d3.event.scale, d3.event.translate);
            }
        }

        zoom = d3.behavior.zoom()
            .translate([0, 0])
            .scale(1)
            .scaleExtent([1, 8])
            .on("zoom", zoomed);

        svg.call(zoom);
    }

    // ==============================
    // Test harness code

    function prepareScenario(view, ctx, dbg) {
        var sc = scenario,
            urlSc = sc.evDir + ctx + sc.evScenario;

        if (!ctx) {
            view.alert("No scenario specified (null ctx)");
            return;
        }

        sc.view = view;
        sc.ctx = ctx;
        sc.debug = dbg;
        sc.evNumber = 0;

        d3.json(urlSc, function(err, data) {
            var p = data && data.params || {},
                desc = data && data.description || null,
                intro = data && data.title;

            if (err) {
                view.alert('No scenario found:\n\n' + urlSc + '\n\n' + err);
            } else {
                sc.params = p;
                if (desc) {
                    intro += '\n\n  ' + desc.join('\n  ');
                }
                view.alert(intro);
            }
        });

    }

    // ==============================
    // Toggle Buttons in masthead

    // TODO: toggle button (and other widgets in the masthead) should be provided
    //  by the framework; not generated by the view.

    var showInstances,
        doPanZoom,
        showTrafficOnHover;

    function addButtonBar(view) {
        var bb = d3.select('#mast')
            .append('span').classed('right', true).attr('id', 'bb');

        function mkTogBtn(text, cb) {
            return bb.append('span')
                .classed('btn', true)
                .text(text)
                .on('click', cb);
        }

        showInstances = mkTogBtn('Show Instances', toggleInst);
        doPanZoom = mkTogBtn('Pan/Zoom', togglePanZoom);
        showTrafficOnHover = mkTogBtn('Show traffic on hover', toggleTrafficHover);
    }

    function instShown() {
        return showInstances.classed('active');
    }
    function toggleInst() {
        showInstances.classed('active', !instShown());
        if (instShown()) {
            oiBox.show();
        } else {
            oiBox.hide();
        }
    }

    function panZoom() {
        return doPanZoom.classed('active');
    }
    function togglePanZoom() {
        doPanZoom.classed('active', !panZoom());
    }

    function trafficHover() {
        return showTrafficOnHover.classed('active');
    }
    function toggleTrafficHover() {
        showTrafficOnHover.classed('active', !trafficHover());
    }


    // ==============================
    // View life-cycle callbacks

    function preload(view, ctx, flags) {
        var w = view.width(),
            h = view.height(),
            fcfg = config.force,
            fpad = fcfg.pad,
            forceDim = [w - 2*fpad, h - 2*fpad];

        // NOTE: view.$div is a D3 selection of the view's div
        var viewBox = '0 0 ' + config.logicalSize + ' ' + config.logicalSize;
        svg = view.$div.append('svg').attr('viewBox', viewBox);
        setSize(svg, view);

        zoomPanContainer = svg.append('g').attr('id', 'zoomPanContainer');
        setupZoomPan();

        // add blue glow filter to svg layer
        d3u.appendGlow(zoomPanContainer);

        // group for the topology
        topoG = zoomPanContainer.append('g')
            .attr('id', 'topo-G')
            .attr('transform', fcfg.translate());

        // subgroups for links and nodes
        linkG = topoG.append('g').attr('id', 'links');
        nodeG = topoG.append('g').attr('id', 'nodes');

        // selection of nodes and links
        link = linkG.selectAll('.link');
        node = nodeG.selectAll('.node');

        function chrg(d) {
            return fcfg.charge[d.class] || -12000;
        }
        function ldist(d) {
            return fcfg.linkDistance[d.type] || 50;
        }
        function lstrg(d) {
            // 0.0 - 1.0
            return fcfg.linkStrength[d.type] || 1.0;
        }

        function selectCb(d, self) {
            selectObject(d, self);
        }

        function atDragEnd(d, self) {
            // once we've finished moving, pin the node in position
            d.fixed = true;
            d3.select(self).classed('fixed', true);
            if (config.useLiveData) {
                sendUpdateMeta(d);
            } else {
                console.log('Moving node ' + d.id + ' to [' + d.x + ',' + d.y + ']');
            }
        }

        function sendUpdateMeta(d) {
            sendMessage('updateMeta', {
                id: d.id,
                'class': d.class,
                'memento': {
                    x: d.x,
                    y: d.y
                }
            });
        }

        // set up the force layout
        network.force = d3.layout.force()
            .size(forceDim)
            .nodes(network.nodes)
            .links(network.links)
            .gravity(0.4)
            .friction(0.7)
            .charge(chrg)
            .linkDistance(ldist)
            .linkStrength(lstrg)
            .on('tick', tick);

        network.drag = d3u.createDragBehavior(network.force,
            selectCb, atDragEnd, panZoom);

        // create mask layer for when we lose connection to server.
        // TODO: this should be part of the framework
        mask = view.$div.append('div').attr('id','topo-mask');
        para(mask, 'Oops!');
        para(mask, 'Web-socket connection to server closed...');
        para(mask, 'Try refreshing the page.');
    }

    function para(sel, text) {
        sel.append('p').text(text);
    }


    function load(view, ctx, flags) {
        // resize, in case the window was resized while we were not loaded
        resize(view, ctx, flags);

        // cache the view token, so network topo functions can access it
        network.view = view;
        config.useLiveData = !flags.local;

        if (!config.useLiveData) {
            prepareScenario(view, ctx, flags.debug);
        }

        // set our radio buttons and key bindings
        view.setRadio(btnSet);
        view.setKeys(keyDispatch);

        // patch in our "button bar" for now
        // TODO: implement a more official frameworky way of doing this..
        addButtonBar(view);

        // Load map data asynchronously; complete startup after that..
        loadGeoJsonData();

        // start the and timer
        var dashIdx = 0;
        antTimer = setInterval(function () {
            // TODO: figure out how to choose Src-->Dst and Dst-->Src, per link
            dashIdx = dashIdx === 0 ? 14 : dashIdx - 2;
            d3.selectAll('.animated').style('stroke-dashoffset', dashIdx);
        }, 35);
    }

    function unload(view, ctx, flags) {
        if (antTimer) {
            clearInterval(antTimer);
            antTimer = null;
        }
    }

    // TODO: move these to config/state portion of script
    var geoJsonUrl = 'json/map/continental_us.json',     // TODO: Paul
        geoJson;

    function loadGeoJsonData() {
        d3.json(geoJsonUrl, function (err, data) {
            if (err) {
                // fall back to USA map background
                loadStaticMap();
            } else {
                geoJson = data;
                loadGeoMap();
            }

            // finally, connect to the server...
            if (config.useLiveData) {
                webSock.connect();
            }
        });
    }

    function showBg() {
        return config.options.showBackground ? 'visible' : 'hidden';
    }

    function loadStaticMap() {
        fnTrace('loadStaticMap', config.backgroundUrl);
        var w = network.view.width(),
            h = network.view.height();

        // load the background image
        bgImg = svg.insert('svg:image', '#topo-G')
            .attr({
                id: 'topo-bg',
                width: w,
                height: h,
                'xlink:href': config.backgroundUrl
            })
            .style({
                visibility: showBg()
            });
    }

    function loadGeoMap() {
        fnTrace('loadGeoMap', geoJsonUrl);

        // extracts the topojson data into geocoordinate-based geometry
        var topoData = topojson.feature(geoJson, geoJson.objects.states);

        // see: http://bl.ocks.org/mbostock/4707858
        geoMapProjection = d3.geo.mercator();
        var path = d3.geo.path().projection(geoMapProjection);

        geoMapProjection
            .scale(1)
            .translate([0, 0]);

        // [[x1,y1],[x2,y2]]
        var b = path.bounds(topoData);
        // size map to 95% of minimum dimension to fill space
        var s = .95 / Math.min((b[1][0] - b[0][0]) / config.logicalSize, (b[1][1] - b[0][1]) / config.logicalSize);
        var t = [(config.logicalSize - s * (b[1][0] + b[0][0])) / 2, (config.logicalSize - s * (b[1][1] + b[0][1])) / 2];

        geoMapProjection
            .scale(s)
            .translate(t);

        bgImg = zoomPanContainer.insert("g", '#topo-G');
        bgImg.attr('id', 'map').selectAll('path')
            .data(topoData.features)
            .enter()
            .append('path')
            .attr('d', path);
    }

    function resize(view, ctx, flags) {
        setSize(svg, view);
    }


    // ==============================
    // View registration

    onos.ui.addView('topo', {
        preload: preload,
        load: load,
        unload: unload,
        resize: resize
    });

    detailPane = onos.ui.addFloatingPanel('topo-detail');
    oiBox = onos.ui.addFloatingPanel('topo-oibox', 'TL');

}(ONOS));
