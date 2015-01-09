/*
 * Copyright 2014,2015 Open Networking Laboratory
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
 ONOS GUI -- Topology View Module

 @author Simon Hunt
 */

(function () {
    'use strict';

    var moduleDependencies = [
        'onosUtil',
        'onosSvg'
    ];

    // references to injected services etc.
    var $log, ks, zs, gs, ms;

    // DOM elements
    var svg, defs;

    // Internal state
    var zoomer;

    // Note: "exported" state should be properties on 'self' variable

    // --- Short Cut Keys ------------------------------------------------

    var keyBindings = {
        W: [logWarning, '(temp) log a warning'],
        E: [logError, '(temp) log an error'],
        R: [resetZoom, 'Reset pan / zoom']
    };

    // -----------------
    // these functions are necessarily temporary examples....
    function logWarning() {
        $log.warn('You have been warned!');
    }
    function logError() {
        $log.error('You are erroneous!');
    }
    // -----------------

    function resetZoom() {
        zoomer.reset();
    }

    function setUpKeys() {
        ks.keyBindings(keyBindings);
    }


    // --- Glyphs, Icons, and the like -----------------------------------

    function setUpDefs() {
        defs = svg.append('defs');
        gs.loadDefs(defs);
    }


    // --- Pan and Zoom --------------------------------------------------

    // zoom enabled predicate. ev is a D3 source event.
    function zoomEnabled(ev) {
        return (ev.metaKey || ev.altKey);
    }

    function zoomCallback() {
        var tr = zoomer.translate(),
            sc = zoomer.scale();
        $log.log('ZOOM: translate = ' + tr + ', scale = ' + sc);

        // TODO: keep the map lines constant width while zooming
        //bgImg.style('stroke-width', 2.0 / scale + 'px');
    }

    function setUpZoom() {
        var zoomLayer = svg.append('g').attr('id', 'topo-zoomlayer');
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomEnabled: zoomEnabled,
            zoomCallback: zoomCallback
        });
    }


    // --- Controller Definition -----------------------------------------

    angular.module('ovTopo', moduleDependencies)

        .controller('OvTopoCtrl', [
            '$log', 'KeyService', 'ZoomService', 'GlyphService', 'MapService',

        function (_$log_, _ks_, _zs_, _gs_, _ms_) {
            var self = this;
            $log = _$log_;
            ks = _ks_;
            zs = _zs_;
            gs = _gs_;
            ms = _ms_;

            // exported state
            self.message = 'Topo View Rocks!';

            // svg layer and initialization of components
            svg = d3.select('#ov-topo svg');
            setUpKeys();
            setUpDefs();
            setUpZoom();

            $log.log('OvTopoCtrl has been created');
        }]);
}());
