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
        icfg;

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


    function updateDeviceLabel(d) {
        var label = trimLabel(deviceLabel(d)),
            noLabel = !label,
            node = d.el,
            dim = icfg.device.dim,
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
        updateDeviceLabel(d);
        api.posNode(d, true);
    }

    function hostExisting(d) {
        updateHostLabel(d);
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
                text,
                parms = {
                    x1: d.ldata.source.x,
                    y1: d.ldata.source.y,
                    x2: d.ldata.target.x,
                    y2: d.ldata.target.y
                };

            if (d.ldata.type() === 'hostLink') {
                el.classed('hostLinkLabel', true);
                sus.visible(el, api.showHosts());
            }

            d.el = el;
            rect = el.append('rect');
            text = el.append('text').text(d.label);
            rect.attr(rectAroundText(el));
            text.attr('dy', linkLabelOffset);

            el.attr('transform', transformLabel(parms));
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

                updateDeviceLabel: updateDeviceLabel,
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
                applyPortLabels: applyPortLabels
            };
        }]);
}());
