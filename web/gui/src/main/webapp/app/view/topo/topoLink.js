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
 ONOS GUI -- Topology Link Module.
 Functions for highlighting/selecting links
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, sus, ts, flash;

    var api,
        td3,
        network,
        enhancedLink = null;    // the link which the mouse is hovering over

    // SVG elements;
    var svg;

    // internal state
    var showPorts = true;       // enable port highlighting by default


    // ======== ALGORITHM TO FIND LINK CLOSEST TO MOUSE ========

    function mouseMoveHandler() {
        var m = d3.mouse(this),
            sc = api.zoomer.scale(),
            tr = api.zoomer.translate(),
            mx = (m[0] - tr[0]) / sc,
            my = (m[1] - tr[1]) / sc;
        computeNearestLink({x: mx, y: my});
    }

    function computeNearestLink(mouse) {
        var proximity = 30 / api.zoomer.scale(),
            nearest, minDist;

        function sq(x) { return x * x; }

        function mdist(p, m) {
            return Math.sqrt(sq(p.x - m.x) + sq(p.y - m.y));
        }

        function pdrop(line, mouse) {
            var x1 = line.x1,
                y1 = line.y1,
                x2 = line.x2,
                y2 = line.y2,
                x3 = mouse.x,
                y3 = mouse.y,
                k = ((y2-y1) * (x3-x1) - (x2-x1) * (y3-y1)) /
                    (sq(y2-y1) + sq(x2-x1)),
                x4 = x3 - k * (y2-y1),
                y4 = y3 + k * (x2-x1);
            return {x:x4, y:y4};
        }

        function lineSeg(d) {
            return {
                x1: d.source.x,
                y1: d.source.y,
                x2: d.target.x,
                y2: d.target.y
            };
        }

        function lineHit(line, p, m) {
            if (p.x < line.x1 && p.x < line.x2) return false;
            if (p.x > line.x1 && p.x > line.x2) return false;
            if (p.y < line.y1 && p.y < line.y2) return false;
            if (p.y > line.y1 && p.y > line.y2) return false;
            // line intersects, but are we close enough?
            return mdist(p, m) <= proximity;
        }

        if (network.links.length) {
            nearest = null;
            minDist = proximity * 2;

            network.links.forEach(function (d) {
                if (!api.showHosts() && d.type() === 'hostLink') {
                    return; // skip hidden host links
                }

                var line = lineSeg(d),
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

            enhanceNearestLink(nearest);
        }
    }


    function enhanceNearestLink(ldata) {
        // if the new link is same as old link, do nothing
        if (enhancedLink && ldata && enhancedLink.key === ldata.key) return;

        // first, unenhance the currently enhanced link
        if (enhancedLink) {
            unenhance(enhancedLink);
        }
        enhancedLink = ldata;
        if (enhancedLink) {
            enhance(enhancedLink);
        }
    }

    function unenhance(d) {
        // guard against link element not set
        if (d.el) {
            d.el.classed('enhanced', false);
        }
        api.portLabelG().selectAll('.portLabel').remove();
    }

    function enhance(d) {
        var data = [],
            point;

        // guard against link element not set
        if (!d.el) return;

        d.el.classed('enhanced', true);
        $log.debug('[' + (d.srcPort || 'H') + '] ---> [' + d.tgtPort + ']', d.key);

        // Define port label data objects.
        // NOTE: src port is absent in the case of host-links.

        point = locatePortLabel(d);
        angular.extend(point, {
            id: 'topo-port-tgt',
            num: d.tgtPort
        });
        data.push(point);

        if (d.srcPort) {
            point = locatePortLabel(d, 1);
            angular.extend(point, {
                id: 'topo-port-src',
                num: d.srcPort
            });
            data.push(point);
        }

        td3.applyPortLabels(data, api.portLabelG());
    }

    function locatePortLabel(link, src) {
        var near = src ? 'source' : 'target',
            far = src ? 'target' : 'source',
            ln = link[near],
            lf = link[far],
            offset = 32;

        function dist(x, y) { return Math.sqrt(x*x + y*y); }

        var dx = lf.x - ln.x,
            dy = lf.y - ln.y,
            k = offset / dist(dx, dy);

        return {x: k * dx + ln.x, y: k * dy + ln.y};
    }

    function togglePorts() {
        showPorts = !showPorts;

        var what = showPorts ? 'Enable' : 'Disable',
            handler = showPorts ? mouseMoveHandler : null;

        if (!showPorts) {
            enhanceNearestLink(null);
        }
        svg.on('mousemove', handler);
        flash.flash(what + ' port highlighting');
    }

    // ==========================
    // Module definition

    angular.module('ovTopo')
        .factory('TopoLinkService',
        ['$log', 'FnService', 'SvgUtilService', 'ThemeService', 'FlashService',

        function (_$log_, _fs_, _sus_, _ts_, _flash_) {
            $log = _$log_;
            fs = _fs_;
            sus = _sus_;
            ts = _ts_;
            flash = _flash_;

            function initLink(_api_, _td3_) {
                api = _api_;
                td3 = _td3_;
                svg = api.svg;
                network = api.network;
                if (showPorts) {
                    svg.on('mousemove', mouseMoveHandler);
                }
            }

            function destroyLink() {
                // unconditionally remove any mousemove event handler
                svg.on('mousemove', null);
            }

            return {
                initLink: initLink,
                destroyLink: destroyLink,
                togglePorts: togglePorts
            };
        }]);
}());
