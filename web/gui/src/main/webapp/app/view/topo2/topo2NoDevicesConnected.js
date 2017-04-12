/*
 * Copyright 2017-present Open Networking Laboratory
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
 ONOS GUI -- Topology No Connected Devices View.
 View that contains the 'No Connected Devices' message
 */

(function () {

    var instance;

    angular.module('ovTopo2')
        .factory('Topo2NoDevicesConnectedService', [
            '$log', 'Topo2ViewController', 'GlyphService', 'SvgUtilService',
            function ($log, ViewController, gs, sus) {

                var NoDevicesConnected = ViewController.extend({
                    id: 'topo2-noDevsLayer',
                    displayName: 'No Devices Connected',

                    init: function () {
                        instance = this;
                        this.appendElement('#topo2', 'g')
                            .attr({
                                transform: sus.translate(500, 500)
                            });

                        this.render();
                        this.show();
                    },

                    render: function () {
                        var g, box;

                        g = this.node().append('g');
                        gs.addGlyph(g, 'bird', 100).attr('class', 'noDevsBird');
                        g.append('text').text('No devices are connected')
                            .attr({ x: 120, y: 80});

                        box = g.node().getBBox();
                        box.x -= box.width/2;
                        box.y -= box.height/2;
                        g.attr('transform', sus.translate(box.x, box.y));
                    }
                });

                return instance || new NoDevicesConnected();
            }
        ]);
})();