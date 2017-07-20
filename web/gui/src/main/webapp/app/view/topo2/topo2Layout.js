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

    var instance,
        updateTimer;

    // default settings for force layout
    var defaultSettings = {
        gravity: 0.4,
        friction: 0.7,
        charge: {
            // note: key is node.class
            device: -8000,
            host: -20000,
            region: -8000,
            _def_: -12000
        },
        linkDistance: {
            // note: key is link.type
            direct: 100,
            optical: 120,
            UiEdgeLink: 100,
            _def_: 50
        },
        linkStrength: {
            // note: key is link.type
            // range: {0.0 ... 1.0}
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
    var nodeLock = false;       // whether nodes can be dragged or not (locked)

    // predicate that indicates when clicking is active
    function clickEnabled() {
        return true;
    }

    angular.module('ovTopo2')
    .factory('Topo2LayoutService',
        [
            '$log', '$timeout', 'WebSocketService', 'SvgUtilService', 'Topo2RegionService',
            'Topo2ViewService', 'Topo2SelectService', 'Topo2ZoomService',
            'Topo2ViewController', 'Topo2RegionNavigationService',
            function ($log, $timeout, wss, sus, t2rs, t2vs, t2ss, t2zs,
                      ViewController, t2rns) {

                var Layout = ViewController.extend({
                    init: function (svg, forceG, uplink, dim, zoomer, opts) {
                        instance = this;

                        var navToRegion = this.navigateToRegionHandler.bind(this);
                        t2rns.addListener('region:navigation-start', navToRegion);

                        this.svg = svg;

                        // Append all the SVG Group elements to the forceG object
                        this.createForceElements();

                        this.dim = dim;
                        this.zoomer = zoomer;

                        this.settings = angular.extend({}, defaultSettings, opts);

                        this.link = this.elements.linkG.selectAll('.link');
                        this.elements.linkLabelG.selectAll('.linkLabel');
                        this.node = this.elements.nodeG.selectAll('.node');
                    },
                    createForceElements: function () {

                        this.prevForce = this.forceG;

                        this.forceG = d3.select('#topo2-zoomlayer')
                            .append('g').attr('class', 'topo2-force');

                        this.elements = {
                            linkG: this.addElement(this.forceG, 'topo2-links'),
                            linkLabelG: this.addElement(this.forceG, 'topo2-linkLabels'),
                            numLinksLabels: this.addElement(this.forceG, 'topo2-numLinkLabels'),
                            nodeG: this.addElement(this.forceG, 'topo2-nodes'),
                            portLabels: this.addElement(this.forceG, 'topo2-portLabels')
                        };
                    },
                    addElement: function (parent, className) {
                        return parent.append('g').attr('class', className);
                    },
                    settingOrDefault: function (settingName, node) {
                        var nodeType = node.get('nodeType');
                        return this.settings[settingName][nodeType] || this.settings[settingName]._def_;
                    },
                    createForceLayout: function () {

                        this.force = d3.layout.force()
                            .size(t2vs.getDimensions())
                            .gravity(this.settings.gravity)
                            .friction(this.settings.friction)
                            .charge(this.settingOrDefault.bind(this, 'charge'))
                            .linkDistance(this.settingOrDefault.bind(this, 'linkDistance'))
                            .linkStrength(this.settingOrDefault.bind(this, 'linkStrength'))
                            .nodes([])
                            .links([])
                            .on("tick", this.tick.bind(this))
                            .start();

                        this.drag = sus.createDragBehavior(this.force,
                            function () {}, // click event is no longer handled in the drag service
                            this.atDragEnd,
                            this.dragEnabled.bind(this),
                            clickEnabled,
                            t2zs
                        );

                        this.update();
                    },
                    centerLayout: function () {
                        d3.select('#topo2-zoomlayer').attr('data-layout', t2rs.model.get('id'));

                        var zoomer = d3.select('#topo2-zoomlayer').node().getBBox(),
                            layoutBBox = this.forceG.node().getBBox(),
                            scale = (zoomer.height - 150) / layoutBBox.height,
                            x = (zoomer.width / 2) - ((layoutBBox.x + layoutBBox.width / 2) * scale),
                            y = (zoomer.height / 2) - ((layoutBBox.y + layoutBBox.height / 2) * scale);

                        t2zs.panAndZoom([x, y], scale, 1000);
                    },
                    setLinkPosition: function (link) {
                        link.setPosition.bind(link)();
                    },
                    tick: function () {

                        this.node
                            .attr({
                                transform: function (d) {
                                    var dx = isNaN(d.x) ? 0 : d.x,
                                        dy = isNaN(d.y) ? 0 : d.y;
                                    return sus.translate(dx, dy);
                                }
                            });

                        this.link
                            .each(this.setLinkPosition)
                            .attr("x1", function (d) { return d.get('position').x1; })
                            .attr("y1", function (d) { return d.get('position').y1; })
                            .attr("x2", function (d) { return d.get('position').x2; })
                            .attr("y2", function (d) { return d.get('position').y2; });
                    },

                    start: function () {
                        this.force.start();
                    },
                    update: function () {

                        if (updateTimer) {
                            $timeout.cancel(updateTimer);
                        }
                        updateTimer = $timeout(this._update.bind(this), 150);
                    },
                    _update: function () {
                        this.updateNodes();
                        this.updateLinks();
                        this.force.start();
                    },
                    updateNodes: function () {
                        var regionNodes = t2rs.regionNodes();

                        // select all the nodes in the layout:
                        this.node = this.elements.nodeG.selectAll('.node')
                            .data(regionNodes, function (d) { return d.get('id'); });

                        var entering = this.node.enter()
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
                            .call(this.drag)
                            .transition()
                            .attr('opacity', 1);

                        entering.each(function (d) { d.onEnter(this, d) });

                        this.force.nodes(regionNodes);
                    },
                    updateLinks: function () {

                        var regionLinks = t2rs.regionLinks();

                        this.link = this.elements.linkG.selectAll('.link')
                            .data(regionLinks, function (d) { return d.get('id'); });

                        // operate on entering links:
                        var entering = this.link.enter()
                            .append('line')
                            .call(this.calcPosition)
                            .attr({
                                x1: function (d) { return d.get('position').x1; },
                                y1: function (d) { return d.get('position').y1; },
                                x2: function (d) { return d.get('position').x2; },
                                y2: function (d) { return d.get('position').y2; },
                                stroke: linkConfig.light.inColor,
                                'stroke-width': linkConfig.inWidth
                            });

                        entering.each(function (d) { d.onEnter(this, d) });

                        // operate on exiting links:
                        this.link.exit()
                            .attr('stroke-dasharray', '3 3')
                            .style('opacity', 0.5)
                            .transition()
                            .duration(1500)
                            .attr({
                                'stroke-dasharray': '3 12',
                            })
                            .style('opacity', 0.0)
                            .remove();

                        this.force.links(regionLinks);
                    },
                    calcPosition: function () {
                        var lines = this;
                        lines.each(function (d) {
                            d.setPosition.bind(d)();
                        });
                    },
                    sendUpdateMeta: function (d, clearPos) {
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
                    },
                    setDimensions: function () {
                        if (this.force) {
                            this.force.size(t2vs.getDimensions());
                        }
                    },
                    dragEnabled: function () {
                        var ev = d3.event.sourceEvent;
                        // nodeLock means we aren't allowing nodes to be dragged...
                        return !nodeLock && !this.zoomingOrPanning(ev);
                    },
                    zoomingOrPanning: function (ev) {
                        return ev.metaKey || ev.altKey;
                    },
                    atDragEnd: function (d) {
                        // once we've finished moving, pin the node in position
                        d.fix(true);
                        instance.sendUpdateMeta(d);
                        t2ss.clickConsumed(true);
                    },
                    transitionDownRegion: function () {

                        this.prevForce.transition()
                            .duration(1500)
                            .style('opacity', 0)
                            .remove();

                        this.forceG
                            .style('opacity', 0)
                            .transition()
                            .delay(500)
                            .duration(500)
                            .style('opacity', 1)
                            .each('end', function () {
                                t2rns.navigateToRegionComplete();
                            });
                    },
                    transitionUpRegion: function () {
                        this.prevForce.transition()
                            .duration(1000)
                            .style('opacity', 0)
                            .remove();

                        this.forceG
                            .style('opacity', 0)
                            .transition()
                            .delay(500)
                            .duration(500)
                            .style('opacity', 1)
                            .each('end', function () {
                                t2rns.navigateToRegionComplete();
                            });;
                    },
                    navigateToRegionHandler: function () {
                        this.createForceElements();
                        this.transitionDownRegion();
                    }
                });

                function getInstance(svg, forceG, uplink, dim, zoomer, opts) {
                    return instance || new Layout(svg, forceG, uplink, dim, zoomer, opts);
                }

                return getInstance();
            }
        ]
    );
})();
