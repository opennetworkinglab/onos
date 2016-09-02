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

/*
 ONOS GUI -- Topology Layout Module.
 Module that contains the d3.force.layout logic
 */

(function () {
    'use strict';

    var $log, sus, t2rs, t2d3, t2vs, t2ss;

    var uplink, linkG, linkLabelG, nodeG;
    var link, node;

    var highlightedLink;

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
            direct: 1.0,
            optical: 1.0,
            hostLink: 1.0,
            _def_: 1.0
        }
    };

    // configuration
    var linkConfig = {
        light: {
            baseColor: '#939598',
            inColor: '#66f',
            outColor: '#f00'
        },
        dark: {
            // TODO : theme
            baseColor: '#939598',
            inColor: '#66f',
            outColor: '#f00'
        },
        inWidth: 12,
        outWidth: 10
    };

    // internal state
    var settings,   // merged default settings and options
        force,      // force layout object
        drag,       // drag behavior handler
        nodeLock = false;       // whether nodes can be dragged or not (locked)

    var tickStuff = {
        nodeAttr: {
            transform: function (d) {
                var dx = isNaN(d.x) ? 0 : d.x,
                    dy = isNaN(d.y) ? 0 : d.y;
                return sus.translate(dx, dy);
            }
        },
        linkAttr: {
            x1: function (d) { return d.get('position').x1; },
            y1: function (d) { return d.get('position').y1; },
            x2: function (d) { return d.get('position').x2; },
            y2: function (d) { return d.get('position').y2; }
        }
    };

    function init(_svg_, forceG, _uplink_, _dim_, opts) {

        $log.debug("Initialising Topology Layout");
        uplink = _uplink_;
        settings = angular.extend({}, defaultSettings, opts);

        linkG = forceG.append('g').attr('id', 'topo-links');
        linkLabelG = forceG.append('g').attr('id', 'topo-linkLabels');
        forceG.append('g').attr('id', 'topo-numLinkLabels');
        nodeG = forceG.append('g').attr('id', 'topo-nodes');
        forceG.append('g').attr('id', 'topo-portLabels');

        link = linkG.selectAll('.link');
        linkLabelG.selectAll('.linkLabel');
        node = nodeG.selectAll('.node');

        _svg_.on('mousemove', mouseMoveHandler);
    }

    function createForceLayout() {

        force = d3.layout.force()
            .size(t2vs.getDimensions())
            .nodes(t2rs.regionNodes())
            .links(t2rs.regionLinks())
            .gravity(settings.gravity)
            .friction(settings.friction)
            .charge(settings.charge._def_)
            .linkDistance(settings.linkDistance._def_)
            .linkStrength(settings.linkStrength._def_)
            .on('tick', tick);

        drag = sus.createDragBehavior(force,
            t2ss.selectObject, atDragEnd, dragEnabled, clickEnabled);

        start();
        update();
    }

    function zoomingOrPanning(ev) {
        return ev.metaKey || ev.altKey;
    }

    function atDragEnd(d) {
        // once we've finished moving, pin the node in position
        d.fixed = true;
        d3.select(this).classed('fixed', true);
        // TODO: sendUpdateMeta(d);
        t2ss.clickConsumed(true);
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

    function tick() {
        // guard against null (which can happen when our view pages out)...
        if (node && node.size()) {
            node.attr(tickStuff.nodeAttr);
        }
        if (link && link.size()) {
            link.call(calcPosition)
                .attr(tickStuff.linkAttr);
            // t2d3.applyNumLinkLabels(linkNums, numLinkLabelsG);
        }
    }

    function update() {
        _updateNodes();
        _updateLinks();
    }

    function _updateNodes() {

        var regionNodes = t2rs.regionNodes();

        // select all the nodes in the layout:
        node = nodeG.selectAll('.node')
            .data(regionNodes, function (d) { return d.get('id'); });

        var entering = node.enter()
            .append('g')
            .attr({
                id: function (d) { return sus.safeId(d.get('id')); },
                class: function (d) { return d.svgClassName(); },
                transform: function (d) {
                    // Need to guard against NaN here ??
                    return sus.translate(d.node.x, d.node.y);
                },
                opacity: 0
            })
            .call(drag)
            // .on('mouseover', tss.nodeMouseOver)
            // .on('mouseout', tss.nodeMouseOut)
            .transition()
            .attr('opacity', 1);

        entering.filter('.device').each(t2d3.nodeEnter);
        entering.filter('.sub-region').each(t2d3.nodeEnter);
        entering.filter('.host').each(t2d3.hostEnter);

        // operate on exiting nodes:
        // Note that the node is removed after 2 seconds.
        // Sub element animations should be shorter than 2 seconds.
        var exiting = node.exit()
            .transition()
            .duration(2000)
            .style('opacity', 0)
            .remove();

        // exiting node specifics:
        // exiting.filter('.host').each(t2d3.hostExit);
        exiting.filter('.device').each(t2d3.nodeExit);
    }

    function _updateLinks() {

        // var th = ts.theme();
        var regionLinks = t2rs.regionLinks();

        link = linkG.selectAll('.link')
            .data(regionLinks, function (d) { return d.get('key'); });

        // operate on entering links:
        var entering = link.enter()
            .append('line')
            .call(calcPosition)
            .attr({
                x1: function (d) { return d.get('position').x1; },
                y1: function (d) { return d.get('position').y1; },
                x2: function (d) { return d.get('position').x2; },
                y2: function (d) { return d.get('position').y2; },
                stroke: linkConfig.light.inColor,
                'stroke-width': linkConfig.inWidth
            });

        entering.each(t2d3.linkEntering);

        // operate on both existing and new links:
        // link.each(...)

        // add labels for how many links are in a thick line
        // t2d3.applyNumLinkLabels(linkNums, numLinkLabelsG);

        // apply or remove labels
        // t2d3.applyLinkLabels();

        // operate on exiting links:
        link.exit()
            .attr('stroke-dasharray', '3 3')
            .attr('stroke', linkConfig.light.outColor)
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

    function calcPosition() {
        var lines = this;

        lines.each(function (d) {
            if (d.get('type') === 'hostLink') {
                d.set('position', getDefaultPos(d));
            }
        });

        lines.each(function (d) {
            d.set('position', getDefaultPos(d));
        });
    }

    function getDefaultPos(link) {

        return {
            x1: link.get('source').x,
            y1: link.get('source').y,
            x2: link.get('target').x,
            y2: link.get('target').y
        };
    }

    function setDimensions() {
        if (force) {
            force.size(t2vs.getDimensions());
        }
    }

    function start() {
        force.start();
    }

    // Mouse Events
    function mouseMoveHandler() {
        var mp = getLogicalMousePosition(this),
            link = computeNearestLink(mp);

        if (highlightedLink) {
            highlightedLink.unenhance();
            highlightedLink = null;
        }

        if (link) {
            link.enhance();
            highlightedLink = link;
        }
    }

    // ======== ALGORITHM TO FIND LINK CLOSEST TO MOUSE ========

    function getLogicalMousePosition(container) {
        var m = d3.mouse(container),
            sc = uplink.zoomer().scale(),
            tr = uplink.zoomer().translate(),
            mx = (m[0] - tr[0]) / sc,
            my = (m[1] - tr[1]) / sc;
        return { x: mx, y: my };
    }

    function sq(x) {
        return x * x;
    }

    function mdist(p, m) {
        return Math.sqrt(sq(p.x - m.x) + sq(p.y - m.y));
    }

    function prox(dist) {
        return dist / uplink.zoomer().scale();
    }

    function computeNearestLink(mouse) {
        var proximity = prox(30),
            nearest = null,
            minDist,
            regionLinks = t2rs.regionLinks();

        function pdrop(line, mouse) {

            var x1 = line.x1,
                y1 = line.y1,
                x2 = line.x2,
                y2 = line.y2,
                x3 = mouse.x,
                y3 = mouse.y,
                k = ((y2 - y1) * (x3 - x1) - (x2 - x1) * (y3 - y1)) /
                    (sq(y2 - y1) + sq(x2 - x1)),
                x4 = x3 - k * (y2 - y1),
                y4 = y3 + k * (x2 - x1);
            return { x: x4, y: y4 };
        }

        function lineHit(line, p, m) {
            if (p.x < line.x1 && p.x < line.x2) return false;
            if (p.x > line.x1 && p.x > line.x2) return false;
            if (p.y < line.y1 && p.y < line.y2) return false;
            if (p.y > line.y1 && p.y > line.y2) return false;
            // line intersects, but are we close enough?
            return mdist(p, m) <= proximity;
        }

        if (regionLinks.length) {
            minDist = proximity * 2;

            regionLinks.forEach(function (d) {
                // if (!api.showHosts() && d.type() === 'hostLink') {
                //     return; // skip hidden host links
                // }

                var line = d.get('position'),
                    point = pdrop(line, mouse),
                    hit = lineHit(line, point, mouse),
                    dist;

                if (hit) {
                    dist = mdist(point, mouse);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = d;
                    }
                }
            });
        }
        return nearest;
    }

    angular.module('ovTopo2')
    .factory('Topo2LayoutService',
        [
            '$log', 'SvgUtilService', 'Topo2RegionService',
            'Topo2D3Service', 'Topo2ViewService', 'Topo2SelectService',

            function (_$log_, _sus_, _t2rs_, _t2d3_, _t2vs_, _t2ss_) {

                $log = _$log_;
                t2rs = _t2rs_;
                t2d3 = _t2d3_;
                t2vs = _t2vs_;
                t2ss = _t2ss_;
                sus = _sus_;

                return {
                    init: init,
                    createForceLayout: createForceLayout,
                    update: update,
                    start: start,

                    setDimensions: setDimensions
                };
            }
        ]
    );
})();
