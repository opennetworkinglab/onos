/*
 * Copyright 2015-present Open Networking Foundation
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
 ONOS GUI -- Topology Force Module.
 Visualization of the topology in an SVG layer, using a D3 Force Layout.
 */

(function () {
    'use strict';

    // injected refs
    var $log, $timeout, fs, sus, ts, flash, wss, tov,
        tis, tms, td3, tss, tts, tos, fltr, tls, uplink, svg, tpis;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#tfs#' + x + '#';
    };

    // configuration
    var linkConfig = {
        light: {
            baseColor: '#939598',
            inColor: '#66f',
            outColor: '#f00',
        },
        dark: {
            // TODO : theme
            baseColor: '#939598',
            inColor: '#66f',
            outColor: '#f00',
        },
        inWidth: 12,
        outWidth: 10,
    };

    // internal state
    var settings, // merged default settings and options
        force, // force layout object
        drag, // drag behavior handler
        network = {
            nodes: [],
            links: [],
            linksByDevice: {},
            lookup: {},
            revLinkToKey: {},
        },
        lu, // shorthand for lookup
        rlk, // shorthand for revLinktoKey
        showHosts = false, // whether hosts are displayed
        showOffline = true, // whether offline devices are displayed
        nodeLock = false, // whether nodes can be dragged or not (locked)
        fTimer, // timer for delayed force layout
        fNodesTimer, // timer for delayed nodes update
        fLinksTimer, // timer for delayed links update
        dim, // the dimensions of the force layout [w,h]
        linkNums = [], // array of link number labels
        devIconDim = 36, // node target dimension
        devIconDimMin = 20, // node minimum dimension when zoomed out
        devIconDimMax = 70, // node maximum dimension when zoomed in
        portLabelDim = 30;

    // SVG elements;
    var linkG, linkLabelG, numLinkLblsG, portLabelG, nodeG;

    // D3 selections;
    var link, linkLabel, node;

    // default settings for force layout
    var defaultSettings = {
        gravity: 0.4,
        friction: 0.7,
        charge: {
            // note: key is node.class
            device: -8000,
            host: -5000,
            _def_: -12000,
        },
        linkDistance: {
            // note: key is link.type
            direct: 100,
            optical: 120,
            hostLink: 3,
            _def_: 50,
        },
        linkStrength: {
            // note: key is link.type
            // range: {0.0 ... 1.0}
            // direct: 1.0,
            // optical: 1.0,
            // hostLink: 1.0,
            _def_: 1.0,
        },
    };

    var hostScaleFactor = {icon: 1.0, text: 1.0};

    // ==========================
    // === EVENT HANDLERS

    function mergeNodeData(o, n) {
        angular.extend(o, n);
        if (!n.location) {
            delete o.location;
        }
    }

    function addDevice(data) {
        var id = data.id,
            d;

        uplink.showNoDevs(false);

        // although this is an add device event, if we already have the
        //  device, treat it as an update instead..
        if (lu[id]) {
            updateDevice(data);
            return;
        }

        d = tms.createDeviceNode(data);
        network.nodes.push(d);
        lu[id] = d;
        updateNodes();
        fStart();
    }

    function updateDevice(data) {
        var id = data.id,
            d = lu[id],
            wasOnline;

        if (d) {
            wasOnline = d.online;
            mergeNodeData(d, data);
            if (tms.positionNode(d, true)) {
                sendUpdateMeta(d);
            }
            updateNodes();
            tick();
            if (wasOnline !== d.online) {
                tms.findAttachedLinks(d.id).forEach(restyleLinkElement);
                updateOfflineVisibility(d);
            }
            fStart();
        }
    }

    function removeDevice(data) {
        var id = data.id,
            d = lu[id];
        if (d) {
            removeDeviceElement(d);
        }
    }

    function addHost(data) {
        var id = data.id,
            d;

        // although this is an add host event, if we already have the
        //  host, treat it as an update instead..
        if (lu[id]) {
            updateHost(data);
            return;
        }

        d = tms.createHostNode(data);
        network.nodes.push(d);
        lu[id] = d;
        updateNodes();

        // need to handle possible multiple links (multi-homed host)
        createHostLinks(data.allCps, d);

        if (d.links.length) {
            updateLinks();
        }
        fStart();
    }

    function updateHost(data) {
        var id = data.id,
            d = lu[id];
        if (d) {
            mergeNodeData(d, data);
            if (tms.positionNode(d, true)) {
                sendUpdateMeta(d);
            }
            updateNodes();
            tick();
            fStart();
        }
    }

    function createHostLinks(cps, model) {
        model.links = [];
        cps.forEach(function (cp) {
            var linkData = {
                key: model.id + '/0-' + cp.device + '/' + cp.port,
                dst: cp.device,
                dstPort: cp.port,
            };
            model.links.push(linkData);

            // Make cell-phone devices default to wireless; others have to be annotated explicitly
            var cType = model.type === 'cellPhone' ? "wireless" : model.connectionType
            var lnk = tms.createHostLink(model.id, cp.device, cp.port, cType);
            if (lnk) {
                network.links.push(lnk);
                lu[linkData.key] = lnk;
            }
        });
    }

    function moveHost(data) {
        var id = data.id,
            d = lu[id];

        if (d) {
            removeAllLinkElements(d.links);

            // merge new data
            angular.extend(d, data);
            if (tms.positionNode(d, true)) {
                sendUpdateMeta(d);
            }

            // now create new host link(s)
            createHostLinks(data.allCps, d);

            updateNodes();
            updateLinks();
            fResume();
        }
    }

    function removeHost(data) {
        var id = data.id,
            d = lu[id];
        if (d) {
            removeHostElement(d, true);
        }
    }

    function addLink(data) {
        var result = tms.findLink(data, 'add'),
            bad = result.badLogic,
            d = result.ldata;

        if (bad) {
            $log.debug(bad + ': ' + link.id);
            return;
        }

        if (d) {
            // we already have a backing store link for src/dst nodes
            addLinkUpdate(d, data);
            return;
        }

        // no backing store link yet
        d = tms.createLink(data);
        if (d) {
            network.links.push(d);
            aggregateLink(d, data);
            lu[d.key] = d;
            updateLinks();
            fStart();
        }
    }

    function updateLink(data) {
        var result = tms.findLink(data, 'update'),
            bad = result.badLogic;
        if (bad) {
            $log.debug(bad + ': ' + link.id);
            return;
        }
        result.updateWith(data);
    }

    function removeLink(data) {
        var result = tms.findLink(data, 'remove');

        if (!result.badLogic) {
            result.removeRawLink();
        }
    }

    function topoStartDone(data) {
        // called when the initial barrage of data has been sent from server
        uplink.topoStartDone();
    }

    // ========================

    function nodeById(id) {
        return lu[id];
    }

    function makeNodeKey(node1, node2) {
        return node1 + '-' + node2;
    }

    function findNodePair(key, keyRev) {
        if (network.linksByDevice[key]) {
            return key;
        } else if (network.linksByDevice[keyRev]) {
            return keyRev;
        } else {
            return false;
        }
    }

    function aggregateLink(ldata, link) {
        var key = makeNodeKey(link.src, link.dst),
            keyRev = makeNodeKey(link.dst, link.src),
            found = findNodePair(key, keyRev);

        if (found) {
            network.linksByDevice[found].push(ldata);
            ldata.devicePair = found;
        } else {
            network.linksByDevice[key] = [ldata];
            ldata.devicePair = key;
        }
    }

    function addLinkUpdate(ldata, link) {
        // add link event, but we already have the reverse link installed
        ldata.fromTarget = link;
        rlk[link.id] = ldata.key;
        // possible solution to el being undefined in restyleLinkElement:
        // _updateLinks();
        restyleLinkElement(ldata);
    }


    var widthRatio = 1.4,
        linkScale = d3.scale.linear()
            .domain([1, 12])
            .range([widthRatio, 12 * widthRatio])
            .clamp(true),
        allLinkTypes = 'direct indirect optical tunnel',
        allLinkSubTypes = 'inactive not-permitted';

    function restyleLinkElement(ldata, immediate) {
        // this fn's job is to look at raw links and decide what svg classes
        // need to be applied to the line element in the DOM
        var th = ts.theme(),
            el = ldata.el,
            type = ldata.type(),
            connectionType = ldata.connectionType,
            lw = ldata.linkWidth(),
            online = ldata.online(),
            modeCls = ldata.expected() ? 'inactive' : 'not-permitted',
            delay = immediate ? 0 : 1000;

        // NOTE: understand why el is sometimes undefined on addLink events...
        // Investigated:
        // el is undefined when it's a reverse link that is being added.
        // updateLinks (which sets ldata.el) isn't called before this is called.
        // Calling _updateLinks in addLinkUpdate fixes it, but there might be
        // a more efficient way to fix it.
        if (el && !el.empty()) {
            el.classed('link', true);
            el.classed(allLinkSubTypes, false);
            el.classed(modeCls, !online);
            el.classed(allLinkTypes, false);
            if (type) {
                el.classed(type, true);
            }

            if (connectionType === 'wireless') {
                el.transition()
                    .duration(delay)
                    .attr('stroke-width', linkScale(lw))
                    .attr('stroke', linkConfig[th].baseColor)
                    .attr('stroke-dasharray', '3 6');
            } else {
                el.transition()
                    .duration(delay)
                    .attr('stroke-width', linkScale(lw))
                    .attr('stroke', linkConfig[th].baseColor);
            }
        }
    }

    function removeAllLinkElements(links) {
        links.forEach(function (lnk) {
            removeLinkElement(lnk);
        });
    }

    function removeLinkElement(d) {
        var idx = fs.find(d.key, network.links, 'key'),
            removed;
        if (idx >=0) {
            // remove from links array
            removed = network.links.splice(idx, 1);
            // remove from lookup cache
            delete lu[removed[0].key];
            updateLinks();
            fResume();
        }
    }

    function removeHostElement(d, upd) {
        // first, remove associated hostLink(s)...
        removeAllLinkElements(d.links);

        // remove from lookup cache
        delete lu[d.id];
        // remove from nodes array
        var idx = fs.find(d.id, network.nodes);
        network.nodes.splice(idx, 1);

        // remove from SVG
        // NOTE: upd is false if we were called from removeDeviceElement()
        if (upd) {
            updateNodes();
            fResume();
        }
    }

    function removeDeviceElement(d) {
        var id = d.id,
            idx;
        // first, remove associated hosts and links..
        tms.findAttachedHosts(id).forEach(removeHostElement);
        tms.findAttachedLinks(id).forEach(removeLinkElement);

        // remove from lookup cache
        delete lu[id];
        // remove from nodes array
        idx = fs.find(id, network.nodes);
        if (idx > -1) {
            network.nodes.splice(idx, 1);
        }

        if (!network.nodes.length) {
            uplink.showNoDevs(true);
        }

        // remove from SVG
        updateNodes();
        fResume();
    }

    function updateHostVisibility() {
        sus.visible(nodeG.selectAll('.host'), showHosts);
        sus.visible(linkG.selectAll('.hostLink'), showHosts);
        sus.visible(linkLabelG.selectAll('.hostLinkLabel'), showHosts);
    }

    function updateOfflineVisibility(dev) {
        function updDev(d, show) {
            var b;
            sus.visible(d.el, show);

            tms.findAttachedLinks(d.id).forEach(function (link) {
                b = show && ((link.type() !== 'hostLink') || showHosts);
                sus.visible(link.el, b);
            });
            tms.findAttachedHosts(d.id).forEach(function (host) {
                b = show && showHosts;
                sus.visible(host.el, b);
            });
        }

        if (dev) {
            // updating a specific device that just toggled off/on-line
            updDev(dev, dev.online || showOffline);
        } else {
            // updating all offline devices
            tms.findDevices(true).forEach(function (d) {
                updDev(d, showOffline);
            });
        }
    }


    function sendUpdateMeta(d, clearPos) {
        var metaUi = {},
            ll;

        // if we are not clearing the position data (unpinning),
        // attach the x, y, (and equivalent longitude, latitude)...
        if (!clearPos) {
            ll = tms.lngLatFromCoord([d.x, d.y]);
            metaUi = {
                x: d.x,
                y: d.y,
                equivLoc: {
                    lng: ll[0],
                    lat: ll[1],
                },
            };
        }
        d.metaUi = metaUi;
        wss.sendEvent('updateMeta', {
            id: d.id,
            class: d.class,
            memento: metaUi,
        });
    }


    function mkSvgClass(d) {
        return d.fixed ? d.svgClass + ' fixed' : d.svgClass;
    }

    function vis(b) {
        return topoLion(b ? 'visible' : 'hidden');
    }

    function toggleHosts(x) {
        var kev = (x === 'keyev'),
            on = kev ? !showHosts : !!x;

        showHosts = on;
        updateHostVisibility();
        flash.flash(topoLion('hosts') + ' ' + vis(on));
        return on;
    }

    function toggleOffline(x) {
        var kev = (x === 'keyev'),
            on = kev ? !showOffline : !!x;

        showOffline = on;
        updateOfflineVisibility();
        flash.flash(topoLion('fl_offline_devices') + ' ' + vis(on));
        return on;
    }

    function cycleDeviceLabels() {
        flash.flash(td3.incDevLabIndex());
        tms.findDevices().forEach(function (d) {
            td3.updateDeviceLabel(d);
        });
    }

    function cycleHostLabels() {
        flash.flash(td3.incHostLabIndex());
        tms.findHosts().forEach(function (d) {
            td3.updateHostLabel(d);
        });
    }

    function cycleLinkLabels() {
        td3.toggleLinkLabels();
    }

    function unpin() {
        var hov = tss.hovered();
        if (hov) {
            sendUpdateMeta(hov, true);
            hov.fixed = false;
            hov.el.classed('fixed', false);
            fResume();
        }
    }

    function showMastership(masterId) {
        if (!masterId) {
            restoreLayerState();
        } else {
            showMastershipFor(masterId);
        }
    }

    function restoreLayerState() {
        // NOTE: this level of indirection required, for when we have
        //          the layer filter functionality re-implemented
        suppressLayers(false);
    }

    function showMastershipFor(id) {
        suppressLayers(true);
        node.each(function (n) {
            if (n.master === id) {
                n.el.classed('suppressedmax', false);
            }
        });
    }

    function supAmt(less) {
        return less ? 'suppressed' : 'suppressedmax';
    }

    function suppressLayers(b, less) {
        var cls = supAmt(less);
        node.classed(cls, b);
        link.classed(cls, b);
    }

    function unsuppressNode(id, less) {
        var cls = supAmt(less);
        node.each(function (n) {
            if (n.id === id) {
                n.el.classed(cls, false);
            }
        });
    }

    function unsuppressLink(key, less) {
        var cls = supAmt(less);
        link.each(function (n) {
            if (n.key === key) {
                n.el.classed(cls, false);
            }
        });
    }

    function showBadLinks() {
        var badLinks = tms.findBadLinks();
        flash.flash(topoLion('fl_bad_links') + ': ' + badLinks.length);
        $log.debug('Bad Link List (' + badLinks.length + '):');
        badLinks.forEach(function (d) {
            $log.debug('bad link: (' + d.bad + ') ' + d.key, d);
            if (d.el) {
                d.el.attr('stroke-width', linkScale(2.8))
                    .attr('stroke', 'red');
            }
        });
        // back to normal after 2 seconds...
        $timeout(updateLinks, 2000);
    }

    function deviceScale(scaleFactor) {
        var scale = uplink.zoomer().scale() * scaleFactor,
            dim = devIconDim,
            multiplier = 1;

        if (dim * scale < devIconDimMin) {
            multiplier = devIconDimMin / (dim * scale);
        } else if (dim * scale > devIconDimMax) {
            multiplier = devIconDimMax / (dim * scale);
        }

        return multiplier;
    }

    function linkWidthScale() {
        var scale = uplink.zoomer().scale();
        return linkScale(widthRatio) / scale;
    }

    function portLabelScale() {
        var scale = uplink.zoomer().scale();
        return portLabelDim / (portLabelDim * scale);
    }

    function adjustNodeScale() {
        // Scale the network nodes
        _.each(network.nodes, function (node) {
            if (node.class === 'host') {
                node.el.selectAll('g').style('transform', 'scale(' + deviceScale(hostScaleFactor.icon) + ')');
                node.el.selectAll('text').style('transform', 'scale(' + deviceScale(hostScaleFactor.text) + ')');
                return;
            }
            node.el.selectAll('*').style('transform', 'scale(' + deviceScale(1.0) + ')');
        });

        // Scale the network links
        _.each(network.links, function (link) {
            link.el.style('stroke-width', linkWidthScale() + 'px');
        });

        d3.select('#topo-portLabels')
            .selectAll('.portLabel')
            .selectAll('*')
            .style('transform', 'scale(' + portLabelScale() + ')');
    }


    function toggleScale(scale) {
        if (scale < 0.5) {
            return 1.0
        }
        return scale - 0.2;
    }

    function toggleHostTextSize() {
        hostScaleFactor.text = toggleScale(hostScaleFactor.text);
        adjustNodeScale();
    }

    function toggleHostIconSize() {
         hostScaleFactor.icon = toggleScale(hostScaleFactor.icon);
         adjustNodeScale();
    }

    function resetAllLocations() {
        tms.resetAllLocations();
        updateNodes();
        tick(); // force nodes to be redrawn in their new locations
        flash.flash(topoLion('fl_reset_node_locations'));
    }

    // ==========================================

    function updateNodes() {
        if (fNodesTimer) {
            $timeout.cancel(fNodesTimer);
        }
        fNodesTimer = $timeout(_updateNodes, 150);
    }

    // IMPLEMENTATION NOTE: _updateNodes() should NOT stop, start, or resume
    //  the force layout; that needs to be determined and implemented elsewhere
    function _updateNodes() {
        // select all the nodes in the layout:
        node = nodeG.selectAll('.node')
            .data(network.nodes, function (d) { return d.id; });

        // operate on existing nodes:
        node.filter('.device').each(td3.deviceExisting);
        node.filter('.host').each(td3.hostExisting);

        // operate on entering nodes:
        var entering = node.enter()
            .append('g')
            .attr({
                id: function (d) { return sus.safeId(d.id); },
                class: mkSvgClass,
                transform: function (d) {
                    // Need to guard against NaN here ??
                    return sus.translate(d.x, d.y);
                },
                opacity: 0,
            })
            .call(drag)
            .on('mouseover', tss.nodeMouseOver)
            .on('mouseout', tss.nodeMouseOut)
            .transition()
            .attr('opacity', 1);

        // augment entering nodes:
        entering.filter('.device').each(td3.deviceEnter);
        entering.filter('.host').each(td3.hostEnter);

        // operate on both existing and new nodes:
        td3.updateDeviceColors();

        // operate on exiting nodes:
        // Note that the node is removed after 2 seconds.
        // Sub element animations should be shorter than 2 seconds.
        var exiting = node.exit()
            .transition()
            .duration(2000)
            .style('opacity', 0)
            .remove();

        // exiting node specifics:
        exiting.filter('.host').each(td3.hostExit);
        exiting.filter('.device').each(td3.deviceExit);
        tick();
    }

    // ==========================

    function getDefaultPos(link) {
        return {
            x1: link.source.x,
            y1: link.source.y,
            x2: link.target.x,
            y2: link.target.y,
        };
    }

    // returns amount of adjustment along the normal for given link
    function amt(numLinks, linkIdx) {
        var gap = 6;
        return (linkIdx - ((numLinks - 1) / 2)) * gap;
    }

    function calcMovement(d, amt, flipped) {
        var pos = getDefaultPos(d),
            mult = flipped ? -amt : amt,
            dx = pos.x2 - pos.x1,
            dy = pos.y2 - pos.y1,
            length = Math.sqrt((dx * dx) + (dy * dy));

        return {
            x1: pos.x1 + (mult * dy / length),
            y1: pos.y1 + (mult * -dx / length),
            x2: pos.x2 + (mult * dy / length),
            y2: pos.y2 + (mult * -dx / length),
        };
    }

    function calcPosition() {
        var lines = this,
            linkSrcId;
        linkNums = [];
        lines.each(function (d) {
            if (d.type() === 'hostLink') {
                d.position = getDefaultPos(d);
            }
        });

        function normalizeLinkSrc(link) {
            // ensure source device is consistent across set of links
            // temporary measure until link modeling is refactored
            if (!linkSrcId) {
                linkSrcId = link.source.id;
                return false;
            }

            return link.source.id !== linkSrcId;
        }

        angular.forEach(network.linksByDevice, function (linkArr, key) {
            var numLinks = linkArr.length,
                link;

            if (numLinks === 1) {
                link = linkArr[0];
                link.position = getDefaultPos(link);
                link.position.multiLink = false;
            } else if (numLinks >= 5) {
                // this code is inefficient, in the future the way links
                // are modeled will be changed
                angular.forEach(linkArr, function (link) {
                    link.position = getDefaultPos(link);
                    link.position.multiLink = true;
                });
                linkNums.push({
                    id: key,
                    num: numLinks,
                    linkCoords: linkArr[0].position,
                });
            } else {
                linkSrcId = null;
                angular.forEach(linkArr, function (link, index) {
                    var offsetAmt = amt(numLinks, index),
                        needToFlip = normalizeLinkSrc(link);
                    link.position = calcMovement(link, offsetAmt, needToFlip);
                    link.position.multiLink = false;
                });
            }
        });
    }

    function updateLinks() {
        if (fLinksTimer) {
            $timeout.cancel(fLinksTimer);
        }
        fLinksTimer = $timeout(_updateLinks, 150);
    }

    // IMPLEMENTATION NOTE: _updateLinks() should NOT stop, start, or resume
    //  the force layout; that needs to be determined and implemented elsewhere
    function _updateLinks() {
        var th = ts.theme();

        link = linkG.selectAll('.link')
            .data(network.links, function (d) { return d.key; });

        // operate on existing links:
        link.each(function (d) {
            // this is supposed to be an existing link, but we have observed
            //  occasions (where links are deleted and added rapidly?) where
            //  the DOM element has not been defined. So protect against that...
            if (d.el) {
                restyleLinkElement(d, true);
            }
        });

        // operate on entering links:
        var entering = link.enter()
            .append('line')
            .call(calcPosition)
            .attr({
                x1: function (d) { return d.position.x1; },
                y1: function (d) { return d.position.y1; },
                x2: function (d) { return d.position.x2; },
                y2: function (d) { return d.position.y2; },
                stroke: linkConfig[th].inColor,
                'stroke-width': linkConfig.inWidth,
            });

        // augment links
        entering.each(td3.linkEntering);

        // operate on both existing and new links:
        // link.each(...)

        // add labels for how many links are in a thick line
        td3.applyNumLinkLabels(linkNums, numLinkLblsG);

        // apply or remove labels
        td3.applyLinkLabels();

        // operate on exiting links:
        link.exit()
            .attr('stroke-dasharray', '3 3')
            .attr('stroke', linkConfig[th].outColor)
            .style('opacity', 0.5)
            .transition()
            .duration(1500)
            .attr({
                'stroke-dasharray': '3 12',
                'stroke-width': linkConfig.outWidth,
            })
            .style('opacity', 0.0)
            .remove();
    }


    // ==========================
    // force layout tick function

    function fResume() {
        if (!tos.isOblique()) {
            force.resume();
        }
    }

    function fStart() {
        if (!tos.isOblique()) {
            if (fTimer) {
                $timeout.cancel(fTimer);
            }
            fTimer = $timeout(function () {
                $log.debug('Starting force-layout');
                force.start();
            }, 200);
        }
    }

    var tickStuff = {
        nodeAttr: {
            transform: function (d) {
                var dx = isNaN(d.x) ? 0 : d.x,
                    dy = isNaN(d.y) ? 0 : d.y;
                return sus.translate(dx, dy);
            },
        },
        linkAttr: {
            x1: function (d) { return d.position.x1; },
            y1: function (d) { return d.position.y1; },
            x2: function (d) { return d.position.x2; },
            y2: function (d) { return d.position.y2; },
        },
        linkLabelAttr: {
            transform: function (d) {
                var lnk = tms.findLinkById(d.key);
                if (lnk) {
                    return td3.transformLabel(lnk.position, d.key);
                }
            },
        },
    };

    function tick() {
        // guard against null (which can happen when our view pages out)...
        if (node && node.size()) {
            node.attr(tickStuff.nodeAttr);
        }
        if (link && link.size()) {
            link.call(calcPosition)
                .attr(tickStuff.linkAttr);
            td3.applyNumLinkLabels(linkNums, numLinkLblsG);
        }
        if (linkLabel && linkLabel.size()) {
            linkLabel.attr(tickStuff.linkLabelAttr);
        }
    }


    // ==========================
    // === MOUSE GESTURE HANDLERS

    function zoomingOrPanning(ev) {
        return ev.metaKey || ev.altKey;
    }

    function atDragEnd(d) {
        // once we've finished moving, pin the node in position
        d.fixed = true;
        d3.select(this).classed('fixed', true);
        sendUpdateMeta(d);
        tss.clickConsumed(true);
    }

    // predicate that indicates when dragging is active
    function dragEnabled() {
        var ev = d3.event.sourceEvent;
        // nodeLock means we aren't allowing nodes to be dragged...
        return !nodeLock && !zoomingOrPanning(ev);
    }

    // predicate that indicates when clicking is active
    function clickEnabled() {
        return true;
    }

    // =============================================
    // function entry points for overlay module

    // TODO: find an automatic way of tracking via the "showHighlights" events
    var allTrafficClasses = 'primary secondary optical animated ' +
        'port-traffic-green port-traffic-yellow port-traffic-orange ' +
        'port-traffic-red';

    function clearLinkTrafficStyle() {
        link.style('stroke-width', null)
            .style('stroke', null)
            .classed(allTrafficClasses, false);
    }

    function removeLinkLabels() {
        network.links.forEach(function (d) {
            d.label = '';
        });
    }

    function clearNodeDeco() {
        node.selectAll('g.badge').remove();
    }

    function removeNodeBadges() {
        network.nodes.forEach(function (d) {
            d.badge = null;
        });
    }

    function updateLinkLabelModel() {
        // create the backing data for showing labels..
        var data = [];
        link.each(function (d) {
            if (d.label) {
                data.push({
                    id: 'lab-' + d.key,
                    key: d.key,
                    label: d.label,
                    ldata: d,
                });
            }
        });

        linkLabel = linkLabelG.selectAll('.linkLabel')
            .data(data, function (d) { return d.id; });
    }

    // ==========================
    // Module definition

    function mkModelApi(uplink) {
        return {
            projection: uplink.projection,
            network: network,
            restyleLinkElement: restyleLinkElement,
            removeLinkElement: removeLinkElement,
        };
    }

    function mkD3Api() {
        return {
            node: function () { return node; },
            link: function () { return link; },
            linkLabel: function () { return linkLabel; },
            instVisible: function () { return tis.isVisible(); },
            posNode: tms.positionNode,
            showHosts: function () { return showHosts; },
            restyleLinkElement: restyleLinkElement,
            updateLinkLabelModel: updateLinkLabelModel,
            linkConfig: function () { return linkConfig; },
            deviceScale: deviceScale,
            linkWidthScale: linkWidthScale,
        };
    }

    function mkSelectApi() {
        return {
            node: function () { return node; },
            zoomingOrPanning: zoomingOrPanning,
            updateDeviceColors: td3.updateDeviceColors,
            deselectAllLinks: tls.deselectAllLinks,
        };
    }

    function mkTrafficApi() {
        return {
            hovered: tss.hovered,
            somethingSelected: tss.somethingSelected,
            selectOrder: tss.selectOrder,
        };
    }

    function mkOverlayApi() {
        return {
            clearNodeDeco: clearNodeDeco,
            removeNodeBadges: removeNodeBadges,
            clearLinkTrafficStyle: clearLinkTrafficStyle,
            removeLinkLabels: removeLinkLabels,
            findLinkById: tms.findLinkById,
            findNodeById: nodeById,
            updateLinks: updateLinks,
            updateNodes: updateNodes,
            supLayers: suppressLayers,
            unsupNode: unsuppressNode,
            unsupLink: unsuppressLink,
        };
    }

    function mkObliqueApi(uplink, fltr) {
        return {
            force: function () { return force; },
            zoomLayer: uplink.zoomLayer,
            nodeGBBox: function () { return nodeG.node().getBBox(); },
            node: function () { return node; },
            link: function () { return link; },
            linkLabel: function () { return linkLabel; },
            nodes: function () { return network.nodes; },
            tickStuff: tickStuff,
            nodeLock: function (b) {
                var old = nodeLock;
                nodeLock = b;
                return old;
            },
            opacifyMap: uplink.opacifyMap,
            inLayer: fltr.inLayer,
            calcLinkPos: calcPosition,
            applyNumLinkLabels: function () {
                td3.applyNumLinkLabels(linkNums, numLinkLblsG);
            },
        };
    }

    function mkFilterApi() {
        return {
            node: function () { return node; },
            link: function () { return link; },
        };
    }

    function mkLinkApi(svg, uplink) {
        return {
            svg: svg,
            zoomer: uplink.zoomer(),
            network: network,
            portLabelG: function () { return portLabelG; },
            showHosts: function () { return showHosts; },
        };
    }

    function updateLinksAndNodes() {
        updateLinks();
        updateNodes();
    }

    // invoked after the localization bundle has been received from the server
    function setLionBundle(bundle) {
        topoLion = bundle;
        td3.setLionBundle(bundle);
        fltr.setLionBundle(bundle);
        tls.setLionBundle(bundle);
        tos.setLionBundle(bundle);
        tov.setLionBundle(bundle);
        tss.setLionBundle(bundle);
    }

    angular.module('ovTopo')
    .factory('TopoForceService',
        ['$log', '$timeout', 'FnService', 'SvgUtilService',
            'ThemeService', 'FlashService', 'WebSocketService',
            'TopoOverlayService', 'TopoInstService', 'TopoModelService',
            'TopoD3Service', 'TopoSelectService', 'TopoTrafficService',
            'TopoObliqueService', 'TopoFilterService', 'TopoLinkService',
            'TopoProtectedIntentsService',

        function (_$log_, _$timeout_, _fs_, _sus_, _ts_, _flash_, _wss_, _tov_,
                  _tis_, _tms_, _td3_, _tss_, _tts_, _tos_, _fltr_, _tls_, _tpis_) {
            $log = _$log_;
            $timeout = _$timeout_;
            fs = _fs_;
            sus = _sus_;
            ts = _ts_;
            flash = _flash_;
            wss = _wss_;
            tov = _tov_;
            tis = _tis_;
            tms = _tms_;
            td3 = _td3_;
            tss = _tss_;
            tts = _tts_;
            tos = _tos_;
            fltr = _fltr_;
            tls = _tls_;
            tpis = _tpis_;

            ts.addListener(updateLinksAndNodes);

            // forceG is the SVG group to display the force layout in
            // uplink is the api from the main topo source file
            // dim is the initial dimensions of the SVG as [w,h]
            // opts are, well, optional :)
            function initForce(_svg_, forceG, _uplink_, _dim_, opts) {
                uplink = _uplink_;
                dim = _dim_;
                svg = _svg_;

                lu = network.lookup;
                rlk = network.revLinkToKey;

                $log.debug('initForce().. dim = ' + dim);

                tov.setApi(mkOverlayApi(), tss);
                tms.initModel(mkModelApi(uplink), dim);
                td3.initD3(mkD3Api(), uplink.zoomer());
                tss.initSelect(mkSelectApi());
                tts.initTraffic(mkTrafficApi());
                tpis.initProtectedIntents(mkTrafficApi());
                tos.initOblique(mkObliqueApi(uplink, fltr));
                fltr.initFilter(mkFilterApi());
                tls.initLink(mkLinkApi(svg, uplink), td3);

                settings = angular.extend({}, defaultSettings, opts);

                linkG = forceG.append('g').attr('id', 'topo-links');
                linkLabelG = forceG.append('g').attr('id', 'topo-linkLabels');
                numLinkLblsG = forceG.append('g').attr('id', 'topo-numLinkLabels');
                nodeG = forceG.append('g').attr('id', 'topo-nodes');
                portLabelG = forceG.append('g').attr('id', 'topo-portLabels');

                link = linkG.selectAll('.link');
                linkLabel = linkLabelG.selectAll('.linkLabel');
                node = nodeG.selectAll('.node');

                force = d3.layout.force()
                    .size(dim)
                    .nodes(network.nodes)
                    .links(network.links)
                    .gravity(settings.gravity)
                    .friction(settings.friction)
                    .charge(settings.charge._def_)
                    .linkDistance(settings.linkDistance._def_)
                    .linkStrength(settings.linkStrength._def_)
                    .on('tick', tick);

                drag = sus.createDragBehavior(force,
                    tss.selectObject, atDragEnd, dragEnabled, clickEnabled);
            }

            function newDim(_dim_) {
                dim = _dim_;
                force.size(dim);
                tms.newDim(dim);
            }

            function destroyForce() {
                force.stop();

                tls.destroyLink();
                tos.destroyOblique();
                tts.destroyTraffic();
                tpis.destroyProtectedIntents();
                tss.destroySelect();
                td3.destroyD3();
                tms.destroyModel();
                // note: no need to destroy overlay service
                ts.removeListener(updateLinksAndNodes);

                // clean up the DOM
                svg.selectAll('g').remove();
                svg.selectAll('defs').remove();

                // clean up internal state
                network.nodes = [];
                network.links = [];
                network.linksByDevice = {};
                network.lookup = {};
                network.revLinkToKey = {};

                linkNums = [];

                linkG = linkLabelG = numLinkLblsG = nodeG = portLabelG = null;
                link = linkLabel = node = null;
                force = drag = null;

                // clean up $timeout promises
                if (fTimer) {
                    $timeout.cancel(fTimer);
                }
                if (fNodesTimer) {
                    $timeout.cancel(fNodesTimer);
                }
                if (fLinksTimer) {
                    $timeout.cancel(fLinksTimer);
                }
            }

            return {
                initForce: initForce,
                newDim: newDim,
                destroyForce: destroyForce,

                updateDeviceColors: td3.updateDeviceColors,
                toggleHosts: toggleHosts,
                togglePorts: tls.togglePorts,
                toggleOffline: toggleOffline,
                cycleDeviceLabels: cycleDeviceLabels,
                cycleHostLabels: cycleHostLabels,
                cycleLinkLabels: cycleLinkLabels,
                unpin: unpin,
                showMastership: showMastership,
                showBadLinks: showBadLinks,
                adjustNodeScale: adjustNodeScale,

                toggleHostTextSize: toggleHostTextSize,
                toggleHostIconSize: toggleHostIconSize,

                resetAllLocations: resetAllLocations,
                addDevice: addDevice,
                updateDevice: updateDevice,
                removeDevice: removeDevice,
                addHost: addHost,
                updateHost: updateHost,
                moveHost: moveHost,
                removeHost: removeHost,
                addLink: addLink,
                updateLink: updateLink,
                removeLink: removeLink,
                topoStartDone: topoStartDone,

                setLionBundle: setLionBundle,
            };
        }]);
}());
