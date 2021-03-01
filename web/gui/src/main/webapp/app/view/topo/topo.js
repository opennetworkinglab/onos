/*
 * Copyright 2014-present Open Networking Foundation
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
        'onosRemote',
    ];

    // references to injected services
    var $scope, $log, $loc, $timeout,
        fs, ks, zs, gs, ms, sus, flash, wss, ps, th, tds, t3s, tes, tfs, tps,
        tis, tms, tss, tls, tos, fltr, ttbs, tspr, tov;

    // DOM elements
    var ovtopo, svg, defs, zoomLayer, mapG, spriteG, forceG, noDevsLayer;

    // Internal state
    var zoomer,
        actionMap,
        topoLion = function (x) { return '#' + x + '#'; }; // func replaced later

    // --- Short Cut Keys ------------------------------------------------

    function setUpKeys(overlayKeys) {
        // key bindings need to be made after the services have been injected
        // thus, deferred to here...

        // we need functions that can be invoked after LION bundle loaded
        function togInst() { return topoLion('tbtt_tog_instances'); }
        function togSumm() { return topoLion('tbtt_tog_summary'); }
        function togUseDet() { return topoLion('tbtt_tog_use_detail'); }

        function togHost() { return topoLion('tbtt_tog_host'); }
        function togOff() { return topoLion('tbtt_tog_offline'); }
        function togPortHi() { return topoLion('tbtt_tog_porthi'); }

        function showBad() { return topoLion('tbtt_bad_links'); }
        function togMap() { return topoLion('tbtt_tog_map'); }
        function selMap() { return topoLion('tbtt_sel_map'); }
        function togSpr() { return topoLion('tbtt_tog_sprite'); }

        function rstLoc() { return topoLion('tbtt_reset_loc'); }
        function togOb() { return topoLion('tbtt_tog_oblique'); }
        function cycLayer() { return topoLion('tbtt_cyc_layers'); }
        function cycDev() { return topoLion('tbtt_cyc_dev_labs'); }
        function cycHost() { return topoLion('tbtt_cyc_host_labs'); }

        function unpin() { return topoLion('tbtt_unpin_node'); }
        function rzoom() { return topoLion('tbtt_reset_zoom'); }
        function togtb() { return topoLion('tbtt_tog_toolbar'); }
        function eqmaster() { return topoLion('tbtt_eq_master'); }

        function uiClick() { return topoLion('click'); }
        function uiShClick() { return topoLion('shift_click'); }
        function uiDrag() { return topoLion('drag'); }
        function uiCmdScr() { return topoLion('cmd_scroll'); }
        function uiCmdDrag() { return topoLion('cmd_drag'); }

        function uiClickTxt() { return topoLion('qh_gest_click'); }
        function uiShClickTxt() { return topoLion('qh_gest_shift_click'); }
        function uiDragTxt() { return topoLion('qh_gest_drag'); }
        function uiCmdScrTxt() { return topoLion('qh_gest_cmd_scroll'); }
        function uiCmdDragTxt() { return topoLion('qh_gest_cmd_drag'); }
        function quiet() { return ""; }

        actionMap = {
            I: [toggleInstances, togInst],
            O: [toggleSummary, togSumm],
            D: [toggleUseDetailsFlag, togUseDet],

            H: [toggleHosts, togHost],
            M: [toggleOffline, togOff],
            P: [togglePorts, togPortHi],

            dash: [tfs.showBadLinks, showBad],
            B: [toggleMap, togMap],
            G: [openMapSelection, selMap],
            S: [toggleSprites, togSpr],

            X: [tfs.resetAllLocations, rstLoc],
            Z: [tos.toggleOblique, togOb],
            N: [fltr.clickAction, cycLayer],
            L: [tfs.cycleDeviceLabels, cycDev],
            'shift-L': [tfs.cycleHostLabels, cycHost],
            'alt-L': [tfs.cycleLinkLabels, quiet],

            U: [tfs.unpin, unpin],
            R: [resetZoom, rzoom],
            dot: [ttbs.toggleToolbar, togtb],
            E: [equalizeMasters, eqmaster],

            "shift-openBracket": [tfs.toggleHostTextSize, quiet],
            "shift-closeBracket": [tfs.toggleHostIconSize, quiet],

            // -- instance color palette debug
            // 9: function () { sus.cat7().testCard(svg); },

            // topology overlay selections
            F1: function () { ttbs.fnkey(0); },
            F2: function () { ttbs.fnkey(1); },
            F3: function () { ttbs.fnkey(2); },
            F4: function () { ttbs.fnkey(3); },
            F5: function () { ttbs.fnkey(4); },

            esc: handleEscape,

            _keyListener: ttbs.keyListener,

            _helpFormat: [
                ['I', 'O', 'D', 'H', 'M', 'P', 'dash', 'B', 'G', 'S'],
                ['X', 'Z', 'N', 'L', 'shift-L', 'U', 'R', '-', 'E', '-', 'dot'],
                [], // this column reserved for overlay actions
            ],
        };

        if (fs.isO(overlayKeys)) {
            mergeKeys(overlayKeys);
        }

        ks.keyBindings(actionMap);

        ks.gestureNotes([
            [uiClick, uiClickTxt],
            [uiShClick, uiShClickTxt],
            [uiDrag, uiDragTxt],
            [uiCmdScr, uiCmdScrTxt],
            [uiCmdDrag, uiCmdDragTxt],
        ]);
    }

    // when a topology overlay is activated, we need to bind their keystrokes
    // and include them in the quick-help panel
    function mergeKeys(extra) {
        var _hf = actionMap._helpFormat[2];

        ks.checkNotGlobal(extra);

        extra._keyOrder.forEach(function (k) {
            var d = extra[k],
                cb = d && d.cb,
                tt = d && d.tt;
            // NOTE: ignore keys that are already defined
            if (d && !actionMap[k]) {
                actionMap[k] = [cb, tt];
                _hf.push(k);
            }
        });
    }

    // --- Keystroke functions -------------------------------------------

    function toggleInstances(x) {
        updatePrefsState('insts', tis.toggle(x));
        tfs.updateDeviceColors();
    }

    function toggleSummary(x) {
        updatePrefsState('summary', tps.toggleSummary(x));
    }

    function toggleUseDetailsFlag(x) {
        updatePrefsState('detail', tps.toggleUseDetailsFlag(x));
    }

    function toggleHosts(x) {
        updatePrefsState('hosts', tfs.toggleHosts(x));
    }

    function toggleOffline(x) {
        updatePrefsState('offdev', tfs.toggleOffline(x));
    }

    function togglePorts(x) {
        updatePrefsState('porthl', tfs.togglePorts(x));
    }

    function _togSvgLayer(x, G, tag, what) {
        var on = (x === 'keyev') ? !sus.visible(G) : !!x,
            verb = on ? topoLion('show') : topoLion('hide');
        sus.visible(G, on);
        updatePrefsState(tag, on);
        flash.flash(verb + ' ' + what);
    }

    function toggleMap(x) {
        _togSvgLayer(x, mapG, 'bg', topoLion('fl_background_map'));
    }

    function openMapSelection() {
        tms.openMapSelection();
    }

    function toggleSprites(x) {
        _togSvgLayer(x, spriteG, 'spr', topoLion('fl_sprite_layer'));
    }

    function resetZoom() {
        zoomer.reset();
        flash.flash(topoLion('fl_pan_zoom_reset'));
    }

    function equalizeMasters() {
        wss.sendEvent('equalizeMasters');
        flash.flash(topoLion('fl_eq_masters'));
    }

    function handleEscape() {
        if (tis.showMaster()) {
            // if an instance is selected, cancel the affinity mapping
            tis.cancelAffinity();

        } else if (tov.hooks.escape()) {
            // else if the overlay consumed the ESC event...
            // (work already done)

        } else if (tss.deselectAll()) {
            // else if we have node selections, deselect them all
            // (work already done)

        } else if (tls.deselectAllLinks()) {
            // else if we have a link selected, deselect it
            // (work already done)

        } else if (tis.isVisible()) {
            // else if the Instance Panel is visible, hide it
            tis.hide();
            tfs.updateDeviceColors();

        } else if (tps.summaryVisible()) {
            // else if the Summary Panel is visible, hide it
            tps.hideSummary();
        }
    }

    // --- Toolbar Functions ---------------------------------------------

    function notValid(what) {
        $log.warn('topo.js getActionEntry(): Not a valid ' + what);
    }

    function getActionEntry(key) {
        var entry;

        if (!key) {
            notValid('key');
            return null;
        }

        entry = actionMap[key];

        if (!entry) {
            notValid('actionMap entry');
            return null;
        }
        return fs.isA(entry) || [entry, ''];
    }

    function setUpToolbar() {
        ttbs.init({
            getActionEntry: getActionEntry,
            setUpKeys: setUpKeys,
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
        var sc = zoomer.scale(),
            tr = zoomer.translate();

        ps.setPrefs('topo_zoom', { tx: tr[0], ty: tr[1], sc: sc });

        // keep the map lines constant width while zooming
        mapG.style('stroke-width', (2.0 / sc) + 'px');

        tfs.adjustNodeScale();
    }

    function setUpZoom() {
        zoomLayer = svg.append('g').attr('id', 'topo-zoomlayer');
        zoomer = zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomEnabled: zoomEnabled,
            zoomCallback: zoomCallback,
        });
    }


    // callback invoked when the SVG view has been resized..
    function svgResized(s) {
        tfs.newDim([s.width, s.height]);
    }

    // --- Background Map ------------------------------------------------

    function recenterLabel(g) {
        var box = g.node().getBBox();

        box.x -= box.width/2;
        box.y -= box.height/2;
        g.attr('transform', sus.translate(box.x, box.y));
    }

    function setUpNoDevs() {
        var g;

        noDevsLayer = svg.append('g').attr({
            id: 'topo-noDevsLayer',
            transform: sus.translate(500, 500),
        });
        // Note, SVG viewbox is '0 0 1000 1000', defined in topo.html.
        // We are translating this layer to have its origin at the center

        g = noDevsLayer.append('g');
        gs.addGlyph(g, 'bird', 100).attr('class', 'noDevsBird');
        g.append('text').text('').attr({ x: 120, y: 80 });
        recenterLabel(g);
        showNoDevs(true);
    }

    function lionNoDevs() {
        var g = d3.select('#topo-noDevsLayer g');

        g.select('text').text(topoLion('no_devices_are_connected'));
        recenterLabel(g);
    }

    function showNoDevs(b) {
        sus.visible(noDevsLayer, b);
    }


    var countryFilters = {
        s_america: function (c) {
            return c.properties.continent === 'South America';
        },

        ns_america: function (c) {
            return c.properties.custom === 'US-cont' ||
                c.properties.subregion === 'Central America' ||
                c.properties.continent === 'South America';
        },

        japan: function (c) {
            return c.properties.geounit === 'Japan';
        },

        europe: function (c) {
            return c.properties.continent === 'Europe';
        },

        italy: function (c) {
            return c.properties.geounit === 'Italy';
        },

        uk: function (c) {
            // technically, Ireland is not part of the United Kingdom,
            // but the map looks weird without it showing.
            return c.properties.adm0_a3 === 'GBR' ||
                c.properties.adm0_a3 === 'IRL';
        },

        s_korea: function (c) {
            return c.properties.adm0_a3 === 'KOR';
        },

        australia: function (c) {
            return c.properties.adm0_a3 === 'AUS';
        },
    };

    var tintOn = 0,
        shadeFlip = 0,
        shadePalette = {
        light: {
            sea: 'aliceblue',
            land: 'white',
            outline: '#ddd',
        },
        dark: {
            sea: '#001830',
            land: '#232331',
            outline: '#3a3a3a',
        },
    };

    function shading() {
        return tintOn ? {
            palette: shadePalette[th.theme()],
            flip: shadeFlip,
        } : '';
    }

    function setMap(map) {
        ps.setPrefs('topo_mapid', map);
        setUpMap();
        opacifyMap(true);
    }

    function currentMap() {
        return ps.getPrefs(
            'topo_mapid',
            {
                mapid: 'usa',
                mapscale: 1,
                mapfilepath: '*continental_us',
                tint: 'off',
            },
            $loc.search()
        );
    }

    function setUpMap() {
        var prefs = currentMap(),
            mapId = prefs.mapid,
            mapFilePath = prefs.mapfilepath,
            mapScale = prefs.mapscale,
            tint = prefs.tint,
            promise,
            cfilter;

        tintOn = tint === 'on' ? 1 : 0;

        $log.debug('setUpMap() mapId:', mapId, ', mapScale:', mapScale,
                   ', tint:', tint);

        mapG = d3.select('#topo-map');
        if (mapG.empty()) {
            mapG = zoomLayer.append('g').attr('id', 'topo-map');
        } else {
            mapG.each(function (d, i) {
                d3.selectAll(this.childNodes).remove();
            });
        }

        if (mapFilePath === '*countries') {

            cfilter = countryFilters[mapId] || countryFilters.uk;

            promise = ms.loadMapRegionInto(mapG, {
                countryFilter: cfilter,
                adjustScale: mapScale,
                shading: shading(),
            });
        } else {

            promise = ms.loadMapInto(mapG, mapFilePath, mapId, {
                adjustScale: mapScale,
                shading: shading(),
            });
        }

        ps.setPrefs('topo_mapid', prefs);
        return promise;
    }

    function mapReshader() {
        $log.debug('... Re-shading map ...');
        ms.reshade(shading());
    }

    // set up theme listener to re-shade the map when required.
    function mapShader(on) {
        if (on) {
            th.addListener(mapReshader);
        } else {
            th.removeListener(mapReshader);
        }
    }

    function opacifyMap(b) {
        mapG.transition()
            .duration(1000)
            .attr('opacity', b ? 1 : 0);
    }

    function setUpSprites() {
        var prefs = ps.getPrefs('topo_sprites', { sprites: '' }, $loc.search()),
            sprId = prefs.sprites;

        spriteG = zoomLayer.append('g').attr('id', 'topo-sprites');
        if (sprId) {
            ps.setPrefs('topo_sprites', prefs);
            tspr.loadSprites(spriteG, defs, sprId);
        }
    }

    // --- User Preferemces ----------------------------------------------

    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        ps.setPrefs('topo_prefs', prefsState);
    }

    function applyPreferences(evt) {
//        var zoomPrefs = ps.getPrefs('topo_zoom', null);
//        if (ps.getPrefs('topo_prefs', null)) {
//            restoreConfigFromPrefs();
//        }
//        if (zoomPrefs) {
//            $log.debug('TOPO- Zoom State:', zoomPrefs);
//            zoomer.panZoom([zoomPrefs.tx, zoomPrefs.ty], zoomPrefs.sc, 100);
//        }
    }

    function restoreConfigFromPrefs() {
        // NOTE: toolbar will have set this for us..
        prefsState = ps.asNumbers(
            ps.getPrefs('topo_prefs', ttbs.defaultPrefs), ['ovid'], true
        );

        $log.debug('TOPO- Prefs State:', prefsState);

        flash.enable(false);
        toggleInstances(prefsState.insts);
        toggleSummary(prefsState.summary);
        toggleUseDetailsFlag(prefsState.detail);
        toggleHosts(prefsState.hosts);
        toggleOffline(prefsState.offdev);
        togglePorts(prefsState.porthl);
        toggleMap(prefsState.bg);
        toggleSprites(prefsState.spr);
        ttbs.setToolbar(prefsState.toolbar);
        t3s.setDevLabIndex(prefsState.dlbls);
        t3s.setHostLabIndex(prefsState.hlbls);
        flash.enable(true);
    }


    // somewhat hackish, because summary update cannot happen until we
    //  have opened the websocket to the server; hence this extra function
    // invoked after tes.start()
    function restoreSummaryFromPrefs() {
        prefsState = ps.asNumbers(
            ps.getPrefs('topo_prefs', ttbs.defaultPrefs), ['ovid'], true
        );
        $log.debug('TOPO- Prefs SUMMARY State:', prefsState.summary);

        flash.enable(false);
        toggleSummary(prefsState.summary);
        flash.enable(true);
    }

    // initial set of topo events received, now do post-processing
    function topoStartDone() {
        // give a small delay before attempting to reselect node(s) and
        // highlight elements, since they have to be re-added to the DOM first...
        $timeout(function () {
            $log.debug('^^ topo.topoStartDone() ^^');

            // reselect the previous selection...
            tss.reselect();

            // if an intent should be shown, invoke the appropriate callback
            if ($scope.intentData) {
                tov.hooks.showIntent($scope.intentData);
            }

        }, 200);
    }

    // --- Controller Definition -----------------------------------------

    angular.module('ovTopo', moduleDependencies)
        .controller('OvTopoCtrl',
            ['$scope', '$log', '$location', '$timeout', '$cookies',
            'FnService', 'MastService', 'KeyService', 'ZoomService',
            'GlyphService', 'MapService', 'SvgUtilService', 'FlashService',
            'WebSocketService', 'PrefsService', 'ThemeService',
            'TopoDialogService', 'TopoD3Service', 'TopoEventService',
            'TopoForceService', 'TopoPanelService', 'TopoInstService',
            'TopoSelectService', 'TopoLinkService', 'TopoTrafficService',
            'TopoObliqueService', 'TopoFilterService', 'TopoToolbarService',
            'TopoMapService', 'TopoSpriteService', 'TooltipService',
            'TopoOverlayService', 'LionService',

                function (_$scope_, _$log_, _$loc_, _$timeout_, _$cookies_,
                  _fs_, mast, _ks_, _zs_,
                  _gs_, _ms_, _sus_, _flash_,
                  _wss_, _ps_, _th_,
                  _tds_, _t3s_, _tes_,
                  _tfs_, _tps_, _tis_,
                  _tss_, _tls_, _tts_,
                  _tos_, _fltr_, _ttbs_,
                  _tms_, _tspr_, _ttip_,
                  _tov_, lion) {

            var params = _$loc_.search(),
                selOverlay = params.overlayId,
                projection,
                dim,
                uplink = {
                    // provides function calls back into this space
                    showNoDevs: showNoDevs,
                    projection: function () { return projection; },
                    zoomLayer: function () { return zoomLayer; },
                    zoomer: function () { return zoomer; },
                    opacifyMap: opacifyMap,
                    topoStartDone: topoStartDone,
                };

            $scope = _$scope_;
            $log = _$log_;
            $loc = _$loc_;
            $timeout = _$timeout_;
            fs = _fs_;
            ks = _ks_;
            zs = _zs_;
            gs = _gs_;
            ms = _ms_;
            sus = _sus_;
            flash = _flash_;
            wss = _wss_;
            ps = _ps_;
            th = _th_;
            tds = _tds_;
            t3s = _t3s_;
            tes = _tes_;
            tfs = _tfs_;
            // TODO: consider funnelling actions through TopoForceService...
            //  rather than injecting references to these 'sub-modules',
            //  just so we can invoke functions on them.
            tps = _tps_;
            tis = _tis_;
            tms = _tms_;
            tls = _tls_;
            tos = _tos_;
            fltr = _fltr_;
            ttbs = _ttbs_;
            tspr = _tspr_;
            tov = _tov_;
            tss = _tss_;

            tms.start({
                toggleMap: toggleMap,
                currentMap: currentMap,
                setMap: setMap,
            });

            // pull intent data from the query string...
            if (params.key && params.appId && params.appName) {
                $scope.intentData = {
                    key: params.key,
                    appId: params.appId,
                    appName: params.appName,
                    intentType: params.intentType,
                };
            }

            $scope.notifyResize = function () {
                svgResized(fs.windowSize(mast.mastHeight()));
            };

            // Cleanup on destroyed scope..
            $scope.$on('$destroy', function () {
                $log.log('OvTopoCtrl is saying Buh-Bye!');
                tes.stop();
                tms.stop();
                ks.unbindKeys();
                tps.destroyPanels();
                tds.closeDialog();
                tis.destroyInst();
                tfs.destroyForce();
                ttbs.destroyToolbar();
                mapShader(false);
            });

            // svg layer and initialization of components
            ovtopo = d3.select('#ov-topo');
            svg = ovtopo.select('svg');
            // set the svg size to match that of the window, less the masthead
            svg.attr(fs.windowSize(mast.mastHeight()));
            dim = [svg.attr('width'), svg.attr('height')];

            ps.addListener(applyPreferences);

            setUpKeys();
            setUpToolbar();
            setUpDefs();
            setUpZoom();
            setUpNoDevs();
            setUpMap().then(
                function (proj) {
                    var z = ps.getPrefs('topo_zoom', { tx: 0, ty: 0, sc: 1 });
                    zoomer.panZoom([z.tx, z.ty], z.sc);
                    $log.debug('** Zoom restored:', z);

                    projection = proj;
                    $log.debug('** We installed the projection:', proj);
                    flash.enable(false);
                    toggleMap(prefsState.bg);
                    flash.enable(true);
                    mapShader(true);

                    // piggyback off the deferred map loading to load the
                    // localization bundle after the uber bundle has arrived...
                    $scope.lion = lion.bundle('core.view.Topo');
                    topoLion = $scope.lion;
                    $log.debug('Loaded Topo LION Bundle:', topoLion);

                    // insert localized text into already established
                    // DOM elements...
                    lionNoDevs();

                    // pass lion bundle function ref to other topo modules
                    tfs.setLionBundle(topoLion);
                    tis.setLionBundle(topoLion);
                    tms.setLionBundle(topoLion);
                    tps.setLionBundle(topoLion);
                    ttbs.setLionBundle(topoLion);

                    // now we have the map projection, we are ready for
                    //  the server to send us device/host data...
                    tes.start();
                    // need to do the following so we immediately get
                    //  the summary panel data back from the server
                    restoreSummaryFromPrefs();
                }
            );
            tes.bindHandlers();
            setUpSprites();

            forceG = zoomLayer.append('g').attr('id', 'topo-force');
            tfs.initForce(svg, forceG, uplink, dim);
            tis.initInst({ showMastership: tfs.showMastership });
            tps.initPanels();

            restoreConfigFromPrefs();

            ttbs.selectOverlay(selOverlay || prefsState.ovid);

            $log.debug('registered overlays...', tov.list());
            $log.log('OvTopoCtrl has been created');
        }]);
}());
