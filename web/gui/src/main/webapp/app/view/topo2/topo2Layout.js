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

    var $log, wss, sus, t2rs, t2d3, t2vs, t2ss;

    var linkG, linkLabelG, nodeG;
    var link, node;

    // default settings for force layout
    var defaultSettings = {
        gravity: 0.4,
        friction: 0.7,
        charge: {
            // note: key is node.class
            device: -8000,
            host: -20000,
            region: -5000,
            _def_: -12000
        },
        linkDistance: {
            // note: key is link.type
            direct: 100,
            optical: 120,
            UiEdgeLink: 30,
            _def_: 50
        },
        linkStrength: {
            // note: key is link.type
            // range: {0.0 ... 1.0}
            direct: 1.0,
            optical: 1.0,
            UiEdgeLink: 15.0,
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

    function init(_svg_, forceG, _uplink_, _dim_, opts) {

        $log.debug("Initialising Topology Layout");
        settings = angular.extend({}, defaultSettings, opts);

        linkG = forceG.append('g').attr('id', 'topo-links');
        linkLabelG = forceG.append('g').attr('id', 'topo-linkLabels');
        forceG.append('g').attr('id', 'topo-numLinkLabels');
        nodeG = forceG.append('g').attr('id', 'topo-nodes');
        forceG.append('g').attr('id', 'topo-portLabels');

        link = linkG.selectAll('.link');
        linkLabelG.selectAll('.linkLabel');
        node = nodeG.selectAll('.node');
    }

    function getDeviceChargeForType(node) {

        var nodeType = node.get('nodeType');

        return settings.charge[nodeType] ||
            settings.charge._def_;
    }

    function getLinkDistanceForLinkType(node) {
        var nodeType = node.get('type');

        return settings.linkDistance[nodeType] ||
            settings.linkDistance._def_;
    }

    function getLinkStrenghForLinkType(node) {
        var nodeType = node.get('type');

        return settings.linkStrength[nodeType] ||
            settings.linkStrength._def_;
    }

    function createForceLayout() {

        var regionLinks = t2rs.regionLinks(),
            regionNodes = t2rs.regionNodes();

        force = d3.layout.force()
            .size(t2vs.getDimensions())
            .gravity(settings.gravity)
            .friction(settings.friction)
            .charge(getDeviceChargeForType)
            .linkDistance(getLinkDistanceForLinkType)
            .linkStrength(getLinkStrenghForLinkType)
            .on("tick", tick);

        force
            .nodes(t2rs.regionNodes())
            .links(regionLinks)
            .start();

        link = linkG.selectAll('.link')
            .data(regionLinks, function (d) { return d.get('key'); });

        node = nodeG.selectAll('.node')
            .data(regionNodes, function (d) { return d.get('id'); });

        drag = sus.createDragBehavior(force,
          t2ss.selectObject, atDragEnd, dragEnabled, clickEnabled);

        update();
    }

    // predicate that indicates when clicking is active
    function clickEnabled() {
        return true;
    }

    function zoomingOrPanning(ev) {
        return ev.metaKey || ev.altKey;
    }

    function atDragEnd(d) {
        // once we've finished moving, pin the node in position
        d.fixed = true;
        d3.select(this).classed('fixed', true);
        sendUpdateMeta(d);
        $log.debug(d);
        t2ss.clickConsumed(true);
    }

    // predicate that indicates when dragging is active
    function dragEnabled() {
        var ev = d3.event.sourceEvent;
        // nodeLock means we aren't allowing nodes to be dragged...
        return !nodeLock && !zoomingOrPanning(ev);
    }

    function sendUpdateMeta(d, clearPos) {
        var metaUi = {},
            ll;

        // if we are not clearing the position data (unpinning),
        // attach the x, y, (and equivalent longitude, latitude)...
        if (!clearPos) {
            ll = d.lngLatFromCoord([d.x, d.y]);
            metaUi = {
                x: d.x,
                y: d.y,
                equivLoc: {
                    lng: ll[0],
                    lat: ll[1]
                }
            };
        }
        d.metaUi = metaUi;
        wss.sendEvent('updateMeta2', {
            id: d.get('id'),
            class: d.get('class'),
            memento: metaUi
        });
    }

    function tick() {
        link
            .attr("x1", function (d) { return d.source.x; })
            .attr("y1", function (d) { return d.source.y; })
            .attr("x2", function (d) { return d.target.x; })
            .attr("y2", function (d) { return d.target.y; });

        node
            .attr({
                transform: function (d) {
                    var dx = isNaN(d.x) ? 0 : d.x,
                        dy = isNaN(d.y) ? 0 : d.y;
                    return sus.translate(dx, dy);
                }
            });
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
            .duration(300)
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

        // operate on exiting links:
        link.exit()
            .style('opacity', 1)
            .transition()
            .duration(300)
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

    angular.module('ovTopo2')
    .factory('Topo2LayoutService',
        [
            '$log', 'WebSocketService', 'SvgUtilService', 'Topo2RegionService',
            'Topo2D3Service', 'Topo2ViewService', 'Topo2SelectService',

            function (_$log_, _wss_, _sus_, _t2rs_, _t2d3_, _t2vs_, _t2ss_) {

                $log = _$log_;
                wss = _wss_;
                t2rs = _t2rs_;
                t2d3 = _t2d3_;
                t2vs = _t2vs_;
                t2ss = _t2ss_;
                sus = _sus_;

                return {
                    init: init,
                    createForceLayout: createForceLayout,
                    update: update,
                    tick: tick,
                    start: start,

                    setDimensions: setDimensions
                };
            }
        ]
    );
})();
