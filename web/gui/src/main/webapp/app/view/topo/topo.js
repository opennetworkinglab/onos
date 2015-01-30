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
 */

(function () {
    'use strict';

    var moduleDependencies = [
        'onosUtil',
        'onosSvg',
        'onosRemote'
    ];

    // references to injected services etc.
    var $log, fs, ks, zs, gs, ms, tfs;

    // DOM elements
    var ovtopo, svg, defs, zoomLayer, mapG, forceG;

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

        // keep the map lines constant width while zooming
        mapG.style('stroke-width', (2.0 / sc) + 'px');
    }

    function setUpZoom() {
        zoomLayer = svg.append('g').attr('id', 'topo-zoomlayer');
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomEnabled: zoomEnabled,
            zoomCallback: zoomCallback
        });
    }


    // callback invoked when the SVG view has been resized..
    function svgResized(dim) {
        //$log.debug('TopoView just resized... ', dim);
        tfs.resize(dim);
    }

    // --- Background Map ------------------------------------------------

    function showCallibrationPoints() {
        // temp code for calibration
        var points = [
            [0, 0], [0, 1000], [1000, 0], [1000, 1000]
        ];
        mapG.selectAll('circle')
            .data(points)
            .enter()
            .append('circle')
            .attr('cx', function (d) { return d[0]; })
            .attr('cy', function (d) { return d[1]; })
            .attr('r', 5)
            .style('fill', 'red');
    }

    function setUpMap() {
        mapG = zoomLayer.append('g').attr('id', 'topo-map');
        //ms.loadMapInto(map, '*continental_us', {mapFillScale:0.5});
        ms.loadMapInto(mapG, '*continental_us');
        //showCallibrationPoints();
    }

    // --- Force Layout --------------------------------------------------

    function setUpForce() {
        forceG = zoomLayer.append('g').attr('id', 'topo-force');
        tfs.initForce(forceG, svg.attr('width'), svg.attr('height'));
    }

    // --- Controller Definition -----------------------------------------

    angular.module('ovTopo', moduleDependencies)

        .controller('OvTopoCtrl', [
            '$scope', '$log', '$location', '$timeout',
            'FnService', 'MastService', 'KeyService', 'ZoomService',
            'GlyphService', 'MapService',
            'TopoEventService', 'TopoForceService', 'TopoPanelService',
            'TopoInstService',

        function ($scope, _$log_, $loc, $timeout, _fs_, mast,
                  _ks_, _zs_, _gs_, _ms_, tes, _tfs_, tps, tis) {
            var self = this;
            $log = _$log_;
            fs = _fs_;
            ks = _ks_;
            zs = _zs_;
            gs = _gs_;
            ms = _ms_;
            tfs = _tfs_;

            self.notifyResize = function () {
                svgResized(fs.windowSize(mast.mastHeight()));
            };

            // Cleanup on destroyed scope..
            $scope.$on('$destroy', function () {
                $log.log('OvTopoCtrl is saying Buh-Bye!');
                tes.closeSock();
                tps.destroyPanels();
                tis.destroyInst();
            });

            // svg layer and initialization of components
            ovtopo = d3.select('#ov-topo');
            svg = ovtopo.select('svg');
            // set the svg size to match that of the window, less the masthead
            svg.attr(fs.windowSize(mast.mastHeight()));

            setUpKeys();
            setUpDefs();
            setUpZoom();
            setUpMap();
            setUpForce();

            tis.initInst();
            tps.initPanels();
            tes.openSock();

            $log.log('OvTopoCtrl has been created');
        }]);
}());
