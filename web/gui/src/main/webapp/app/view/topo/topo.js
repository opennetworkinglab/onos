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
    var $log, fs, ks, zs, gs, ms, sus, tfs, tis;

    // DOM elements
    var ovtopo, svg, defs, zoomLayer, mapG, forceG, noDevsLayer;

    // Internal state
    var zoomer;

    // Note: "exported" state should be properties on 'self' variable

    // --- Short Cut Keys ------------------------------------------------

    var keyBindings = {
        //O: [toggleSummary, 'Toggle ONOS summary pane'],
        I: [toggleInstances, 'Toggle ONOS instances pane'],
        //D: [toggleDetails, 'Disable / enable details pane'],

        //H: [toggleHosts, 'Toggle host visibility'],
        //M: [toggleOffline, 'Toggle offline visibility'],
        //B: [toggleBg, 'Toggle background image'],
        //P: togglePorts,

        //X: [toggleNodeLock, 'Lock / unlock node positions'],
        //Z: [toggleOblique, 'Toggle oblique view (Experimental)'],
        L: [cycleLabels, 'Cycle device labels'],
        //U: [unpin, 'Unpin node (hover mouse over)'],
        R: [resetZoom, 'Reset pan / zoom'],

        //V: [showRelatedIntentsAction, 'Show all related intents'],
        //rightArrow: [showNextIntentAction, 'Show next related intent'],
        //leftArrow: [showPrevIntentAction, 'Show previous related intent'],
        //W: [showSelectedIntentTrafficAction, 'Monitor traffic of selected intent'],
        //A: [showAllTrafficAction, 'Monitor all traffic'],
        //F: [showDeviceLinkFlowsAction, 'Show device link flows'],

        //E: [equalizeMasters, 'Equalize mastership roles'],

        //esc: handleEscape,

        _helpFormat: [
            ['O', 'I', 'D', '-', 'H', 'M', 'B', 'P' ],
            ['X', 'Z', 'L', 'U', 'R' ],
            ['V', 'rightArrow', 'leftArrow', 'W', 'A', 'F', '-', 'E' ]
        ]

    };

    // mouse gestures
    var gestures = [
        ['click', 'Select the item and show details'],
        ['shift-click', 'Toggle selection state'],
        ['drag', 'Reposition (and pin) device / host'],
        ['cmd-scroll', 'Zoom in / out'],
        ['cmd-drag', 'Pan']
    ];

    function toggleInstances() {
        if (tis.isVisible()) {
            tis.hide();
        } else {
            tis.show();
        }
        tfs.updateDeviceColors();
    }

    function cycleLabels() {
        $log.debug('Cycle Labels.....');
    }

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
        tfs.resize(dim);
    }

    // --- Background Map ------------------------------------------------

    function setUpNoDevs() {
        var g, box;
        noDevsLayer = svg.append('g').attr({
            id: 'topo-noDevsLayer',
            transform: sus.translate(500,500)
        });
        // Note, SVG viewbox is '0 0 1000 1000', defined in topo.html.
        // We are translating this layer to have its origin at the center

        g = noDevsLayer.append('g');
        gs.addGlyph(g, 'bird', 100).attr('class', 'noDevsBird');
        g.append('text').text('No devices are connected')
            .attr({ x: 120, y: 80});

        box = g.node().getBBox();
        box.x -= box.width/2;
        box.y -= box.height/2;
        g.attr('transform', sus.translate(box.x, box.y));

        showNoDevs(true);
    }

    function showNoDevs(b) {
        sus.makeVisible(noDevsLayer, b);
    }

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

        //showCallibrationPoints();
        //return ms.loadMapInto(map, '*continental_us', {mapFillScale:0.5});

        // returns a promise for the projection...
        return ms.loadMapInto(mapG, '*continental_us');
    }

    // --- Force Layout --------------------------------------------------

    function setUpForce(xlink) {
        forceG = zoomLayer.append('g').attr('id', 'topo-force');
        tfs.initForce(forceG, xlink, svg.attr('width'), svg.attr('height'));
    }

    // --- Controller Definition -----------------------------------------

    angular.module('ovTopo', moduleDependencies)

        .controller('OvTopoCtrl', [
            '$scope', '$log', '$location', '$timeout',
            'FnService', 'MastService', 'KeyService', 'ZoomService',
            'GlyphService', 'MapService', 'SvgUtilService',
            'TopoEventService', 'TopoForceService', 'TopoPanelService',
            'TopoInstService',

        function ($scope, _$log_, $loc, $timeout, _fs_, mast,
                  _ks_, _zs_, _gs_, _ms_, _sus_, tes, _tfs_, tps, _tis_) {
            var self = this,
                xlink = {
                    showNoDevs: showNoDevs
                };

            $log = _$log_;
            fs = _fs_;
            ks = _ks_;
            zs = _zs_;
            gs = _gs_;
            ms = _ms_;
            sus = _sus_;
            tfs = _tfs_;
            tis = _tis_;

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
            setUpNoDevs();
            xlink.projectionPromise = setUpMap();
            setUpForce(xlink);

            tis.initInst();
            tps.initPanels();
            tes.openSock();

            $log.log('OvTopoCtrl has been created');
        }]);
}());
