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
 ONOS GUI -- Topology D3 Module.
 Functions for manipulating the D3 visualizations of the Topology
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, sus, is, ts;

    // api to topoForce
    var api;
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
    var devCfg = {
            xoff: -20,
            yoff: -18
        },
        labelConfig = {
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
        badgeConfig = {
            radius: 12,
            yoff: 5,
            gdelta: 10
        },
        icfg;

    var status = {
        i: 'badgeInfo',
        w: 'badgeWarn',
        e: 'badgeError'
    };

    function badgeStatus(badge) {
        return status[badge.status] || status.i;
    }

    // internal state
    var deviceLabelIndex = 0,
        hostLabelIndex = 0;


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
        } else if (api.instVisible()) {
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

    // ====

    function incDevLabIndex() {
        deviceLabelIndex = (deviceLabelIndex+1) % 3;
        switch(deviceLabelIndex) {
            case 0: return 'Hide device labels';
            case 1: return 'Show friendly device labels';
            case 2: return 'Show device ID labels';
        }
    }

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

    function updateDeviceRendering(d) {
        var label = trimLabel(deviceLabel(d)),
            noLabel = !label,
            node = d.el,
            dim = icfg.device.dim,
            box, dx, dy, bsel,
            bdg = d.badge,
            bcr = badgeConfig.radius,
            bcgd = badgeConfig.gdelta;

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

        // handle badge, if defined
        if (bdg) {
            renderBadge(node, bdg, { dx: dx + dim, dy: dy });
        }
    }

    function updateHostRendering(d) {
        var node = d.el,
            bdg = d.badge;

        updateHostLabel(d);

        // handle badge, if defined
        if (bdg) {
            renderBadge(node, bdg, icfg.host.badge);
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
                    'xlink:href': '#' + bdg.gid
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
            glyphId = d.type || 'unknown',
            label = trimLabel(deviceLabel(d)),
            //devCfg = deviceIconConfig,
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
        sus.visible(node, api.showHosts());

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
    // updateLinks - subfunctions

    function linkEntering(d) {
        var link = d3.select(this);
        d.el = link;
        api.restyleLinkElement(d);
        if (d.type() === 'hostLink') {
            sus.visible(link, api.showHosts());
        }
    }

    var linkLabelOffset = '0.3em';

    function applyLinkLabels() {
        var entering;

        api.updateLinkLabelModel();

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

            el.attr('transform', transformLabel(d.ldata.position));
        });

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

    function transformLabel(p) {
        var dx = p.x2 - p.x1,
            dy = p.y2 - p.y1,
            xMid = dx/2 + p.x1,
            yMid = dy/2 + p.y1;
        return sus.translate(xMid, yMid);
    }

    function applyPortLabels(data, portLabelG) {
        var entering = portLabelG.selectAll('.portLabel')
            .data(data).enter().append('g')
            .classed('portLabel', true)
            .attr('id', function (d) { return d.id; });

        entering.each(function (d) {
            var el = d3.select(this),
                rect = el.append('rect'),
                text = el.append('text').text(d.num);

            rect.attr(rectAroundText(el));
            text.attr('dy', linkLabelOffset);
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
            y: movedY
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
            transform: 'rotate(' + angle + ',' + mid.x + ',' + mid.y + ')'
        };
    }

    function textLabelPos(linkPos) {
        var point = labelPoint(linkPos),
            dist = 20;
        return {
            x: point.x + dist,
            y: point.y + dist
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
                transform: function (d) { return calcGroupPos(d.linkCoords); }
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
                id: function (d) { return 'pair-' + d.id; }
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

    // ==========================
    // Module definition

    angular.module('ovTopo')
    .factory('TopoD3Service',
        ['$log', 'FnService', 'SvgUtilService', 'IconService', 'ThemeService',

        function (_$log_, _fs_, _sus_, _is_, _ts_) {
            $log = _$log_;
            fs = _fs_;
            sus = _sus_;
            is = _is_;
            ts = _ts_;

            icfg = is.iconConfig();

            function initD3(_api_) {
                api = _api_;
            }

            function destroyD3() { }

            return {
                initD3: initD3,
                destroyD3: destroyD3,

                incDevLabIndex: incDevLabIndex,
                adjustRectToFitText: adjustRectToFitText,
                hostLabel: hostLabel,
                deviceLabel: deviceLabel,
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
                applyNumLinkLabels: applyNumLinkLabels
            };
        }]);
}());
