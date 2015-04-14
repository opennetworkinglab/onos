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
        'ngCookies',
        'onosUtil',
        'onosSvg',
        'onosRemote'
    ];

    // references to injected services etc.
    var $log, $cookies, fs, ks, zs, gs, ms, sus, flash, wss, ps,
        tes, tfs, tps, tis, tss, tls, tts, tos, fltr, ttbs;

    // DOM elements
    var ovtopo, svg, defs, zoomLayer, mapG, spriteG, forceG, noDevsLayer;

    // Internal state
    var zoomer, actionMap;

    // --- Short Cut Keys ------------------------------------------------

    function setUpKeys() {
        // key bindings need to be made after the services have been injected
        // thus, deferred to here...
        actionMap = {
            I: [toggleInstances, 'Toggle ONOS instances panel'],
            O: [toggleSummary, 'Toggle ONOS summary panel'],
            D: [toggleDetails, 'Disable / enable details panel'],

            H: [tfs.toggleHosts, 'Toggle host visibility'],
            M: [tfs.toggleOffline, 'Toggle offline visibility'],
            P: [tfs.togglePorts, 'Toggle Port Highlighting'],
            dash: [tfs.showBadLinks, 'Show bad links'],
            B: [toggleMap, 'Toggle background map'],
            S: [toggleSprites, 'Toggle sprite layer'],

            //X: [toggleNodeLock, 'Lock / unlock node positions'],
            Z: [tos.toggleOblique, 'Toggle oblique view (Experimental)'],
            N: [fltr.clickAction, 'Cycle node layers'],
            L: [tfs.cycleDeviceLabels, 'Cycle device labels'],
            U: [tfs.unpin, 'Unpin node (hover mouse over)'],
            R: [resetZoom, 'Reset pan / zoom'],
            dot: [ttbs.toggleToolbar, 'Toggle Toolbar'],

            V: [tts.showRelatedIntentsAction, 'Show all related intents'],
            rightArrow: [tts.showNextIntentAction, 'Show next related intent'],
            leftArrow: [tts.showPrevIntentAction, 'Show previous related intent'],
            W: [tts.showSelectedIntentTrafficAction, 'Monitor traffic of selected intent'],
            A: [tts.showAllTrafficAction, 'Monitor all traffic'],
            F: [tts.showDeviceLinkFlowsAction, 'Show device link flows'],

            E: [equalizeMasters, 'Equalize mastership roles'],

            esc: handleEscape,

            _keyListener: ttbs.keyListener,

            _helpFormat: [
                ['I', 'O', 'D', '-', 'H', 'M', 'P', 'dash', 'B' ],
                ['X', 'Z', 'N', 'L', 'U', 'R', '-', 'dot'],
                ['V', 'rightArrow', 'leftArrow', 'W', 'A', 'F', '-', 'E' ]
            ]
        };

        ks.keyBindings(actionMap);

        ks.gestureNotes([
            ['click', 'Select the item and show details'],
            ['shift-click', 'Toggle selection state'],
            ['drag', 'Reposition (and pin) device / host'],
            ['cmd-scroll', 'Zoom in / out'],
            ['cmd-drag', 'Pan']
        ]);
    }

    // --- Keystroke functions -------------------------------------------

    function toggleInstances(x) {
        updatePrefsState('insts', tis.toggle(x));
        tfs.updateDeviceColors();
    }

    function toggleSummary(x) {
        updatePrefsState('summary', tps.toggleSummary(x));
    }

    function toggleDetails(x) {
        updatePrefsState('detail', tps.toggleDetails(x));
    }

    function toggleMap(x) {
        var on = (x === 'keyev') ? !sus.visible(mapG) : !!x,
            verb = on ? 'Show' : 'Hide';
        sus.visible(mapG, on);
        updatePrefsState('bg', on);
        flash.flash(verb + ' background map');
    }

    function toggleSprites(x) {
        var on = (x === 'keyev') ? !sus.visible(spriteG) : !!x,
            verb = on ? 'Show' : 'Hide';
        sus.visible(spriteG, on);
        updatePrefsState('sprites', on);
        flash.flash(verb + ' sprite layer');
    }

    function resetZoom() {
        zoomer.reset();
    }

    function equalizeMasters() {
        wss.sendEvent('equalizeMasters');
        flash.flash('Equalizing master roles');
    }

    function handleEscape() {
        if (tis.showMaster()) {
            // if an instance is selected, cancel the affinity mapping
            tis.cancelAffinity()

        } else if (tss.deselectAll()) {
            // else if we have node selections, deselect them all
            // (work already done)

        } else if (tls.deselectLink()) {
            // else if we have a link selected, deselect it
            // (work already done)

        } else if (tis.isVisible()) {
            // else if the Instance Panel is visible, hide it
            tis.hide();
            tfs.updateDeviceColors();

        } else if (tps.summaryVisible()) {
            // else if the Summary Panel is visible, hide it
            tps.hideSummaryPanel();

        } else {
            // TODO: set hover mode to hoverModeNone
            // talk to Thomas about this: shouldn't it be done
            // when we deselect the node (if tss.haveDetails()...)
        }
    }

    // --- Toolbar Functions ---------------------------------------------

    function getActionEntry(key) {
        var entry = actionMap[key];
        return fs.isA(entry) || [entry, ''];
    }

    function setUpToolbar() {
        ttbs.init({
            getActionEntry: getActionEntry
        });
        ttbs.createToolbar();
    }

    // --- Glyphs, Icons, and the like -----------------------------------

    function setUpDefs() {
        defs = svg.append('defs');
        gs.loadDefs(defs);
        sus.loadGlowDefs(defs);
    }


    // --- Pan and Zoom --------------------------------------------------

    // zoom enabled predicate. ev is a D3 source event.
    function zoomEnabled(ev) {
        return fs.isMobile() || (ev.metaKey || ev.altKey);
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
        sus.visible(noDevsLayer, b);
    }

    function setUpMap() {
        mapG = zoomLayer.append('g').attr('id', 'topo-map');
        // returns a promise for the projection...
        return ms.loadMapInto(mapG, '*continental_us');
    }

    function opacifyMap(b) {
        mapG.transition()
            .duration(1000)
            .attr('opacity', b ? 1 : 0);
    }


    // --- User Preferemces ----------------------------------------------

    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        ps.setPrefs('topo_prefs', prefsState);
    }


    function restoreConfigFromPrefs() {
        // NOTE: toolbar will have set this for us..
        prefsState = ps.getPrefs('topo_prefs');

        $log.debug('TOPO---- Prefs State:', prefsState);

        toggleInstances(prefsState.insts);
        toggleSummary(prefsState.summary);
        toggleDetails(prefsState.detail);
        toggleSprites(prefsState.sprites);
    }


    // --- Controller Definition -----------------------------------------

    angular.module('ovTopo', moduleDependencies)
        .controller('OvTopoCtrl', ['$scope', '$log', '$location', '$timeout',
            '$cookies', 'FnService', 'MastService', 'KeyService', 'ZoomService',
            'GlyphService', 'MapService', 'SvgUtilService', 'FlashService',
            'WebSocketService', 'PrefsService',
            'TopoEventService', 'TopoForceService', 'TopoPanelService',
            'TopoInstService', 'TopoSelectService', 'TopoLinkService',
            'TopoTrafficService', 'TopoObliqueService', 'TopoFilterService',
            'TopoToolbarService', 'TopoSpriteService',

        function ($scope, _$log_, $loc, $timeout, _$cookies_, _fs_, mast, _ks_,
                  _zs_, _gs_, _ms_, _sus_, _flash_, _wss_, _ps_, _tes_, _tfs_,
                  _tps_, _tis_, _tss_, _tls_, _tts_, _tos_, _fltr_, _ttbs_, tspr) {
            var self = this,
                projection,
                dim,
                uplink = {
                    // provides function calls back into this space
                    showNoDevs: showNoDevs,
                    projection: function () { return projection; },
                    zoomLayer: function () { return zoomLayer; },
                    zoomer: function () { return zoomer; },
                    opacifyMap: opacifyMap
                };

            $log = _$log_;
            $cookies = _$cookies_;
            fs = _fs_;
            ks = _ks_;
            zs = _zs_;
            gs = _gs_;
            ms = _ms_;
            sus = _sus_;
            flash = _flash_;
            wss = _wss_;
            ps = _ps_;
            tes = _tes_;
            tfs = _tfs_;
            // TODO: consider funnelling actions through TopoForceService...
            //  rather than injecting references to these 'sub-modules',
            //  just so we can invoke functions on them.
            tps = _tps_;
            tis = _tis_;
            tss = _tss_;
            tls = _tls_;
            tts = _tts_;
            tos = _tos_;
            fltr = _fltr_;
            ttbs = _ttbs_;

            self.notifyResize = function () {
                svgResized(fs.windowSize(mast.mastHeight()));
            };

            // Cleanup on destroyed scope..
            $scope.$on('$destroy', function () {
                $log.log('OvTopoCtrl is saying Buh-Bye!');
                tes.stop();
                tps.destroyPanels();
                tis.destroyInst();
                tfs.destroyForce();
                ttbs.destroyToolbar();
            });

            // svg layer and initialization of components
            ovtopo = d3.select('#ov-topo');
            svg = ovtopo.select('svg');
            // set the svg size to match that of the window, less the masthead
            svg.attr(fs.windowSize(mast.mastHeight()));
            dim = [svg.attr('width'), svg.attr('height')];

            setUpKeys();
            setUpToolbar();
            setUpDefs();
            setUpZoom();
            setUpNoDevs();
            setUpMap().then(
                function (proj) {
                    projection = proj;
                    $log.debug('** We installed the projection: ', proj);
                    toggleMap(prefsState.bg);
                }
            );
            spriteG = zoomLayer.append ('g').attr('id', 'topo-sprites');
            tspr.loadSprites(spriteG);

            forceG = zoomLayer.append('g').attr('id', 'topo-force');
            tfs.initForce(svg, forceG, uplink, dim);
            tis.initInst({ showMastership: tfs.showMastership });
            tps.initPanels();
            tes.start();

            // temporary solution for persisting user settings
            restoreConfigFromPrefs();

            $log.log('OvTopoCtrl has been created');
        }]);
}());
