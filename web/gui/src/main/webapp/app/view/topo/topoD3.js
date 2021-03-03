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
 ONOS GUI -- Topology D3 Module.
 Functions for manipulating the D3 visualizations of the Topology
 */

(function () {
    'use strict';

    // injected refs
    var sus, is, ts, ps, ttbs;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#tfs#' + x + '#';
    };

    // api to topoForce
    var zoomer, api;
    /*
     node()                 // get ref to D3 selection of nodes
     link()                 // get ref to D3 selection of links
     linkLabel()            // get ref to D3 selection of link labels
     instVisible()          // true if instances panel is visible
     posNode()              // position node
     showHosts()            // true if hosts are to be shown
     restyleLinkElement()   // update link styles based on backing data
     updateLinkLabelModel() // update backing data for link labels
     */

    // configuration
    var devIconDim = 36,
        devColorDim = 32,
        labelPad = 4,
        hostRadius = 14,
        badgeConfig = {
            radius: 12,
            yoff: 5,
            gdelta: 10,
        },
        halfDevIcon = devIconDim / 2,
        devBadgeOff = { dx: -halfDevIcon, dy: -halfDevIcon },
        hostBadgeOff = { dx: -hostRadius, dy: -hostRadius },
        portLabelDim = 30,
        status = {
            i: 'badgeInfo',
            w: 'badgeWarn',
            e: 'badgeError',
        };

    // NOTE: this type of hack should go away once we have implemented
    //       the server-side UiModel code.
    // {virtual -> cord} is for the E-CORD demo at ONS 2016
    var remappedDeviceTypes = {
        virtual: 'cord',

        // for now, map to the new glyphs via this lookup.
        // may have to find a better way to do this...
        'switch': 'm_switch',
        roadm: 'm_roadm',
        otn: 'm_otn',
        ols: 'm_roadm',
        terminal_device: 'm_otn',
        roadm_otn: 'm_roadm_otn',
        fiber_switch: 'm_fiberSwitch',
        microwave: 'm_microwave',
    };

    var remappedHostTypes = {
        router: 'm_router',
        endstation: 'm_endstation',
        bgpSpeaker: 'm_bgpSpeaker',
    };

    function mapDeviceTypeToGlyph(type) {
        return remappedDeviceTypes[type] || ('m_' + type) || 'unknown';
    }

    function mapHostTypeToGlyph(type) {
        return remappedHostTypes[type] || ('m_' + type) || 'unknown';
    }

    function badgeStatus(badge) {
        return status[badge.status] || status.i;
    }

    // internal state
    var deviceLabelIndex = 0,
        hostLabelIndex = 0,
        linkLabelsEnabled = true;

    // note: these are the device icon colors without affinity (no master)
    var dColTheme = {
        light: {
            online: '#444444',
            offline: '#cccccc',
        },
        dark: {
            // TODO: theme
            online: '#444444',
            offline: '#cccccc',
        },
    };

    function devGlyphColor(d) {
        var o = d.online,
            id = d.master,
            otag = o ? 'online' : 'offline';
        return o ? sus.cat7().getColor(id, 0, ts.theme())
                 : dColTheme[ts.theme()][otag];
    }

    function setDeviceColor(d) {
        // want to color the square rectangle (no longer the 'use' glyph)
        d.el.selectAll('rect').filter(function (d, i) { return i === 1; })
            .style('fill', devGlyphColor(d));
    }

    function incDevLabIndex() {
        setDevLabIndex(deviceLabelIndex+1);
        switch (deviceLabelIndex) {
            case 0: return topoLion('fl_device_labels_hide');
            case 1: return topoLion('fl_device_labels_show_friendly');
            case 2: return topoLion('fl_device_labels_show_id');
        }
    }

    function setDevLabIndex(mode) {
        deviceLabelIndex = mode % 3;
        var p = ps.getPrefs('topo_prefs', ttbs.defaultPrefs);
        p.dlbls = deviceLabelIndex;
        ps.setPrefs('topo_prefs', p);
    }

    function incHostLabIndex() {
        setHostLabIndex(hostLabelIndex+1);
        switch (hostLabelIndex) {
            case 0: return topoLion('fl_host_labels_show_friendly');
            case 1: return topoLion('fl_host_labels_show_ip');
            case 2: return topoLion('fl_host_labels_show_mac');
            case 3: return topoLion('fl_host_labels_hide');
        }
    }

    function setHostLabIndex(mode) {
        hostLabelIndex = mode % 4;
        var p = ps.getPrefs('topo_prefs', ttbs.defaultPrefs);
        p.hlbls = hostLabelIndex;
        ps.setPrefs('topo_prefs', p);
    }

    function hostLabel(d) {
        var idx = (hostLabelIndex < d.labels.length) ? hostLabelIndex : 0;
        return d.labels[idx];
    }

    function deviceLabel(d) {
        var idx = (deviceLabelIndex < d.labels.length) ? deviceLabelIndex : 0;
        return d.labels[idx];
    }

    function toggleLinkLabels() {
        linkLabelsEnabled = !linkLabelsEnabled;
        return linkLabelsEnabled;
    }

    function trimLabel(label) {
        return (label && label.trim()) || '';
    }

    function computeLabelWidth(n) {
        var text = n.select('text'),
            box = text.node().getBBox();
        return box.width + labelPad * 2;
    }

    function iconBox(dim, labelWidth) {
        return {
            x: -dim/2,
            y: -dim/2,
            width: dim + labelWidth,
            height: dim,
        };
    }

    function updateDeviceRendering(d) {
        var node = d.el,
            bdg = d.badge,
            label = trimLabel(deviceLabel(d)),
            labelWidth;

        node.select('text').text(label);
        labelWidth = label ? computeLabelWidth(node) : 0;

        node.select('rect')
            .transition()
            .attr(iconBox(devIconDim, labelWidth));

        if (bdg) {
            renderBadge(node, bdg, devBadgeOff);
        }
    }

    function updateHostRendering(d) {
        var node = d.el,
            bdg = d.badge;

        updateHostLabel(d);

        if (bdg) {
            renderBadge(node, bdg, hostBadgeOff);
        }
    }

    function renderBadge(node, bdg, boff) {
        var bsel,
            bcr = badgeConfig.radius,
            bcgd = badgeConfig.gdelta;

        node.select('g.badge').remove();

        bsel = node.append('g')
            .classed('badge', true)
            .classed(badgeStatus(bdg), true)
            .attr('transform', sus.translate(boff.dx, boff.dy));

        bsel.append('circle')
            .attr('r', bcr);

        if (bdg.txt) {
            bsel.append('text')
                .attr('dy', badgeConfig.yoff)
                .attr('text-anchor', 'middle')
                .text(bdg.txt);
        } else if (bdg.gid) {
            bsel.append('use')
                .attr({
                    width: bcgd * 2,
                    height: bcgd * 2,
                    transform: sus.translate(-bcgd, -bcgd),
                    'xlink:href': '#' + bdg.gid,
                });
        }
    }

    function updateHostLabel(d) {
        var label = trimLabel(hostLabel(d));
        d.el.select('text').text(label);
    }

    function updateDeviceColors(d) {
        if (d) {
            setDeviceColor(d);
        } else {
            api.node().filter('.device').each(function (d) {
                setDeviceColor(d);
            });
        }
    }


    // ==========================
    // updateNodes - subfunctions

    function deviceExisting(d) {
        var node = d.el;
        node.classed('online', d.online);
        updateDeviceRendering(d);
        api.posNode(d, true);
    }

    function hostExisting(d) {
        updateHostRendering(d);
        api.posNode(d, true);
    }

    function deviceEnter(d) {
        var node = d3.select(this),
            glyphId = mapDeviceTypeToGlyph(d.type),
            label = trimLabel(deviceLabel(d)),
            rect, crect, glyph, labelWidth;

        d.el = node;

        rect = node.append('rect');
        crect = node.append('rect');

        node.append('text').text(label)
            .attr('text-anchor', 'left')
            .attr('y', '0.3em')
            .attr('x', halfDevIcon + labelPad);

        glyph = is.addDeviceIcon(node, glyphId, devIconDim);

        labelWidth = label ? computeLabelWidth(node) : 0;

        rect.attr(iconBox(devIconDim, labelWidth));
        crect.attr(iconBox(devColorDim, 0));
        glyph.attr(iconBox(devIconDim, 0));

        node.attr('transform', sus.translate(-halfDevIcon, -halfDevIcon));

        d.el.selectAll('*')
            .style('transform', 'scale(' + api.deviceScale() + ')');
    }

    function hostEnter(d) {
        var node = d3.select(this),
            glyphId = mapHostTypeToGlyph(d.type),
            textDy = hostRadius + 10;

        d.el = node;
        sus.visible(node, api.showHosts());

        is.addHostIcon(node, hostRadius, glyphId);

        node.append('text')
            .text(hostLabel)
            .attr('dy', textDy)
            .attr('text-anchor', 'middle');

        d.el.selectAll('g').style('transform', 'scale(' + api.deviceScale() + ')');
        d.el.selectAll('text').style('transform', 'scale(' + api.deviceScale() + ')');
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
    // updateLinks - subfunctions

    function linkEntering(d) {

        var link = d3.select(this);
        d.el = link;
        d.el.style('stroke-width', api.linkWidthScale() + 'px');
        api.restyleLinkElement(d);
        if (d.type() === 'hostLink') {
            sus.visible(link, api.showHosts());
        }
    }

    var linkLabelOffset = '0.3em';

    function applyLinkLabels() {
        var entering;

        api.updateLinkLabelModel();
        if (linkLabelsEnabled) {

            // for elements already existing, we need to update the text
            // and adjust the rectangle size to fit
            api.linkLabel().each(function (d) {
                var el = d3.select(this),
                    rect = el.select('rect'),
                    text = el.select('text');
                text.text(d.label);
                rect.attr(rectAroundText(el));
            });

            entering = api.linkLabel().enter().append('g')
                .classed('linkLabel', true)
                .attr('id', function (d) { return d.id; });

            entering.each(function (d) {
                var el = d3.select(this),
                    rect,
                    text;

                if (d.ldata.type() === 'hostLink') {
                    el.classed('hostLinkLabel', true);
                    sus.visible(el, api.showHosts());
                }

                d.el = el;
                rect = el.append('rect');
                text = el.append('text').text(d.label);
                rect.attr(rectAroundText(el));
                text.attr('dy', linkLabelOffset);

                el.attr('transform', transformLabel(d.ldata.position, d.key));
            });
        } else {
            api.linkLabel().each(function (d) {
                var el = d3.select(this),
                    rect = el.select('rect'),
                    text = el.select('text');
                text.text('');
                rect.attr(rectAroundText(el));
            });
        }

        // Remove any labels that are no longer required.
        api.linkLabel().exit().remove();
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

    function generateLabelFunction() {
        var labels = [],
            xGap = 15,
            yGap = 17;

        return function (newId, newX, newY) {
            var idx = -1;

            labels.forEach(function (lab, i) {
                var minX, maxX, minY, maxY;

                if (lab.id === newId) {
                    idx = i;
                    return;
                }
                minX = lab.x - xGap;
                maxX = lab.x + xGap;
                minY = lab.y - yGap;
                maxY = lab.y + yGap;

                if (newX > minX && newX < maxX && newY > minY && newY < maxY) {
                    // labels are overlapped
                    newX = newX - xGap;
                    newY = newY - yGap;
                }
            });

            if (idx === -1) {
                labels.push({ id: newId, x: newX, y: newY });
            } else {
                labels[idx] = { id: newId, x: newX, y: newY };
            }

            return { x: newX, y: newY };
        };
    }

    var getLabelPos = generateLabelFunction();

    function transformLabel(p, id) {
        var dx = p.x2 - p.x1,
            dy = p.y2 - p.y1,
            xMid = dx/2 + p.x1,
            yMid = dy/2 + p.y1;

        if (id) {
            var pos = getLabelPos(id, xMid, yMid);
            return sus.translate(pos.x, pos.y);
        }

        return sus.translate(xMid, yMid);
    }

    function applyPortLabels(data, portLabelG) {
        var entering = portLabelG.selectAll('.portLabel')
            .data(data).enter().append('g')
            .classed('portLabel', true)
            .attr('id', function (d) { return d.id; });

        var labelScale = portLabelDim / (portLabelDim * zoomer.scale());

        entering.each(function (d) {
            var el = d3.select(this),
                rect = el.append('rect'),
                text = el.append('text').text(d.num);

            rect.attr(rectAroundText(el))
                .style('transform', 'scale(' + labelScale + ')');
            text.attr('dy', linkLabelOffset)
                .style('transform', 'scale(' + labelScale + ')');

            el.attr('transform', sus.translate(d.x, d.y));
        });
    }

    function labelPoint(linkPos) {
        var lengthUpLine = 1 / 3,
            dx = linkPos.x2 - linkPos.x1,
            dy = linkPos.y2 - linkPos.y1,
            movedX = dx * lengthUpLine,
            movedY = dy * lengthUpLine;

        return {
            x: movedX,
            y: movedY,
        };
    }

    function calcGroupPos(linkPos) {
        var moved = labelPoint(linkPos);
        return sus.translate(linkPos.x1 + moved.x, linkPos.y1 + moved.y);
    }

    // calculates where on the link that the hash line for 5+ label appears
    function hashAttrs(linkPos) {
        var hashLength = 25,
            halfLength = hashLength / 2,
            dx = linkPos.x2 - linkPos.x1,
            dy = linkPos.y2 - linkPos.y1,
            length = Math.sqrt((dx * dx) + (dy * dy)),
            moveAmtX = (dx / length) * halfLength,
            moveAmtY = (dy / length) * halfLength,
            mid = labelPoint(linkPos),
            angle = Math.atan(dy / dx) + 45;

        return {
            x1: mid.x - moveAmtX,
            y1: mid.y - moveAmtY,
            x2: mid.x + moveAmtX,
            y2: mid.y + moveAmtY,
            stroke: api.linkConfig()[ts.theme()].baseColor,
            transform: 'rotate(' + angle + ',' + mid.x + ',' + mid.y + ')',
        };
    }

    function textLabelPos(linkPos) {
        var point = labelPoint(linkPos),
            dist = 20;
        return {
            x: point.x + dist,
            y: point.y + dist,
        };
    }

    function applyNumLinkLabels(data, lblsG) {
        var labels = lblsG.selectAll('g.numLinkLabel')
                .data(data, function (d) { return 'pair-' + d.id; }),
            entering;

        // update existing labels
        labels.each(function (d) {
            var el = d3.select(this);

            el.attr({
                transform: function (d) { return calcGroupPos(d.linkCoords); },
            });
            el.select('line')
                .attr(hashAttrs(d.linkCoords));
            el.select('text')
                .attr(textLabelPos(d.linkCoords))
                .text(d.num);
        });

        // add new labels
        entering = labels
            .enter()
            .append('g')
            .attr({
                transform: function (d) { return calcGroupPos(d.linkCoords); },
                id: function (d) { return 'pair-' + d.id; },
            })
            .classed('numLinkLabel', true);

        entering.each(function (d) {
            var el = d3.select(this);

            el.append('line')
                .classed('numLinkHash', true)
                .attr(hashAttrs(d.linkCoords));
            el.append('text')
                .classed('numLinkText', true)
                .attr(textLabelPos(d.linkCoords))
                .text(d.num);
        });

        // remove old labels
        labels.exit().remove();
    }

    // invoked after the localization bundle has been received from the server
    function setLionBundle(bundle) {
        topoLion = bundle;
    }

    // ==========================
    // Module definition

    angular.module('ovTopo')
    .factory('TopoD3Service',
        ['SvgUtilService', 'IconService', 'ThemeService',
            'PrefsService', 'TopoToolbarService',

        function (_sus_, _is_, _ts_, _ps_, _ttbs_) {
            sus = _sus_;
            is = _is_;
            ts = _ts_;
            ps = _ps_;
            ttbs = _ttbs_;

            function initD3(_api_, _zoomer_) {
                api = _api_;
                zoomer = _zoomer_;
            }

            function destroyD3() { }

            return {
                initD3: initD3,
                destroyD3: destroyD3,

                incDevLabIndex: incDevLabIndex,
                setDevLabIndex: setDevLabIndex,
                incHostLabIndex: incHostLabIndex,
                setHostLabIndex: setHostLabIndex,
                hostLabel: hostLabel,
                deviceLabel: deviceLabel,
                toggleLinkLabels: toggleLinkLabels,
                trimLabel: trimLabel,

                updateDeviceLabel: updateDeviceRendering,
                updateHostLabel: updateHostLabel,
                updateDeviceColors: updateDeviceColors,

                deviceExisting: deviceExisting,
                hostExisting: hostExisting,
                deviceEnter: deviceEnter,
                hostEnter: hostEnter,
                hostExit: hostExit,
                deviceExit: deviceExit,

                linkEntering: linkEntering,
                applyLinkLabels: applyLinkLabels,
                transformLabel: transformLabel,
                applyPortLabels: applyPortLabels,
                applyNumLinkLabels: applyNumLinkLabels,

                setLionBundle: setLionBundle,
            };
        }]);
}());
