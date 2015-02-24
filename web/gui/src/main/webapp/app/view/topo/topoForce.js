/*
 * Copyright 2015 Open Networking Laboratory
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
    var $log, fs, sus, is, ts, flash, tis, tms, tss, tts, tos, fltr,
        icfg, uplink;

    // configuration
    var labelConfig = {
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
        };

    var deviceIconConfig = {
         xoff: -20,
         yoff: -18
    };

    var linkConfig = {
        light: {
            baseColor: '#666',
            inColor: '#66f',
            outColor: '#f00'
        },
        dark: {
            baseColor: '#aaa',
            inColor: '#66f',
            outColor: '#f66'
        },
        inWidth: 12,
        outWidth: 10
    };

    // internal state
    var settings,   // merged default settings and options
        force,      // force layout object
        drag,       // drag behavior handler
        network = {
            nodes: [],
            links: [],
            lookup: {},
            revLinkToKey: {}
        },
        lu = network.lookup,    // shorthand
        rlk = network.revLinkToKey,
        deviceLabelIndex = 0,   // for device label cycling
        hostLabelIndex = 0,     // for host label cycling
        showHosts = false,      // whether hosts are displayed
        showOffline = true,     // whether offline devices are displayed
        nodeLock = false,       // whether nodes can be dragged or not (locked)
        dim;                    // the dimensions of the force layout [w,h]

    // SVG elements;
    var linkG, linkLabelG, nodeG;

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
            _def_: -12000
        },
        linkDistance: {
            // note: key is link.type
            direct: 100,
            optical: 120,
            hostLink: 3,
            _def_: 50
        },
        linkStrength: {
            // note: key is link.type
            // range: {0.0 ... 1.0}
            //direct: 1.0,
            //optical: 1.0,
            //hostLink: 1.0,
            _def_: 1.0
        }
    };


    // ==========================
    // === EVENT HANDLERS

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
            angular.extend(d, data);
            if (tms.positionNode(d, true)) {
                sendUpdateMeta(d);
            }
            updateNodes();
            if (wasOnline !== d.online) {
                tms.findAttachedLinks(d.id).forEach(restyleLinkElement);
                updateOfflineVisibility(d);
            }
        } else {
            // TODO: decide whether we want to capture logic errors
            //logicError('updateDevice lookup fail. ID = "' + id + '"');
        }
    }

    function removeDevice(data) {
        var id = data.id,
            d = lu[id];
        if (d) {
            removeDeviceElement(d);
        } else {
            // TODO: decide whether we want to capture logic errors
            //logicError('removeDevice lookup fail. ID = "' + id + '"');
        }
    }

    function addHost(data) {
        var id = data.id,
            d, lnk;

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

        lnk = tms.createHostLink(data);
        if (lnk) {
            d.linkData = lnk;    // cache ref on its host
            network.links.push(lnk);
            lu[d.ingress] = lnk;
            lu[d.egress] = lnk;
            updateLinks();
        }

        fStart();
    }

    function updateHost(data) {
        var id = data.id,
            d = lu[id];
        if (d) {
            angular.extend(d, data);
            if (tms.positionNode(d, true)) {
                sendUpdateMeta(d);
            }
            updateNodes();
        } else {
            // TODO: decide whether we want to capture logic errors
            //logicError('updateHost lookup fail. ID = "' + id + '"');
        }
    }

    function removeHost(data) {
        var id = data.id,
            d = lu[id];
        if (d) {
            removeHostElement(d, true);
        } else {
            // may have already removed host, if attached to removed device
            //console.warn('removeHost lookup fail. ID = "' + id + '"');
        }
    }

    function addLink(data) {
        var result = tms.findLink(data, 'add'),
            bad = result.badLogic,
            d = result.ldata;

        if (bad) {
            //logicError(bad + ': ' + link.id);
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
            lu[d.key] = d;
            updateLinks();
            fStart();
        }
    }

    function updateLink(data) {
        var result = tms.findLink(data, 'update'),
            bad = result.badLogic;
        if (bad) {
            //logicError(bad + ': ' + link.id);
            return;
        }
        result.updateWith(link);
    }

    function removeLink(data) {
        var result = tms.findLink(data, 'remove'),
            bad = result.badLogic;
        if (bad) {
            // may have already removed link, if attached to removed device
            //console.warn(bad + ': ' + link.id);
            return;
        }
        result.removeRawLink();
    }

    // ========================

    function addLinkUpdate(ldata, link) {
        // add link event, but we already have the reverse link installed
        ldata.fromTarget = link;
        rlk[link.id] = ldata.key;
        restyleLinkElement(ldata);
    }


    var widthRatio = 1.4,
        linkScale = d3.scale.linear()
            .domain([1, 12])
            .range([widthRatio, 12 * widthRatio])
            .clamp(true),
        allLinkTypes = 'direct indirect optical tunnel';

    function restyleLinkElement(ldata, immediate) {
        // this fn's job is to look at raw links and decide what svg classes
        // need to be applied to the line element in the DOM
        var th = ts.theme(),
            el = ldata.el,
            type = ldata.type(),
            lw = ldata.linkWidth(),
            online = ldata.online(),
            delay = immediate ? 0 : 1000;

        el.classed('link', true);
        el.classed('inactive', !online);
        el.classed(allLinkTypes, false);
        if (type) {
            el.classed(type, true);
        }
        el.transition()
            .duration(delay)
            .attr('stroke-width', linkScale(lw))
            .attr('stroke', linkConfig[th].baseColor);
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
        // first, remove associated hostLink...
        removeLinkElement(d.linkData);

        // remove hostLink bindings
        delete lu[d.ingress];
        delete lu[d.egress];

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
        var id = d.id;
        // first, remove associated hosts and links..
        tms.findAttachedHosts(id).forEach(removeHostElement);
        tms.findAttachedLinks(id).forEach(removeLinkElement);

        // remove from lookup cache
        delete lu[id];
        // remove from nodes array
        var idx = fs.find(id, network.nodes);
        network.nodes.splice(idx, 1);

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
        // attach the x, y, longitude, latitude...
        if (!clearPos) {
            ll = tms.lngLatFromCoord([d.x, d.y]);
            metaUi = {x: d.x, y: d.y, lng: ll[0], lat: ll[1]};
        }
        d.metaUi = metaUi;
        uplink.sendEvent('updateMeta', {
            id: d.id,
            'class': d.class,
            memento: metaUi
        });
    }


    // ==========================
    // === Devices and hosts - D3 rendering


    // Returns the newly computed bounding box of the rectangle
    function adjustRectToFitText(n) {
        var text = n.select('text'),
            box = text.node().getBBox(),
            lab = labelConfig;

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
            dim = icfg.device.dim,
            devCfg = deviceIconConfig,
            box, dx, dy;

        node.select('text')
            .text(label)
            .style('opacity', 0)
            .transition()
            .style('opacity', 1);

        if (noLabel) {
            box = emptyBox();
            dx = -dim/2;
            dy = -dim/2;
        } else {
            box = adjustRectToFitText(node);
            dx = box.x + devCfg.xoff;
            dy = box.y + devCfg.yoff;
        }

        node.select('rect')
            .transition()
            .attr(box);

        node.select('g.deviceIcon')
            .transition()
            .attr('transform', sus.translate(dx, dy));
    }

    function updateHostLabel(d) {
        var label = trimLabel(hostLabel(d));
        d.el.select('text').text(label);
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

    function vis(b) {
        return b ? 'visible' : 'hidden';
    }

    function toggleHosts() {
        showHosts = !showHosts;
        updateHostVisibility();
        flash.flash('Hosts ' + vis(showHosts));
    }

    function toggleOffline() {
        showOffline = !showOffline;
        updateOfflineVisibility();
        flash.flash('Offline devices ' + vis(showOffline));
    }

    function cycleDeviceLabels() {
        deviceLabelIndex = (deviceLabelIndex+1) % 3;
        tms.findDevices().forEach(function (d) {
            updateDeviceLabel(d);
        });
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
                n.el.classed('suppressed', false);
            }
        });
    }

    function suppressLayers(b) {
        node.classed('suppressed', b);
        link.classed('suppressed', b);
//        d3.selectAll('svg .port').classed('inactive', b);
//        d3.selectAll('svg .portText').classed('inactive', b);
    }

    // ==========================================

    var dCol = {
        black: '#000',
        paleblue: '#acf',
        offwhite: '#ddd',
        darkgrey: '#444',
        midgrey: '#888',
        lightgrey: '#bbb',
        orange: '#f90'
    };

    // note: these are the device icon colors without affinity
    var dColTheme = {
        light: {
            rfill: dCol.offwhite,
            online: {
                glyph: dCol.darkgrey,
                rect: dCol.paleblue
            },
            offline: {
                glyph: dCol.midgrey,
                rect: dCol.lightgrey
            }
        },
        dark: {
            rfill: dCol.midgrey,
            online: {
                glyph: dCol.darkgrey,
                rect: dCol.paleblue
            },
            offline: {
                glyph: dCol.midgrey,
                rect: dCol.darkgrey
            }
        }
    };

    function devBaseColor(d) {
        var o = d.online ? 'online' : 'offline';
        return dColTheme[ts.theme()][o];
    }

    function setDeviceColor(d) {
        var o = d.online,
            s = d.el.classed('selected'),
            c = devBaseColor(d),
            a = instColor(d.master, o),
            icon = d.el.select('g.deviceIcon'),
            g, r;

        if (s) {
            g = c.glyph;
            r = dCol.orange;
        } else if (tis.isVisible()) {
            g = o ? a : c.glyph;
            r = o ? c.rfill : a;
        } else {
            g = c.glyph;
            r = c.rect;
        }

        icon.select('use').style('fill', g);
        icon.select('rect').style('fill', r);
    }

    function instColor(id, online) {
        return sus.cat7().getColor(id, !online, ts.theme());
    }

    // ==========================

    function updateNodes() {
        // select all the nodes in the layout:
        node = nodeG.selectAll('.node')
            .data(network.nodes, function (d) { return d.id; });

        // operate on existing nodes:
        node.filter('.device').each(deviceExisting);
        node.filter('.host').each(hostExisting);

        // operate on entering nodes:
        var entering = node.enter()
            .append('g')
            .attr({
                id: function (d) { return sus.safeId(d.id); },
                class: mkSvgClass,
                transform: function (d) { return sus.translate(d.x, d.y); },
                opacity: 0
            })
            .call(drag)
            .on('mouseover', tss.nodeMouseOver)
            .on('mouseout', tss.nodeMouseOut)
            .transition()
            .attr('opacity', 1);

        // augment entering nodes:
        entering.filter('.device').each(deviceEnter);
        entering.filter('.host').each(hostEnter);

        // operate on both existing and new nodes:
        updateDeviceColors();

        // operate on exiting nodes:
        // Note that the node is removed after 2 seconds.
        // Sub element animations should be shorter than 2 seconds.
        var exiting = node.exit()
            .transition()
            .duration(2000)
            .style('opacity', 0)
            .remove();

        // exiting node specifics:
        exiting.filter('.host').each(hostExit);
        exiting.filter('.device').each(deviceExit);

        // finally, resume the force layout
        fResume();
    }

    // ==========================
    // updateNodes - subfunctions

    function deviceExisting(d) {
        var node = d.el;
        node.classed('online', d.online);
        updateDeviceLabel(d);
        tms.positionNode(d, true);
    }

    function hostExisting(d) {
        updateHostLabel(d);
        tms.positionNode(d, true);
    }

    function deviceEnter(d) {
        var node = d3.select(this),
            glyphId = d.type || 'unknown',
            label = trimLabel(deviceLabel(d)),
            devCfg = deviceIconConfig,
            noLabel = !label,
            box, dx, dy, icon;

        d.el = node;

        node.append('rect').attr({ rx: 5, ry: 5 });
        node.append('text').text(label).attr('dy', '1.1em');
        box = adjustRectToFitText(node);
        node.select('rect').attr(box);

        icon = is.addDeviceIcon(node, glyphId);

        if (noLabel) {
            dx = -icon.dim/2;
            dy = -icon.dim/2;
        } else {
            box = adjustRectToFitText(node);
            dx = box.x + devCfg.xoff;
            dy = box.y + devCfg.yoff;
        }

        icon.attr('transform', sus.translate(dx, dy));
    }

    function hostEnter(d) {
        var node = d3.select(this),
            gid = d.type || 'unknown',
            rad = icfg.host.radius,
            r = d.type ? rad.withGlyph : rad.noGlyph,
            textDy = r + 10;

        d.el = node;
        sus.visible(node, showHosts);

        is.addHostIcon(node, r, gid);

        node.append('text')
            .text(hostLabel)
            .attr('dy', textDy)
            .attr('text-anchor', 'middle');
    }

    function hostExit(d) {
        var node = d.el;
        node.select('use')
            .style('opacity', 0.5)
            .transition()
            .duration(800)
            .style('opacity', 0);

        node.select('text')
            .style('opacity', 0.5)
            .transition()
            .duration(800)
            .style('opacity', 0);

        node.select('circle')
            .style('stroke-fill', '#555')
            .style('fill', '#888')
            .style('opacity', 0.5)
            .transition()
            .duration(1500)
            .attr('r', 0);
    }

    function deviceExit(d) {
        var node = d.el;
        node.select('use')
            .style('opacity', 0.5)
            .transition()
            .duration(800)
            .style('opacity', 0);

        node.selectAll('rect')
            .style('stroke-fill', '#555')
            .style('fill', '#888')
            .style('opacity', 0.5);
    }

    // ==========================

    function updateLinks() {
        var th = ts.theme();

        link = linkG.selectAll('.link')
            .data(network.links, function (d) { return d.key; });

        // operate on existing links:
        link.each(linkExisting);

        // operate on entering links:
        var entering = link.enter()
            .append('line')
            .attr({
                x1: function (d) { return d.x1; },
                y1: function (d) { return d.y1; },
                x2: function (d) { return d.x2; },
                y2: function (d) { return d.y2; },
                stroke: linkConfig[th].inColor,
                'stroke-width': linkConfig.inWidth
            });

        // augment links
        entering.each(linkEntering);

        // operate on both existing and new links:
        //link.each(...)

        // apply or remove labels
        var labelData = getLabelData();
        applyLinkLabels(labelData);

        // operate on exiting links:
        link.exit()
            .attr('stroke-dasharray', '3 3')
            .attr('stroke', linkConfig[th].outColor)
            .style('opacity', 0.5)
            .transition()
            .duration(1500)
            .attr({
                'stroke-dasharray': '3 12',
                'stroke-width': linkConfig.outWidth
            })
            .style('opacity', 0.0)
            .remove();
    }

    // ==========================
    // updateLinks - subfunctions

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

    function linkExisting(d) {
        // this is supposed to be an existing link, but we have observed
        //  occasions (where links are deleted and added rapidly?) where
        //  the DOM element has not been defined. So protection against that...
        if (d.el) {
            restyleLinkElement(d, true);
        }
    }

    function linkEntering(d) {
        var link = d3.select(this);
        d.el = link;
        restyleLinkElement(d);
        if (d.type() === 'hostLink') {
            sus.visible(link, showHosts);
        }
    }

    //function linkExiting(d) { }

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
                    x1: d.ldata.source.x,
                    y1: d.ldata.source.y,
                    x2: d.ldata.target.x,
                    y2: d.ldata.target.y
                };

            if (d.ldata.type() === 'hostLink') {
                el.classed('hostLinkLabel', true);
                sus.visible(el, showHosts);
            }

            d.el = el;
            rect = el.append('rect');
            text = el.append('text').text(d.label);
            rect.attr(rectAroundText(el));
            text.attr('dy', linkLabelOffset);

            el.attr('transform', transformLabel(parms));
        });

        // Remove any labels that are no longer required.
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
        return sus.translate(xMid, yMid);
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
            force.start();
        }
    }

    var tickStuff = {
        nodeAttr: {
            transform: function (d) { return sus.translate(d.x, d.y); }
        },
        linkAttr: {
            x1: function (d) { return d.source.x; },
            y1: function (d) { return d.source.y; },
            x2: function (d) { return d.target.x; },
            y2: function (d) { return d.target.y; }
        },
        linkLabelAttr: {
            transform: function (d) {
                var lnk = tms.findLinkById(d.key);
                if (lnk) {
                    return transformLabel({
                        x1: lnk.source.x,
                        y1: lnk.source.y,
                        x2: lnk.target.x,
                        y2: lnk.target.y
                    });
                }
            }
        }
    };

    function tick() {
        node.attr(tickStuff.nodeAttr);
        link.attr(tickStuff.linkAttr);
        linkLabel.attr(tickStuff.linkLabelAttr);
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

    // ==========================
    // function entry points for traffic module

    var allTrafficClasses = 'primary secondary animated optical';

    function clearLinkTrafficStyle() {
        link.style('stroke-width', null)
            .classed(allTrafficClasses, false);
    }

    function removeLinkLabels() {
        network.links.forEach(function (d) {
            d.label = '';
        });
    }

    // ==========================
    // Module definition

    function mkModelApi(uplink) {
        return {
            projection: uplink.projection,
            network: network,
            restyleLinkElement: restyleLinkElement,
            removeLinkElement: removeLinkElement
        };
    }

    function mkSelectApi(uplink) {
        return {
            node: function () { return node; },
            zoomingOrPanning: zoomingOrPanning,
            updateDeviceColors: updateDeviceColors,
            sendEvent: uplink.sendEvent
        };
    }

    function mkTrafficApi(uplink) {
        return {
            clearLinkTrafficStyle: clearLinkTrafficStyle,
            removeLinkLabels: removeLinkLabels,
            updateLinks: updateLinks,
            findLinkById: tms.findLinkById,
            hovered: tss.hovered,
            validateSelectionContext: tss.validateSelectionContext,
            selectOrder: tss.selectOrder,
            sendEvent: uplink.sendEvent
        }
    }

    function mkObliqueApi(uplink, fltr) {
        return {
            force: function() { return force; },
            zoomLayer: uplink.zoomLayer,
            nodeGBBox: function() { return nodeG.node().getBBox(); },
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
            inLayer: fltr.inLayer
        };
    }

    function mkFilterApi(uplink) {
        return {
            node: function () { return node; },
            link: function () { return link; }
        };
    }

    angular.module('ovTopo')
    .factory('TopoForceService',
        ['$log', 'FnService', 'SvgUtilService', 'IconService', 'ThemeService',
            'FlashService', 'TopoInstService', 'TopoModelService',
            'TopoSelectService', 'TopoTrafficService',
            'TopoObliqueService', 'TopoFilterService',

        function (_$log_, _fs_, _sus_, _is_, _ts_, _flash_,
                  _tis_, _tms_, _tss_, _tts_, _tos_, _fltr_) {
            $log = _$log_;
            fs = _fs_;
            sus = _sus_;
            is = _is_;
            ts = _ts_;
            flash = _flash_;
            tis = _tis_;
            tms = _tms_;
            tss = _tss_;
            tts = _tts_;
            tos = _tos_;
            fltr = _fltr_;

            icfg = is.iconConfig();

            var themeListener = ts.addListener(function () {
                updateLinks();
                updateNodes();
            });

            // forceG is the SVG group to display the force layout in
            // uplink is the api from the main topo source file
            // dim is the initial dimensions of the SVG as [w,h]
            // opts are, well, optional :)
            function initForce(forceG, _uplink_, _dim_, opts) {
                uplink = _uplink_;
                dim = _dim_;

                $log.debug('initForce().. dim = ' + dim);

                tms.initModel(mkModelApi(uplink), dim);
                tss.initSelect(mkSelectApi(uplink));
                tts.initTraffic(mkTrafficApi(uplink));
                tos.initOblique(mkObliqueApi(uplink, fltr));
                fltr.initFilter(mkFilterApi(uplink), d3.select('#mast-right'));

                settings = angular.extend({}, defaultSettings, opts);

                linkG = forceG.append('g').attr('id', 'topo-links');
                linkLabelG = forceG.append('g').attr('id', 'topo-linkLabels');
                nodeG = forceG.append('g').attr('id', 'topo-nodes');

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
                // Review -- do we need to nudge the layout ?
            }

            function destroyForce() {
                fltr.destroyFilter();
                tos.destroyOblique();
                tts.destroyTraffic();
                tss.destroySelect();
                tms.destroyModel();
                ts.removeListener(themeListener);
                themeListener = null;
            }

            return {
                initForce: initForce,
                newDim: newDim,
                destroyForce: destroyForce,

                updateDeviceColors: updateDeviceColors,
                toggleHosts: toggleHosts,
                toggleOffline: toggleOffline,
                cycleDeviceLabels: cycleDeviceLabels,
                unpin: unpin,
                showMastership: showMastership,

                addDevice: addDevice,
                updateDevice: updateDevice,
                removeDevice: removeDevice,
                addHost: addHost,
                updateHost: updateHost,
                removeHost: removeHost,
                addLink: addLink,
                updateLink: updateLink,
                removeLink: removeLink
            };
        }]);
}());
