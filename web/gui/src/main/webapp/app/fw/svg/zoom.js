/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- SVG -- Zoom Service
 */
(function () {
    'use strict';

    // configuration
    var defaultSettings = {
        zoomMin: 0.05,
        zoomMax: 50,
        zoomEnabled: function (ev) { return true; },
        zoomCallback: function () {}
    };

    // injected references to services
    var $log;

    angular.module('onosSvg')
        .factory('ZoomService', ['$log',

        function (_$log_) {
            $log = _$log_;

/*
            NOTE: opts is an object:
            {
                svg: svg,                       D3 selection of <svg> element
                zoomLayer: zoomLayer,           D3 selection of <g> element
                zoomEnabled: function (ev) { ... },
                zoomCallback: function () { ... }
            }

            where:
                * svg and zoomLayer should be D3 selections of DOM elements.
                    * zoomLayer <g> is a child of <svg> element.
                * zoomEnabled is an optional predicate based on D3 event.
                    * default is always enabled.
                * zoomCallback is an optional callback invoked each time we pan/zoom.
                    * default is do nothing.

            Optionally, zoomMin and zoomMax also can be defined.
            These default to 0.25 and 10 respectively.
*/
            function createZoomer(opts) {
                var cz = 'ZoomService.createZoomer(): ',
                    d3s = ' (D3 selection) property defined',
                    settings = angular.extend({}, defaultSettings, opts),
                    zoom = d3.behavior.zoom()
                        .translate([0, 0])
                        .scale(1)
                        .scaleExtent([settings.zoomMin, settings.zoomMax])
                        .on('zoom', zoomed),
                    fail = false,
                    zoomer;


                if (!settings.svg) {
                    $log.error(cz + 'No "svg" (svg tag)' + d3s);
                    fail = true;
                }
                if (!settings.zoomLayer) {
                    $log.error(cz + 'No "zoomLayer" (g tag)' + d3s);
                    fail = true;
                }

                if (fail) {
                    return null;
                }

                // zoom events from mouse gestures...
                function zoomed() {
                    var ev = d3.event.sourceEvent;
                    if (settings.zoomEnabled(ev)) {
                        adjustZoomLayer(d3.event.translate, d3.event.scale);
                    }
                }

                function adjustZoomLayer(translate, scale, transition) {

                    settings.zoomLayer.transition()
                        .duration(transition || 0)
                        .attr("transform",
                            'translate(' + translate + ')scale(' + scale + ')');

                    settings.zoomCallback(translate, scale);
                }

                zoomer = {
                    panZoom: function (translate, scale, transition) {

                        zoom.translate(translate).scale(scale);
                        adjustZoomLayer(translate, scale, transition);
                    },

                    reset: function () {
                        zoomer.panZoom([0,0], 1);
                    },

                    translate: function () {
                        return zoom.translate();
                    },

                    scale: function () {
                        return zoom.scale();
                    },

                    scaleExtent: function () {
                        return zoom.scaleExtent();
                    }
                };

                // apply the zoom behavior to the SVG element
                settings.svg && settings.svg.call(zoom);

                // Remove zoom on double click (prevents a
                // false zoom navigating regions)
                settings.svg.on("dblclick.zoom", null);
                return zoomer;
            }

            return {
                createZoomer: createZoomer
            };
        }]);

}());
