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
    var $log, fs, sus, ts;

    var api,
        td3,
        network,
        enhancedLink = null;    // the link which the mouse is hovering over

    // SVG elements;
    var svg, mouseG;


    // ======== ALGORITHM TO FIND LINK CLOSEST TO MOUSE ========

    function setupMouse(forceG, zoomer) {
        $log.debug('set up mouse handlers for mouse move');
        mouseG = forceG.append('g').attr('id', 'topo-mouse');
        //mouseG.append('circle')
        //    .attr({
        //        r: 5,
        //        opacity: 0
        //    })
        //    .style('fill', 'red');

        svg.on('mouseenter', function () {
            //$log.log('M--ENTER');
            //mouseG.selectAll('circle').attr('opacity', 1);
        })
            .on('mouseleave', function () {
                //$log.log('M--LEAVE');
                //mouseG.selectAll('circle').attr('opacity', 0);
            })
            .on('mousemove', function () {
                var m = d3.mouse(this),
                    sc = zoomer.scale(),
                    tr = zoomer.translate(),
                    mx = (m[0] - tr[0]) / sc,
                    my = (m[1] - tr[1]) / sc;

                //$log.log('M--MOVE', m);

                //mouseG.selectAll('circle')
                //    .attr({
                //        cx: mx,
                //        cy: my
                //    });
                updatePerps({x: mx, y: my}, zoomer);
            });
    }

    function updatePerps(mouse, zoomer) {
        var proximity = 30 / zoomer.scale(),
            perpData, perps, nearest, minDist;

        function sq(x) { return x * x; }

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

        function mdist(p, m) {
            return Math.sqrt(sq(p.x - m.x) + sq(p.y - m.y));
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
            perpData = [];
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
                    /*
                     perpData.push({
                     key: d.key,
                     x1: mouse.x,
                     y1: mouse.y,
                     x2: point.x,
                     y2: point.y
                     });
                     */
                }
            });

            /*
             perps = mouseG.selectAll('line')
             .data(perpData, function (d) { return d.key; })
             .attr({
             x1: function (d) { return d.x1; },
             y1: function (d) { return d.y1; },
             x2: function (d) { return d.x2; },
             y2: function (d) { return d.y2; }
             });

             perps.enter().append('line')
             .attr({
             x1: function (d) { return d.x1; },
             y1: function (d) { return d.y1; },
             x2: function (d) { return d.x2; },
             y2: function (d) { return d.y2; }
             })
             .style('stroke-width', 2)
             .style('stroke', 'limegreen');

             perps.exit().remove();
             */

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
        d.el.classed('enhanced', false);
        api.portLabelG().selectAll('.portLabel').remove();
    }

    function enhance(d) {
        d.el.classed('enhanced', true);
        $log.debug('[' + (d.srcPort || 'H') + '] ---> [' + d.tgtPort + ']', d.key);

        // define port label data objects
        var data = [
            {
                id: 'topo-port-src',
                num: d.srcPort,
                baseX: d.source.x,
                baseY: d.source.y
            },
            {
                id: 'topo-port-tgt',
                num: d.tgtPort,
                baseX: d.target.x,
                baseY: d.target.y
            }
        ];

        td3.applyPortLabels(data, api.portLabelG());
    }


    // ==========================
    // Module definition

    angular.module('ovTopo')
        .factory('TopoLinkService',
        ['$log', 'FnService', 'SvgUtilService', 'ThemeService',

            function (_$log_, _fs_, _sus_, _ts_) {
                $log = _$log_;
                fs = _fs_;
                sus = _sus_;
                ts = _ts_;

                function initLink(_api_, _td3_) {
                    api = _api_;
                    td3 = _td3_;
                    svg = api.svg;
                    network = api.network;
                    setupMouse(api.forceG, api.zoomer);
                }

                function destroyLink() {
                    svg.on('mouseenter', null)
                        .on('mouseleave', null)
                        .on('mousemove', null);
                }

                return {
                    initLink: initLink,
                    destroyLink: destroyLink
                };
            }]);
}());
