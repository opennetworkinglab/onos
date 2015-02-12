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
    var $log, fs, ks, zs, gs, ms, sus, tes, tfs, tps, tis, tss, tts;

    // DOM elements
    var ovtopo, svg, defs, zoomLayer, mapG, forceG, noDevsLayer;

    // Internal state
    var zoomer;

    // --- Short Cut Keys ------------------------------------------------

    function setUpKeys() {
        // key bindings need to be made after the services have been injected
        // thus, deferred to here...
        ks.keyBindings({
            O: [tps.toggleSummary, 'Toggle ONOS summary pane'],
            I: [toggleInstances, 'Toggle ONOS instances pane'],
            D: [tss.toggleDetails, 'Disable / enable details pane'],

            H: [tfs.toggleHosts, 'Toggle host visibility'],
            M: [tfs.toggleOffline, 'Toggle offline visibility'],
            //B: [toggleBg, 'Toggle background image'],
            //P: togglePorts,

            //X: [toggleNodeLock, 'Lock / unlock node positions'],
            //Z: [toggleOblique, 'Toggle oblique view (Experimental)'],
            L: [tfs.cycleDeviceLabels, 'Cycle device labels'],
            U: [tfs.unpin, 'Unpin node (hover mouse over)'],
            R: [resetZoom, 'Reset pan / zoom'],

            V: [tts.showRelatedIntentsAction, 'Show all related intents'],
            rightArrow: [tts.showNextIntentAction, 'Show next related intent'],
            leftArrow: [tts.showPrevIntentAction, 'Show previous related intent'],
            W: [tts.showSelectedIntentTrafficAction, 'Monitor traffic of selected intent'],
            A: [tts.showAllTrafficAction, 'Monitor all traffic'],
            F: [tts.showDeviceLinkFlowsAction, 'Show device link flows'],

            //E: [equalizeMasters, 'Equalize mastership roles'],

            esc: handleEscape,

            _helpFormat: [
                ['O', 'I', 'D', '-', 'H', 'M', 'B', 'P' ],
                ['X', 'Z', 'L', 'U', 'R' ],
                ['V', 'rightArrow', 'leftArrow', 'W', 'A', 'F', '-', 'E' ]
            ]
        });

        // TODO:         // mouse gestures
        var gestures = [
            ['click', 'Select the item and show details'],
            ['shift-click', 'Toggle selection state'],
            ['drag', 'Reposition (and pin) device / host'],
            ['cmd-scroll', 'Zoom in / out'],
            ['cmd-drag', 'Pan']
        ];
    }

    // --- Keystroke functions -------------------------------------------

    // NOTE: this really belongs in the TopoPanelService -- but how to
    //       cleanly link in the updateDeviceColors() call? To be fixed later.
    function toggleInstances() {
        tis.toggle();
        tfs.updateDeviceColors();
    }

    function resetZoom() {
        zoomer.reset();
    }

    function handleEscape() {
        if (false) {
            // TODO: if an instance is selected, cancel the affinity mapping
            // tis.cancelAffinity()

        } else if (tss.haveDetails()) {
            // else if we have node selections, deselect them all
            tss.deselectAll();

        } else if (tis.isVisible()) {
            // else if the Instance Panel is visible, hide it
            tis.hide();
            tfs.updateDeviceColors();

        } else if (tps.summaryVisible()) {
            // else if the Summary Panel is visible, hide it
            tps.hideSummaryPanel();

        } else {
            // TODO: set hover mode to hoverModeNone
        }
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
        var sc = zoomer.scale();

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
    function svgResized(s) {
        tfs.newDim([s.width, s.height]);
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


    // --- Controller Definition -----------------------------------------

    angular.module('ovTopo', moduleDependencies)

        .controller('OvTopoCtrl', [
            '$scope', '$log', '$location', '$timeout',
            'FnService', 'MastService', 'KeyService', 'ZoomService',
            'GlyphService', 'MapService', 'SvgUtilService',
            'TopoEventService', 'TopoForceService', 'TopoPanelService',
            'TopoInstService', 'TopoSelectService', 'TopoTrafficService',

        function ($scope, _$log_, $loc, $timeout, _fs_, mast,
                  _ks_, _zs_, _gs_, _ms_, _sus_,
                  _tes_, _tfs_, _tps_, _tis_, _tss_, _tts_) {
            var self = this,
                projection,
                dim,
                uplink = {
                    // provides function calls back into this space
                    showNoDevs: showNoDevs,
                    projection: function () { return projection; },
                    sendEvent: _tes_.sendEvent
                };

            $log = _$log_;
            fs = _fs_;
            ks = _ks_;
            zs = _zs_;
            gs = _gs_;
            ms = _ms_;
            sus = _sus_;
            tes = _tes_;
            tfs = _tfs_;
            tps = _tps_;
            tis = _tis_;
            tss = _tss_;
            tts = _tts_;

            self.notifyResize = function () {
                svgResized(fs.windowSize(mast.mastHeight()));
            };

            // Cleanup on destroyed scope..
            $scope.$on('$destroy', function () {
                $log.log('OvTopoCtrl is saying Buh-Bye!');
                tes.closeSock();
                tps.destroyPanels();
                tis.destroyInst();
                tfs.destroyForce();
            });

            // svg layer and initialization of components
            ovtopo = d3.select('#ov-topo');
            svg = ovtopo.select('svg');
            // set the svg size to match that of the window, less the masthead
            svg.attr(fs.windowSize(mast.mastHeight()));
            dim = [svg.attr('width'), svg.attr('height')];

            setUpKeys();
            setUpDefs();
            setUpZoom();
            setUpNoDevs();
            setUpMap().then(
                function (proj) {
                    projection = proj;
                    $log.debug('** We installed the projection: ', proj);
                }
            );

            forceG = zoomLayer.append('g').attr('id', 'topo-force');
            tfs.initForce(forceG, uplink, dim);
            tis.initInst();
            tps.initPanels({ sendEvent: tes.sendEvent });
            tes.openSock();

            $log.log('OvTopoCtrl has been created');
        }]);
}());
