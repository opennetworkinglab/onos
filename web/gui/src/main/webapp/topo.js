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
 @author Thomas Vachuska
 */

(function (onos) {
    'use strict';

    // shorter names for library APIs
    var d3u = onos.lib.d3util,
        gly = onos.lib.glyphs;

    // configuration data
    var config = {
        useLiveData: true,
        fnTrace: true,
        debugOn: false,
        debug: {
            showNodeXY: true,
            showKeyHandler: false
        },
        birdDim: 400,
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
            linkBaseColor: '#666',
            linkInColor: '#66f',
            linkInWidth: 12,
            linkOutColor: '#f00',
            linkOutWidth: 10
        },
        icons: {
            device: {
                dim: 36,
                rx: 4,
                xoff: -20,
                yoff: -18
            },
            host: {
                defaultRadius: 9,
                radius: {
                    endstation: 14,
                    bgpSpeaker: 14,
                    router: 14
                }
            }
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
    var layerButtons = [
            { text: 'All Layers', id: 'all', cb: showAllLayers },
            { text: 'Packet Only', id: 'pkt', cb: showPacketLayer },
            { text: 'Optical Only', id: 'opt', cb: showOpticalLayer }
        ],
        layerBtnSet,
        layerBtnDispatch = {
            all: showAllLayers,
            pkt: showPacketLayer,
            opt: showOpticalLayer
        };

    // key bindings
    var keyDispatch = {
        // TODO: remove these "development only" bindings
        0: testMe,
        equals: injectStartupEvents,
        dash: injectTestEvent,

        O: [toggleSummary, 'Toggle ONOS summary pane'],
        I: [toggleInstances, 'Toggle ONOS instances pane'],
        B: [toggleBg, 'Toggle background image'],
        H: [toggleHosts, 'Toggle host visibility'],
        L: [cycleLabels, 'Cycle Device labels'],
        P: togglePorts,
        U: [unpin, 'Unpin node (hover mouse over)'],
        R: [resetZoomPan, 'Reset zoom / pan'],
        V: [showTrafficAction, 'Show related traffic'],
        A: [showAllTrafficAction, 'Show all traffic'],
        F: [showDeviceLinkFlowsAction, 'Show device link flows'],
        esc: handleEscape
    };

    // mouse gestures
    var gestures = [
        ['click', 'Select the item and show details'],
        ['shift-click', 'Toggle selection state'],
        ['drag', 'Reposition (and pin) device / host'],
        ['cmd-scroll', 'Zoom in / out'],
        ['cmd-drag', 'Pan']
    ];

    // state variables
    var network = {
            view: null,     // view token reference
            nodes: [],
            links: [],
            lookup: {},
            revLinkToKey: {}
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
            view: null
        },
        webSock,
        sid = 0,
        deviceLabelCount = 3,
        hostLabelCount = 2,
        deviceLabelIndex = 0,
        hostLabelIndex = 0,
        selections = {},
        selectOrder = [],
        hovered = null,
        summaryPane,
        detailPane,
        antTimer = null,
        onosInstances = {},
        onosOrder = [],
        oiBox,
        oiShowMaster = false,
        portLabelsOn = false,
        cat7 = d3u.cat7(),
        colorAffinity = false,
        showHosts = false;

    // constants
    var hoverModeAll = 1,
        hoverModeFlows = 2,
        hoverModeIntents = 3,
        hoverMode = hoverModeFlows;

    // D3 selections
    var svg,
        zoomPanContainer,
        bgImg,
        topoG,
        nodeG,
        linkG,
        linkLabelG,
        node,
        link,
        linkLabel,
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
        //view.alert('Theme is ' + view.getTheme());
        //view.flash('This is some text');
        cat7.testCard(svg);
    }

    function injectTestEvent(view) {
        if (config.useLiveData) { return; }

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
                wsTrace('test', JSON.stringify(data));
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
        if (config.useLiveData) { return; }

        while (scenario.evNumber < last) {
            injectTestEvent(view);
        }
    }

    function toggleBg() {
        var vis = bgImg.style('visibility');
        bgImg.style('visibility', visVal(vis === 'hidden'));
    }

    function toggleHosts() {
        showHosts = !showHosts;
        updateHostVisibility();
        network.view.flash('Hosts ' + visVal(showHosts));
    }

    function cycleLabels() {
        deviceLabelIndex = (deviceLabelIndex === 2)
            ? 0 : deviceLabelIndex + 1;

        network.nodes.forEach(function (d) {
            if (d.class === 'device') {
                updateDeviceLabel(d);
            }
        });
    }

    function togglePorts(view) {
        //view.alert('togglePorts() callback')
    }

    function unpin() {
        if (hovered) {
            sendUpdateMeta(hovered);
            hovered.fixed = false;
            hovered.el.classed('fixed', false);
            network.force.resume();
        }
    }

    function handleEscape(view) {
        if (oiShowMaster) {
            cancelAffinity();
        } else if (detailPane.isVisible()) {
            deselectAll();
        } else if (oiBox.isVisible()) {
            hideInstances();
        } else if (summaryPane.isVisible()) {
            cancelSummary();
        }
    }

    // ==============================
    // Radio Button Callbacks

    var layerLookup = {
        host: {
            endstation: 'pkt', // default, if host event does not define type
            router:     'pkt',
            bgpSpeaker: 'pkt'
        },
        device: {
            switch: 'pkt',
            roadm: 'opt'
        },
        link: {
            hostLink: 'pkt',
            direct: 'pkt',
            indirect: '',
            tunnel: '',
            optical: 'opt'
        }
    };

    function inLayer(d, layer) {
        var type = d.class === 'link' ? d.type() : d.type,
            look = layerLookup[d.class],
            lyr = look && look[type];
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

    function suppressLayers(b) {
        node.classed('suppressed', b);
        link.classed('suppressed', b);
//        d3.selectAll('svg .port').classed('inactive', false);
//        d3.selectAll('svg .portText').classed('inactive', false);
    }

    function showAllLayers() {
        suppressLayers(false);
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

    function restoreLayerState() {
        layerBtnDispatch[layerBtnSet.selected()]();
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

    function makeNodeKey(d, what) {
        var port = what + 'Port';
        return d[what] + '/' + d[port];
    }

    function makeLinkKey(d, flipped) {
        var one = flipped ? makeNodeKey(d, 'dst') : makeNodeKey(d, 'src'),
            two = flipped ? makeNodeKey(d, 'src') : makeNodeKey(d, 'dst');
        return one + '-' + two;
    }

    function findLinkById(id) {
        // check to see if this is a reverse lookup, else default to given id
        var key = network.revLinkToKey[id] || id;
            return key && network.lookup[key];
    }

    function findLink(linkData, op) {
        var key = makeLinkKey(linkData),
            keyrev = makeLinkKey(linkData, 1),
            link = network.lookup[key],
            linkRev = network.lookup[keyrev],
            result = {},
            ldata = link || linkRev,
            rawLink;

        if (op === 'add') {
            if (link) {
                // trying to add a link that we already know about
                result.ldata = link;
                result.badLogic = 'addLink: link already added';

            } else if (linkRev) {
                // we found the reverse of the link to be added
                result.ldata = linkRev;
                if (linkRev.fromTarget) {
                    result.badLogic = 'addLink: link already added';
                }
            }
        } else if (op === 'update') {
            if (!ldata) {
                result.badLogic = 'updateLink: link not found';
            } else {
                rawLink = link ? ldata.fromSource : ldata.fromTarget;
                result.updateWith = function (data) {
                    $.extend(rawLink, data);
                    restyleLinkElement(ldata);
                }
            }
        } else if (op === 'remove') {
            if (!ldata) {
                result.badLogic = 'removeLink: link not found';
            } else {
                rawLink = link ? ldata.fromSource : ldata.fromTarget;

                if (!rawLink) {
                    result.badLogic = 'removeLink: link not found';

                } else {
                    result.removeRawLink = function () {
                        if (link) {
                            // remove fromSource
                            ldata.fromSource = null;
                            if (ldata.fromTarget) {
                                // promote target into source position
                                ldata.fromSource = ldata.fromTarget;
                                ldata.fromTarget = null;
                                ldata.key = keyrev;
                                delete network.lookup[key];
                                network.lookup[keyrev] = ldata;
                                delete network.revLinkToKey[keyrev];
                            }
                        } else {
                            // remove fromTarget
                            ldata.fromTarget = null;
                            delete network.revLinkToKey[keyrev];
                        }
                        if (ldata.fromSource) {
                            restyleLinkElement(ldata);
                        } else {
                            removeLinkElement(ldata);
                        }
                    }
                }
            }
        }
        return result;
    }

    function addLinkUpdate(ldata, link) {
        // add link event, but we already have the reverse link installed
        ldata.fromTarget = link;
        network.revLinkToKey[link.id] = ldata.key;
        restyleLinkElement(ldata);
    }

    var allLinkTypes = 'direct indirect optical tunnel',
        defaultLinkType = 'direct';

    function restyleLinkElement(ldata) {
        // this fn's job is to look at raw links and decide what svg classes
        // need to be applied to the line element in the DOM
        var el = ldata.el,
            type = ldata.type(),
            lw = ldata.linkWidth(),
            online = ldata.online();

        el.classed('link', true);
        el.classed('inactive', !online);
        el.classed(allLinkTypes, false);
        if (type) {
            el.classed(type, true);
        }
        el.transition()
            .duration(1000)
            .attr('stroke-width', linkScale(lw))
            .attr('stroke', config.topo.linkBaseColor);
    }

    // ==============================
    // Event handlers for server-pushed events

    function logicError(msg) {
        // TODO, report logic error to server, via websock, so it can be logged
        console.warn(msg);
    }

    var eventDispatch = {
        addInstance: addInstance,
        addDevice: addDevice,
        addLink: addLink,
        addHost: addHost,

        updateInstance: updateInstance,
        updateDevice: updateDevice,
        updateLink: updateLink,
        updateHost: updateHost,

        removeInstance: removeInstance,
        removeDevice: stillToImplement,
        removeLink: removeLink,
        removeHost: removeHost,

        showDetails: showDetails,
        showSummary: showSummary,
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
            result = findLink(link, 'add'),
            bad = result.badLogic,
            ldata = result.ldata;

        if (bad) {
            logicError(bad + ': ' + link.id);
            return;
        }

        if (ldata) {
            // we already have a backing store link for src/dst nodes
            addLinkUpdate(ldata, link);
            return;
        }

        // no backing store link yet
        ldata = createLink(link);
        if (ldata) {
            network.links.push(ldata);
            network.lookup[ldata.key] = ldata;
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

    function updateInstance(data) {
        evTrace(data);
        var inst = data.payload,
            id = inst.id,
            instData = onosInstances[id];
        if (instData) {
            $.extend(instData, inst);
            updateInstances();
        } else {
            logicError('updateInstance lookup fail. ID = "' + id + '"');
        }
    }

    function updateDevice(data) {
        evTrace(data);
        var device = data.payload,
            id = device.id,
            d = network.lookup[id];
        if (d) {
            $.extend(d, device);
            if (positionNode(d, true)) {
                sendUpdateMeta(d, true);
            }
            updateNodes();
        } else {
            logicError('updateDevice lookup fail. ID = "' + id + '"');
        }
    }

    function updateLink(data) {
        evTrace(data);
        var link = data.payload,
            result = findLink(link, 'update'),
            bad = result.badLogic;
        if (bad) {
            logicError(bad + ': ' + link.id);
            return;
        }
        result.updateWith(link);
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
    function removeInstance(data) {
        evTrace(data);
        var inst = data.payload,
            id = inst.id,
            instData = onosInstances[id];
        if (instData) {
            var idx = find(id, onosOrder, 'id');
            if (idx >= 0) {
                onosOrder.splice(idx, 1);
            }
            delete onosInstances[id];
            updateInstances();
        } else {
            logicError('updateInstance lookup fail. ID = "' + id + '"');
        }
    }

    function removeLink(data) {
        evTrace(data);
        var link = data.payload,
            result = findLink(link, 'remove'),
            bad = result.badLogic;
        if (bad) {
            logicError(bad + ': ' + link.id);
            return;
        }
        result.removeRawLink();
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

    function showSummary(data) {
        evTrace(data);
        populateSummary(data.payload);
        summaryPane.show();
    }

    function showDetails(data) {
        evTrace(data);
        populateDetails(data.payload);
        detailPane.show();
    }

    function showTraffic(data) {
        evTrace(data);
        var paths = data.payload.paths,
            hasTraffic = false;

        // Revert any links hilighted previously.
        link.style('stroke-width', null)
            .classed('primary secondary animated optical', false);
        // Remove all previous labels.
        removeLinkLabels();

        // Now hilight all links in the paths payload, and attach
        //  labels to them, if they are defined.
        paths.forEach(function (p) {
            var n = p.links.length,
                i,
                ldata;

            hasTraffic = hasTraffic || p.traffic;
            for (i=0; i<n; i++) {
                ldata = findLinkById(p.links[i]);
                if (ldata && ldata.el) {
                    ldata.el.classed(p.class, true);
                    ldata.label = p.labels[i];
                }
            }
        });

        updateLinks();

        if (hasTraffic && !antTimer) {
            startAntTimer();
        } else if (!hasTraffic && antTimer) {
            stopAntTimer();
        }
    }

    // ...............................

    function stillToImplement(data) {
        var p = data.payload;
        note(data.event, p.id);
        if (!config.useLiveData) {
            network.view.alert('Not yet implemented: "' + data.event + '"');
        }
    }

    function unknownEvent(data) {
        console.warn('Unknown event type: "' + data.event + '"', data);
    }

    function handleServerEvent(data) {
        var fn = eventDispatch[data.event] || unknownEvent;
        fn(data);
    }

    // ==============================
    // Out-going messages...

    function nSel() {
        return selectOrder.length;
    }
    function getSel(idx) {
        return selections[selectOrder[idx]];
    }
    function getSelId(idx) {
        return getSel(idx).obj.id;
    }
    function getSelIds(start, endOffset) {
        var end = selectOrder.length - endOffset;
        var ids = [];
        selectOrder.slice(start, end).forEach(function (d) {
            ids.push(getSelId(d));
        });
        return ids;
    }
    function allSelectionsClass(cls) {
        for (var i=0, n=nSel(); i<n; i++) {
            if (getSel(i).obj.class !== cls) {
                return false;
            }
        }
        return true;
    }

    function toggleInstances() {
        if (!oiBox.isVisible()) {
            showInstances();
        } else {
            hideInstances();
        }
    }

    function showInstances() {
        oiBox.show();
        colorAffinity = true;
        updateDeviceColors();
    }

    function hideInstances() {
        oiBox.hide();
        colorAffinity = false;
        cancelAffinity();
        updateDeviceColors();
    }


    function toggleSummary() {
        if (!summaryPane.isVisible()) {
            requestSummary();
        } else {
            cancelSummary();
        }
    }

    // request overall summary data
    function requestSummary() {
        sendMessage('requestSummary', {});
    }

    function cancelSummary() {
        sendMessage('cancelSummary', {});
        summaryPane.hide();
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

    function addHostIntentAction() {
        sendMessage('addHostIntent', {
            one: selectOrder[0],
            two: selectOrder[1],
            ids: selectOrder
        });
        network.view.flash('Host-to-Host flow added');
    }

    function addMultiSourceIntentAction() {
        sendMessage('addMultiSourceIntent', {
            src: selectOrder.slice(0, selectOrder.length - 1),
            dst: selectOrder[selectOrder.length - 1],
            ids: selectOrder
        });
        network.view.flash('Multi-Source flow added');
    }


    function cancelTraffic() {
        sendMessage('cancelTraffic', {});
    }

    function requestTrafficForMode() {
        if (hoverMode === hoverModeAll) {
            requestAllTraffic();
        } else if (hoverMode === hoverModeFlows) {
            requestDeviceLinkFlows();
        } else if (hoverMode === hoverModeIntents) {
            requestSelectTraffic();
        }
    }

    function showTrafficAction() {
        hoverMode = hoverModeIntents;
        requestSelectTraffic();
        network.view.flash('Related Traffic');
    }

    function requestSelectTraffic() {
        if (validateSelectionContext()) {
            var hoverId = (hoverMode === hoverModeIntents && hovered &&
                    (hovered.class === 'host' || hovered.class === 'device'))
                        ? hovered.id : '';
            sendMessage('requestTraffic', {
                ids: selectOrder,
                hover: hoverId
            });
        }
    }


    function showDeviceLinkFlowsAction() {
        hoverMode = hoverModeFlows;
        requestDeviceLinkFlows();
        network.view.flash('Device Flows');
    }

    function requestDeviceLinkFlows() {
        if (validateSelectionContext()) {
            var hoverId = (hoverMode === hoverModeFlows && hovered &&
                    (hovered.class === 'device')) ? hovered.id : '';
            sendMessage('requestDeviceLinkFlows', {
                ids: selectOrder,
                hover: hoverId
            });
        }
    }


    function showAllTrafficAction() {
        hoverMode = hoverModeAll;
        requestAllTraffic();
        network.view.flash('All Traffic');
    }

    function requestAllTraffic() {
        sendMessage('requestAllTraffic', {});
    }

    function validateSelectionContext() {
        if (!hovered && nSel() === 0) {
            cancelTraffic();
            return false;
        }
        return true;
    }


    // ==============================
    // onos instance panel functions

    var instCfg = {
        rectPad: 8,
        nodeOx: 9,
        nodeOy: 9,
        nodeDim: 40,
        birdOx: 19,
        birdOy: 21,
        birdDim: 21,
        uiDy: 45,
        titleDy: 30,
        textYOff: 20,
        textYSpc: 15
    };

    function viewBox(dim) {
        return '0 0 ' + dim.w + ' ' + dim.h;
    }

    function instRectAttr(dim) {
        var pad = instCfg.rectPad;
        return {
            x: pad,
            y: pad,
            width: dim.w - pad*2,
            height: dim.h - pad*2,
            rx: 6
        };
    }

    function computeDim(self) {
        var css = window.getComputedStyle(self);
        return {
            w: stripPx(css.width),
            h: stripPx(css.height)
        };
    }

    function updateInstances() {
        var onoses = oiBox.el.selectAll('.onosInst')
                .data(onosOrder, function (d) { return d.id; }),
            instDim = {w:0,h:0},
            c = instCfg;

        function nSw(n) {
            return '# Switches: ' + n;
        }

        // operate on existing onos instances if necessary
        onoses.each(function (d) {
            var el = d3.select(this),
                svg = el.select('svg');
            instDim = computeDim(this);

            // update online state
            el.classed('online', d.online);

            // update ui-attached state
            svg.select('use.uiBadge').remove();
            if (d.uiAttached) {
                attachUiBadge(svg);
            }

            function updAttr(id, value) {
                svg.select('text.instLabel.'+id).text(value);
            }

            updAttr('ip', d.ip);
            updAttr('ns', nSw(d.switches));
        });


        // operate on new onos instances
        var entering = onoses.enter()
            .append('div')
            .attr('class', 'onosInst')
            .classed('online', function (d) { return d.online; })
            .on('click', clickInst);

        entering.each(function (d) {
            var el = d3.select(this),
                rectAttr,
                svg;
            instDim = computeDim(this);
            rectAttr = instRectAttr(instDim);

            svg = el.append('svg').attr({
                width: instDim.w,
                height: instDim.h,
                viewBox: viewBox(instDim)
            });

            svg.append('rect').attr(rectAttr);

            //appendGlyph(svg, c.nodeOx, c.nodeOy, c.nodeDim, '#node');
            appendBadge(svg, 14, 14, 28, '#bird');

            if (d.uiAttached) {
                attachUiBadge(svg);
            }

            var left = c.nodeOx + c.nodeDim,
                len = rectAttr.width - left,
                hlen = len / 2,
                midline = hlen + left;

            // title
            svg.append('text')
                .attr({
                    class: 'instTitle',
                    x: midline,
                    y: c.titleDy
                })
                .text(d.id);

            // a couple of attributes
            var ty = c.titleDy + c.textYOff;

            function addAttr(id, label) {
                svg.append('text').attr({
                    class: 'instLabel ' + id,
                    x: midline,
                    y: ty
                }).text(label);
                ty += c.textYSpc;
            }

            addAttr('ip', d.ip);
            addAttr('ns', nSw(d.switches));
        });

        // operate on existing + new onoses here
        // set the affinity colors...
        onoses.each(function (d) {
            var el = d3.select(this),
                rect = el.select('svg').select('rect'),
                col = instColor(d.id, d.online);
            rect.style('fill', col);
        });

        // adjust the panel size appropriately...
        oiBox.width(instDim.w * onosOrder.length);
        oiBox.height(instDim.h);

        // remove any outgoing instances
        onoses.exit().remove();
    }

    function instColor(id, online) {
        return cat7.get(id, !online, network.view.getTheme());
    }

    function clickInst(d) {
        var el = d3.select(this),
            aff = el.classed('affinity');
        if (!aff) {
            setAffinity(el, d);
        } else {
            cancelAffinity();
        }
    }

    function setAffinity(el, d) {
        d3.selectAll('.onosInst')
            .classed('mastership', true)
            .classed('affinity', false);
        el.classed('affinity', true);

        suppressLayers(true);
        node.each(function (n) {
             if (n.master === d.id) {
                 n.el.classed('suppressed', false);
             }
        });
        oiShowMaster = true;
    }

    function cancelAffinity() {
        d3.selectAll('.onosInst')
            .classed('mastership affinity', false);
        restoreLayerState();
        oiShowMaster = false;
    }

    // TODO: these should be moved out to utility module.
    function stripPx(s) {
        return s.replace(/px$/,'');
    }

    function appendUse(svg, ox, oy, dim, iid, cls) {
        var use = svg.append('use').attr({
            transform: translate(ox,oy),
            'xlink:href': iid,
            width: dim,
            height: dim
        });
        if (cls) {
            use.classed(cls, true);
        }
        return use;
    }

    function appendGlyph(svg, ox, oy, dim, iid, cls) {
        appendUse(svg, ox, oy, dim, iid, cls).classed('glyphIcon', true);
    }

    function appendBadge(svg, ox, oy, dim, iid, cls) {
        appendUse(svg, ox, oy, dim, iid,cls ).classed('badgeIcon', true);
    }

    function attachUiBadge(svg) {
        appendBadge(svg, 12, instCfg.uiDy, 30, '#uiAttached', 'uiBadge');
    }

    function visVal(b) {
        return b ? 'visible' : 'hidden';
    }

    // ==============================
    // force layout modification functions

    function translate(x, y) {
        return 'translate(' + x + ',' + y + ')';
    }

    function rotate(deg) {
        return 'rotate(' + deg + ')';
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
            key: id,
            class: 'link',

            type: function () { return 'hostLink'; },
            // TODO: ideally, we should see if our edge switch is online...
            online: function () { return true; },
            linkWidth: function () { return 1; }
        });
        return lnk;
    }

    function createLink(link) {
        var lnk = linkEndPoints(link.src, link.dst);

        if (!lnk) {
            return null;
        }

        $.extend(lnk, {
            key: link.id,
            class: 'link',
            fromSource: link,

            // functions to aggregate dual link state
            type: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget;
                return (s && s.type) || (t && t.type) || defaultLinkType;
            },
            online: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget;
                return (s && s.online) || (t && t.online);
            },
            linkWidth: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget,
                    ws = (s && s.linkWidth) || 0,
                    wt = (t && t.linkWidth) || 0;
                return Math.max(ws, wt);
            }
        });
        return lnk;
    }

    function removeLinkLabels() {
        network.links.forEach(function (d) {
            d.label = '';
        });
    }

    function showHostVis(el) {
        el.style('visibility', visVal(showHosts));
    }

    var widthRatio = 1.4,
        linkScale = d3.scale.linear()
            .domain([1, 12])
            .range([widthRatio, 12 * widthRatio])
            .clamp(true);

    function updateLinks() {
        link = linkG.selectAll('.link')
            .data(network.links, function (d) { return d.key; });

        // operate on existing links, if necessary
        // link .foo() .bar() ...

        // operate on entering links:
        var entering = link.enter()
            .append('line')
            .attr({
                x1: function (d) { return d.x1; },
                y1: function (d) { return d.y1; },
                x2: function (d) { return d.x2; },
                y2: function (d) { return d.y2; },
                stroke: config.topo.linkInColor,
                'stroke-width': config.topo.linkInWidth
            });

        // augment links
        entering.each(function (d) {
            var link = d3.select(this);
            // provide ref to element selection from backing data....
            d.el = link;
            restyleLinkElement(d);
            if (d.type() === 'hostLink') {
                showHostVis(link);
            }
        });

        // operate on both existing and new links, if necessary
        //link .foo() .bar() ...

        // apply or remove labels
        var labelData = getLabelData();
        applyLinkLabels(labelData);

        // operate on exiting links:
        link.exit()
            .attr('stroke-dasharray', '3, 3')
            .style('opacity', 0.5)
            .transition()
            .duration(1500)
            .attr({
                'stroke-dasharray': '3, 12',
                stroke: config.topo.linkOutColor,
                'stroke-width': config.topo.linkOutWidth
            })
            .style('opacity', 0.0)
            .remove();

        // NOTE: invoke a single tick to force the labels to position
        //        onto their links.
        tick();
    }

    function getLabelData() {
        // create the backing data for showing labels..
        var data = [];
        link.each(function (d) {
            if (d.label) {
                data.push({
                    id: 'lab-' + d.key,
                    key: d.key,
                    label: d.label,
                    ldata: d
                });
            }
        });
        return data;
    }

    var linkLabelOffset = '0.3em';

    function applyLinkLabels(data) {
        var entering;

        linkLabel = linkLabelG.selectAll('.linkLabel')
            .data(data, function (d) { return d.id; });

        // for elements already existing, we need to update the text
        // and adjust the rectangle size to fit
        linkLabel.each(function (d) {
            var el = d3.select(this),
                rect = el.select('rect'),
                text = el.select('text');
            text.text(d.label);
            rect.attr(rectAroundText(el));
        });

        entering = linkLabel.enter().append('g')
            .classed('linkLabel', true)
            .attr('id', function (d) { return d.id; });

        entering.each(function (d) {
            var el = d3.select(this),
                rect,
                text,
                parms = {
                    x1: d.ldata.x1,
                    y1: d.ldata.y1,
                    x2: d.ldata.x2,
                    y2: d.ldata.y2
                };

            d.el = el;
            rect = el.append('rect');
            text = el.append('text').text(d.label);
            rect.attr(rectAroundText(el));
            text.attr('dy', linkLabelOffset);

            el.attr('transform', transformLabel(parms));
        });

        // Remove any links that are no longer required.
        linkLabel.exit().remove();
    }

    function rectAroundText(el) {
        var text = el.select('text'),
            box = text.node().getBBox();

        // translate the bbox so that it is centered on [x,y]
        box.x = -box.width / 2;
        box.y = -box.height / 2;

        // add padding
        box.x -= 1;
        box.width += 2;
        return box;
    }

    function transformLabel(p) {
        var dx = p.x2 - p.x1,
            dy = p.y2 - p.y1,
            xMid = dx/2 + p.x1,
            yMid = dy/2 + p.y1;
        return translate(xMid, yMid);
    }

    function createDeviceNode(device) {
        // start with the object as is
        var node = device,
            type = device.type,
            svgCls = type ? 'node device ' + type : 'node device',
            labels = device.labels || [];

        // Augment as needed...
        node.class = 'device';
        node.svgClass = device.online ? svgCls + ' online' : svgCls;
        positionNode(node);
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
        node.svgClass = 'node host ' + node.type;
        positionNode(node);
        return node;
    }

    function positionNode(node, forUpdate) {
        var meta = node.metaUi,
            x = meta && meta.x,
            y = meta && meta.y,
            xy;

        // If we have [x,y] already, use that...
        if (x && y) {
            node.fixed = true;
            node.px = node.x = x;
            node.py = node.y = y;
            //node.px = x;
            //node.py = y;
            return;
        }

        var location = node.location;
        if (location && location.type === 'latlng') {
            var coord = geoMapProjection([location.lng, location.lat]);
            node.fixed = true;
            node.px = node.x = coord[0];
            node.py = node.y = coord[1];
            //node.x = coord[0];
            //node.y = coord[1];
            return true;
        }

        // if this is a node update (not a node add).. skip randomizer
        if (forUpdate) {
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


    function iconGlyphUrl(d) {
        var which = d.type || 'unknown';
        return '#' + which;
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
    function trimLabel(label) {
        return (label && label.trim()) || '';
    }

    function emptyBox() {
        return {
            x: -2,
            y: -2,
            width: 4,
            height: 4
        };
    }

    function updateDeviceLabel(d) {
        var label = trimLabel(deviceLabel(d)),
            noLabel = !label,
            node = d.el,
            box,
            dx,
            dy,
            cfg = config.icons.device;

        node.select('text')
            .text(label)
            .style('opacity', 0)
            .transition()
            .style('opacity', 1);

        if (noLabel) {
            box = emptyBox();
            dx = -cfg.dim/2;
            dy = -cfg.dim/2;
        } else {
            box = adjustRectToFitText(node);
            dx = box.x + cfg.xoff;
            dy = box.y + cfg.yoff;
        }

        node.select('rect')
            .transition()
            .attr(box);

        node.select('g.deviceIcon')
            .transition()
            .attr('transform', translate(dx, dy));
    }

    function updateHostLabel(d) {
        var label = hostLabel(d),
            host = d.el;

        host.select('text').text(label);
    }

    // FIXME : fold this into updateNodes.
    function updateHostState(hostData) {
        updateHostLabel(hostData);
    }

    function updateHostVisibility() {
        var v = visVal(showHosts);
        nodeG.selectAll('.host').style('visibility', v);
        linkG.selectAll('.hostLink').style('visibility', v);
    }

    function nodeMouseOver(d) {
        hovered = d;
        requestTrafficForMode();
    }

    function nodeMouseOut(d) {
        hovered = null;
        requestTrafficForMode();
    }

    function addHostIcon(node, radius, iid) {
        var dim = radius * 1.5,
            xlate = -dim / 2;

        node.append('use').attr({
            class: 'glyphIcon hostIcon',
            transform: translate(xlate,xlate),
            'xlink:href': iid,
            width: dim,
            height: dim
        });
    }

    function updateNodes() {
        node = nodeG.selectAll('.node')
            .data(network.nodes, function (d) { return d.id; });

        // operate on existing nodes...
        node.filter('.device').each(function (d) {
            var node = d.el;
            node.classed('online', d.online);
            updateDeviceLabel(d);
            positionNode(d, true);
        });

        node.filter('.host').each(function (d) {
            var node = d.el;
            // TODO: appropriate update of host visuals
        });

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
                label = trimLabel(deviceLabel(d)),
                noLabel = !label,
                box;

            // provide ref to element from backing data....
            d.el = node;

            node.append('rect').attr({ rx: 5, ry: 5 });
            node.append('text').text(label).attr('dy', '1.1em');
            box = adjustRectToFitText(node);
            node.select('rect').attr(box);
            addDeviceIcon(node, box, noLabel, iconGlyphUrl(d));
        });


        // augment host nodes...
        entering.filter('.host').each(function (d) {
            var node = d3.select(this),
                cfg = config.icons.host,
                r = cfg.radius[d.type] || cfg.defaultRadius,
                textDy = r + 10,
                iid = iconGlyphUrl(d);

            // provide ref to element from backing data....
            d.el = node;
            showHostVis(node);

            node.append('circle').attr('r', r);
            if (iid) {
                addHostIcon(node, r, iid);
            }
            node.append('text')
                .text(hostLabel)
                .attr('dy', textDy)
                .attr('text-anchor', 'middle');
        });

        // operate on both existing and new nodes, if necessary
        updateDeviceColors();

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

        // TODO: device node exit animation

        network.force.resume();
    }

    var dCol = {
        black: '#000',
        paleblue: '#acf',
        offwhite: '#ddd',
        midgrey: '#888',
        lightgrey: '#bbb',
        orange: '#f90'
    };

    // note: these are the device icon colors without affinity
    var dColTheme = {
        light: {
            online: {
                glyph: dCol.black,
                rect: dCol.paleblue
            },
            offline: {
                glyph: dCol.midgrey,
                rect: dCol.lightgrey
            }
        },
        // TODO: theme
        dark: {
            online: {
                glyph: dCol.black,
                rect: dCol.paleblue
            },
            offline: {
                glyph: dCol.midgrey,
                rect: dCol.lightgrey
            }
        }
    };

    function devBaseColor(d) {
        var t = network.view.getTheme(),
            o = d.online ? 'online' : 'offline';
        return dColTheme[t][o];
    }

    function setDeviceColor(d) {
        var o = d.online,
            s = d.el.classed('selected'),
            c = devBaseColor(d),
            a = instColor(d.master, o),
            g, r,
            icon = d.el.select('g.deviceIcon');

        if (s) {
            g = c.glyph;
            r = dColTheme.sel;
        } else if (colorAffinity) {
            g = o ? a : c.glyph;
            r = o ? dCol.offwhite : a;
        } else {
            g = c.glyph;
            r = c.rect;
        }

        icon.select('use')
            .style('fill', g);
        icon.select('rect')
            .style('fill', r);
    }

    function addDeviceIcon(node, box, noLabel, iid) {
        var cfg = config.icons.device,
            dx,
            dy,
            g;

        if (noLabel) {
            box = emptyBox();
            dx = -cfg.dim/2;
            dy = -cfg.dim/2;
        } else {
            box = adjustRectToFitText(node);
            dx = box.x + cfg.xoff;
            dy = box.y + cfg.yoff;
        }

        g = node.append('g')
                .attr('class', 'glyphIcon deviceIcon')
                .attr('transform', translate(dx, dy));

        g.append('rect').attr({
            x: 0,
            y: 0,
            rx: cfg.rx,
            width: cfg.dim,
            height: cfg.dim
        });

        g.append('use').attr({
            'xlink:href': iid,
            width: cfg.dim,
            height: cfg.dim
        });

    }

    function find(key, array, tag) {
        var _tag = tag || 'key',
            idx, n, d;
        for (idx = 0, n = array.length; idx < n; idx++) {
            d = array[idx];
            if (d[_tag] === key) {
                return idx;
            }
        }
        return -1;
    }

    function removeLinkElement(linkData) {
        var idx = find(linkData.key, network.links),
            removed;
        if (idx >=0) {
            // remove from links array
            removed = network.links.splice(idx, 1);
            // remove from lookup cache
            delete network.lookup[removed[0].key];
            updateLinks();
            network.force.resume();
        }
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

        linkLabel.each(function (d) {
            var el = d3.select(this);
            var lnk = findLinkById(d.key);

            if (lnk) {
                var parms = {
                    x1: lnk.source.x,
                    y1: lnk.source.y,
                    x2: lnk.target.x,
                    y2: lnk.target.y
                };
                el.attr('transform', transformLabel(parms));
            }
        });
    }

    // ==============================
    // Web-Socket for live data

    function webSockUrl() {
        return document.location.toString()
            .replace(/\#.*/, '')
            .replace('http://', 'ws://')
            .replace('https://', 'wss://')
            .replace('index.html', config.webSockUrl);
    }

    webSock = {
        ws : null,

        connect : function() {
            webSock.ws = new WebSocket(webSockUrl());

            webSock.ws.onopen = function() {
                noWebSock(false);
                requestSummary();
                showInstances();
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
                console.warn('no web socket open', message);
            } else {
                console.log('WS Send: ', message);
            }
        }

    };

    function noWebSock(b) {
        mask.style('display',b ? 'block' : 'none');
    }

    function sendMessage(evType, payload) {
        var toSend = {
                event: evType,
                sid: ++sid,
                payload: payload
            },
            asText = JSON.stringify(toSend);
        wsTraceTx(asText);
        webSock.send(asText);

        // Temporary measure for debugging UI behavior ...
        if (!config.useLiveData) {
            handleTestSend(toSend);
        }
    }

    function wsTraceTx(msg) {
        wsTrace('tx', msg);
    }
    function wsTraceRx(msg) {
        wsTrace('rx', msg);
    }
    function wsTrace(rxtx, msg) {
        console.log('[' + rxtx + '] ' + msg);
    }

    // NOTE: Temporary hardcoded example for showing detail pane
    //       while we fine-
    //       Probably should not merge this change...
    function handleTestSend(msg) {
        if (msg.event === 'requestDetails') {
            showDetails({
                event: 'showDetails',
                sid: 1001,
                payload: {
                    "id": "of:0000ffffffffff09",
                    "type": "roadm",
                    "propOrder": [
                        "Name",
                        "Vendor",
                        "H/W Version",
                        "S/W Version",
                        "-",
                        "Latitude",
                        "Longitude",
                        "Ports"
                    ],
                    "props": {
                        "Name": null,
                        "Vendor": "Linc",
                        "H/W Version": "OE",
                        "S/W Version": "?",
                        "-": "",
                        "Latitude": "40.8",
                        "Longitude": "73.1",
                        "Ports": "2"
                    }
                }
            });
        }
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
        updateDeviceColors(obj);
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
            updateDeviceColors(obj.obj);
        }
    }

    function deselectAll() {
        // deselect all nodes in the network...
        node.classed('selected', false);
        selections = {};
        selectOrder = [];
        updateDeviceColors();
        updateDetailPane();
    }

    function updateDeviceColors(d) {
        if (d) {
            setDeviceColor(d);
        } else {
            node.filter('.device').each(function (d) {
                setDeviceColor(d);
            });
        }
    }

    // update the state of the detail pane, based on current selections
    function updateDetailPane() {
        var nSel = selectOrder.length;
        if (!nSel) {
            emptySelect();
        } else if (nSel === 1) {
            singleSelect();
            requestTrafficForMode();
        } else {
            multiSelect();
        }
    }

    function emptySelect() {
        detailPane.hide();
        cancelTraffic();
    }

    function singleSelect() {
        // NOTE: detail is shown from showDetails event callback
        requestDetails();
        requestTrafficForMode();
    }

    function multiSelect() {
        populateMultiSelect();
        requestTrafficForMode();
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

        var title = detailPane.append('h3'),
            table = detailPane.append('table'),
            tbody = table.append('tbody');

        title.text('Selected Nodes');

        selectOrder.forEach(function (d, i) {
            addProp(tbody, i+1, d);
        });

        addMultiSelectActions();
    }

    // TODO: refactor to consolidate with populateDetails
    function populateSummary(data) {
        summaryPane.empty();

        var svg = summaryPane.append('svg'),
            iid = iconGlyphUrl(data);

        var title = summaryPane.append('h2'),
            table = summaryPane.append('table'),
            tbody = table.append('tbody');

        appendGlyph(svg, 0, 0, 40, iid);

        svg.append('use')
            .attr({
                class: 'birdBadge',
                transform: translate(8,12),
                'xlink:href': '#bird',
                width: 24,
                height: 24,
                fill: '#fff'
            });

        title.text('ONOS Summary');

        data.propOrder.forEach(function(p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, data.props[p]);
            }
        });
    }

    function populateDetails(data) {
        detailPane.empty();

        var svg = detailPane.append('svg'),
            iid = iconGlyphUrl(data);

        var title = detailPane.append('h2'),
            table = detailPane.append('table'),
            tbody = table.append('tbody');

        appendGlyph(svg, 0, 0, 40, iid);
        title.text(data.id);

        data.propOrder.forEach(function(p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, data.props[p]);
            }
        });

        addSingleSelectActions(data);
    }

    function addSingleSelectActions(data) {
        detailPane.append('hr');
        // always want to allow 'show traffic'
        addAction(detailPane, 'Show Related Traffic', showTrafficAction);

        if (data.type === 'switch') {
            addAction(detailPane, 'Show Device Flows', showDeviceLinkFlowsAction);
        }
    }

    function addMultiSelectActions() {
        detailPane.append('hr');
        // always want to allow 'show traffic'
        addAction(detailPane, 'Show Related Traffic', showTrafficAction);
        // if exactly two hosts are selected, also want 'add host intent'
        if (nSel() === 2 && allSelectionsClass('host')) {
            addAction(detailPane, 'Create Host-to-Host Flow', addHostIntentAction);
        } else if (nSel() >= 2 && allSelectionsClass('host')) {
            addAction(detailPane, 'Create Multi-Source Flow', addMultiSourceIntentAction);
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

    //var showInstances;

/*
    function addButtonBar(view) {
        var bb = d3.select('#mast')
            .append('span').classed('right', true).attr('id', 'bb');

        function mkTogBtn(text, cb) {
            return bb.append('span')
                .classed('btn', true)
                .text(text)
                .on('click', cb);
        }

        //showInstances = mkTogBtn('Show Instances', toggleInst);
    }
*/

    function panZoom() {
        return false;
    }

    function loadGlyphs(svg) {
        var defs = svg.append('defs');
        gly.defBird(defs);
        gly.defGlyphs(defs);
        gly.defBadges(defs);
    }

    function sendUpdateMeta(d, store) {
        var metaUi = {},
            ll;

        if (store) {
            ll = geoMapProjection.invert([d.x, d.y]);
            metaUi = {
                x: d.x,
                y: d.y,
                lng: ll[0],
                lat: ll[1]
            };
        }
        d.metaUi = metaUi;
        sendMessage('updateMeta', {
            id: d.id,
            'class': d.class,
            'memento': metaUi
        });
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

        loadGlyphs(svg);

        zoomPanContainer = svg.append('g').attr('id', 'zoomPanContainer');
        setupZoomPan();

        // add blue glow filter to svg layer
        d3u.appendGlow(zoomPanContainer);

        // group for the topology
        topoG = zoomPanContainer.append('g')
            .attr('id', 'topo-G')
            .attr('transform', fcfg.translate());

        // subgroups for links, link labels, and nodes
        linkG = topoG.append('g').attr('id', 'links');
        linkLabelG = topoG.append('g').attr('id', 'linkLabels');
        nodeG = topoG.append('g').attr('id', 'nodes');

        // selection of links, linkLabels, and nodes
        link = linkG.selectAll('.link');
        linkLabel = linkLabelG.selectAll('.linkLabel');
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
                sendUpdateMeta(d, true);
            } else {
                console.log('Moving node ' + d.id + ' to [' + d.x + ',' + d.y + ']');
            }
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

        mask.append('svg')
            .attr({
                id: 'mask-bird',
                width: w,
                height: h
            })
            .append('g')
            .attr('transform', birdTranslate(w, h))
            .style('opacity', 0.3)
            .append('use')
                .attr({
                    'xlink:href': '#bird',
                    width: config.birdDim,
                    height: config.birdDim,
                    fill: '#111'
                })
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
        layerBtnSet = view.setRadio(layerButtons);
        view.setKeys(keyDispatch);
        view.setGestures(gestures);

        // patch in our "button bar" for now
        // TODO: implement a more official frameworky way of doing this..
        //addButtonBar(view);

        // Load map data asynchronously; complete startup after that..
        loadGeoJsonData();
    }

    function startAntTimer() {
        if (!antTimer) {
            var pulses = [5, 3, 1.2, 3],
                pulse = 0;
            antTimer = setInterval(function () {
                pulse = pulse + 1;
                pulse = pulse === pulses.length ? 0 : pulse;
                d3.selectAll('.animated').style('stroke-width', pulses[pulse]);
            }, 200);
        }
    }

    function stopAntTimer() {
        if (antTimer) {
            clearInterval(antTimer);
            antTimer = null;
        }
    }

    function unload(view, ctx, flags) {
        stopAntTimer();
    }

    // TODO: move these to config/state portion of script
    var geoJsonUrl = 'json/map/continental_us.json',
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
        return visVal(config.options.showBackground);
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
        var w = view.width(),
            h = view.height();

        setSize(svg, view);

        d3.select('#mask-bird').attr({ width: w, height: h})
            .select('g').attr('transform', birdTranslate(w, h));
    }

    function theme(view, ctx, flags) {
        updateInstances();
        updateDeviceColors();
    }

    function birdTranslate(w, h) {
        var bdim = config.birdDim;
        return 'translate('+((w-bdim)*.4)+','+((h-bdim)*.1)+')';
    }

    // ==============================
    // View registration

    onos.ui.addView('topo', {
        preload: preload,
        load: load,
        unload: unload,
        resize: resize,
        theme: theme
    });

    summaryPane = onos.ui.addFloatingPanel('topo-summary');
    detailPane = onos.ui.addFloatingPanel('topo-detail');
    oiBox = onos.ui.addFloatingPanel('topo-oibox', 'TL');
    oiBox.width(20);

}(ONOS));
