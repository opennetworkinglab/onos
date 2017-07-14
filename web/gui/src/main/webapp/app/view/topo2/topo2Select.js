/*
 * Copyright 2016-present Open Networking Foundation
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
 ONOS GUI -- Topology Select Module.
 */

(function () {
    'use strict';

    var t2zs, t2ddp;

    // internal state
    var instance,
        consumeClick,
        zoomer,
        previousNearestLink; // previous link to mouse position

    function mouseClickHandler() {
        if (d3.event.defaultPrevented) return;

        if (!d3.event.shiftKey) {
            this.clearSelection();
        }

        if (!this.clickConsumed()) {
            if (previousNearestLink) {
                this.selectObject(previousNearestLink, true);
            }
        }
    }

    // Select Links
    function mouseMoveHandler(ev) {
        var mp = getLogicalMousePosition(this),
            link = computeNearestLink(mp);

        if (link) {
            if (previousNearestLink && previousNearestLink !== link) {
                previousNearestLink.unenhance();
            }
            link.enhance();
        } else if (previousNearestLink) {
            previousNearestLink.unenhance();
        }

        previousNearestLink = link;
    }

    function getLogicalMousePosition(container) {
        var m = d3.mouse(container),
            sc = zoomer.scale(),
            tr = zoomer.translate(),
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
        return dist / zoomer.scale();
    }

    function computeNearestLink(mouse) {
        var proximity = prox(30),
            nearest = null,
            minDist;

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

        var links = [];

        if (instance.region.model.get('links')) {
            links = instance.region.regionLinks();
        }

        if (links.length) {
            minDist = proximity * 2;

            links.forEach(function (d) {
                var line = d.get('position'),
                    point,
                    hit,
                    dist;

                // TODO: Reinstate when showHost() is implemented
                // if (!api.showHosts() && d.type() === 'hostLink') {
                //     return; // skip hidden host links
                // }

                if (line) {
                    point = pdrop(line, mouse);
                    hit = lineHit(line, point, mouse);
                    if (hit) {
                        dist = mdist(point, mouse);
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = d;
                        }
                    }
                }
            });
        }

        return nearest;
    }

    var SelectionService = function () {
        instance = this;
        this.selectedNodes = [];
        this.selectionOrder = [];
    };

    SelectionService.prototype = {
        init: function () {
            zoomer = t2zs.getZoomer();

            var svg = d3.select('#topo2');
            svg.on('mousemove', mouseMoveHandler);
            svg.on('click', mouseClickHandler.bind(this));
        },
        updateDetails: function () {

            var nodeCount = this.selectedNodes.length;

            if (nodeCount === 1) {
                this.selectedNodes[0].showDetails();
            } else if (nodeCount > 1) {
                t2ddp.showMulti(this.selectedNodes);
            } else {
                t2ddp.hide();
            }
        },
        selectObject: function (node, multiSelectEnabled) {

            var event = d3.event,
                nodeIndex = _.indexOf(this.selectionOrder, node.get('id'));

            if (nodeIndex < 0) {

                if (multiSelectEnabled && !event.shiftKey || !multiSelectEnabled) {
                    this.clearSelection();
                }

                this.selectedNodes.push(node);
                this.selectionOrder.push(node.get('id'));

                node.select();
            } else {
                this.removeNode(node, nodeIndex);
            }

            this.updateDetails();
        },
        removeNode: function (node, index) {
            this.selectedNodes.splice(index, 1);
            this.selectionOrder.splice(index, 1);
            node.deselect();
        },
        clearSelection: function () {
            _.each(this.selectedNodes, function (node) {
                node.deselect();
            });

            this.selectedNodes = [];
            this.selectionOrder = [];
            this.updateDetails();
        },
        clickConsumed: function (x) {
            var cc = consumeClick;
            consumeClick = Boolean(x);
            return cc;
        },
    };

    angular.module('ovTopo2')
    .factory('Topo2SelectService', [
        'Topo2ZoomService', 'Topo2DeviceDetailsPanel',
        function (_t2zs_, _t2ddp_) {
            t2zs = _t2zs_;
            t2ddp = _t2ddp_;
            return instance || new SelectionService();
        },
    ]);

})();
