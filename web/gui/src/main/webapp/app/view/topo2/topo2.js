/*
* Copyright 2016-present Open Networking Laboratory
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
 ONOS GUI -- Topology (2) View Module

 NOTE: currently under development to support Regions.
 */

(function () {
    'use strict';

    // references to injected services
    var $scope, $log, fs, mast, ks,
        gs, sus, ps, t2es, t2fs, t2is, t2bcs, t2kcs, t2ms, t2mcs, t2zs;

    // DOM elements
    var ovtopo2, svg, defs, zoomLayer, forceG;

    // Internal state
    var zoomer,
        currentLayoutId = '_default_';  // NOTE: see UiTopoLayoutId.DEFAULT_STR


    // --- Glyphs, Icons, and the like -----------------------------------

    function setUpDefs() {
        defs = svg.append('defs');
        gs.loadDefs(defs);

        // TODO: consider using something other than the "glow" styles
        sus.loadGlowDefs(defs);
    }

    // callback invoked when the SVG view has been resized..
    function svgResized(s) {
        $log.debug('topo2 view resized', s);
        t2fs.newDim([s.width, s.height]);
    }

    function setUpKeys(overlayKeys) {
        $log.debug('topo2: set up keys....');
    }

    // --- Pan and Zoom --------------------------------------------------

    // zoom enabled predicate. ev is a D3 source event.
    function zoomEnabled(ev) {
        return fs.isMobile() || (ev.metaKey || ev.altKey);
    }

    function zoomCallback() {
        var sc = zoomer.scale(),
            tr = zoomer.translate(),
            sparse = {};

        sparse[currentLayoutId] =  {
            zoomScale: sc,
            zoomPanX: tr[0],
            zoomPanY: tr[1]
        };

        ps.mergePrefs('topo2_zoom', sparse);
    }

    function setUpZoom() {
        zoomLayer = svg.append('g').attr('id', 'topo2-zoomlayer');

        zoomer = t2zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomEnabled: zoomEnabled,
            zoomCallback: zoomCallback
        });
    }

    // === Controller Definition -----------------------------------------

    angular.module('ovTopo2', ['onosUtil', 'onosSvg', 'onosRemote'])
    .controller('OvTopo2Ctrl', [
        '$scope', '$log', '$location',
        'FnService', 'MastService', 'KeyService', 'GlyphService', 'MapService',
        'SvgUtilService', 'FlashService', 'WebSocketService',
        'PrefsService', 'ThemeService',
        'Topo2EventService', 'Topo2ForceService', 'Topo2InstanceService',
        'Topo2BreadcrumbService', 'Topo2KeyCommandService', 'Topo2MapService',
        'Topo2MapConfigService', 'Topo2ZoomService', 'Topo2SpriteLayerService',
        'Topo2SummaryPanelService', 'Topo2DeviceDetailsPanel',

        function (
            _$scope_, _$log_, _$loc_,
            _fs_, _mast_, _ks_, _gs_, _ms_,
            _sus_, _flash_, _wss_,
            _ps_, _th_,
            _t2es_, _t2fs_, _t2is_,
            _t2bcs_, _t2kcs_, _t2ms_,
            _t2mcs_, _t2zs_, t2sls,
            summaryPanel, detailsPanel
        ) {
            var params = _$loc_.search(),
                dim,
                wh,
                uplink = {
                    // provides function calls back into this space
                    // showNoDevs: showNoDevs,
                    // projection: function () { return projection; },
                    zoomLayer: function () { return zoomLayer; },
                    zoomer: function () { return zoomer; }
                    // opacifyMap: opacifyMap,
                };

            $scope = _$scope_;
            $log = _$log_;

            fs = _fs_;
            mast = _mast_;
            ks = _ks_;
            gs = _gs_;
            sus = _sus_;
            ps = _ps_;

            t2es = _t2es_;
            t2fs = _t2fs_;
            t2is = _t2is_;
            t2bcs = _t2bcs_;
            t2kcs = _t2kcs_;
            t2ms = _t2ms_;
            t2mcs = _t2mcs_;
            t2zs = _t2zs_;

            // capture selected intent parameters (if they are set in the
            //  query string) so that the traffic overlay can highlight
            //  the path for that intent
            if (params.intentKey &&
                params.intentAppId &&
                params.intentAppName) {

                $scope.intentData = {
                    key: params.intentKey,
                    appId: params.intentAppId,
                    appName: params.intentAppName,
                    intentType: params.intentType
                };
            }

            $scope.notifyResize = function () {
                svgResized(fs.windowSize(mast.mastHeight()));
            };

            // Cleanup on destroyed scope..
            $scope.$on('$destroy', function () {
                $log.log('OvTopo2Ctrl is saying Buh-Bye!');
                t2es.stop();
                ks.unbindKeys();
                t2fs.destroy();
                t2is.destroy();
                summaryPanel.destroy();
                detailsPanel.destroy();
            });

            // svg layer and initialization of components
            ovtopo2 = d3.select('#ov-topo2');
            svg = ovtopo2.select('svg');
            // set the svg size to match that of the window, less the masthead
            wh = fs.windowSize(mast.mastHeight());
            $log.debug('setting topo SVG size to', wh);
            svg.attr(wh);
            dim = [wh.width, wh.height];

            // set up our keyboard shortcut bindings
            setUpKeys();
            setUpZoom();
            setUpDefs();

            // make sure we can respond to topology events from the server
            t2es.bindHandlers();

            // Add the map SVG Group, but don't load a map yet...
            //   we will wait until the server tells us whether we should
            //   be loading a geomap or a sprite layer
            t2ms.init(zoomLayer, zoomer);

            // TODO: figure out from where to call this code...
            // we still need to do the equivalent of this when we load
            //  a geo map, just not here.
            //
            // ... NOTE: we still have to position the nodes AFTER the map
            //           has loaded and the projection has been established...
            //           maybe another promise ending with a "positionNodes()"
            //           call?

            // .then(
            //     function (proj) {
            //         var z = ps.getPrefs('topo2_zoom', { tx: 0, ty: 0, sc: 1 });
            //         zoomer.panZoom([z.tx, z.ty], z.sc);
            //
            //         t2mcs.projection(proj);
            //         $log.debug('** Zoom restored:', z);
            //         $log.debug('** We installed the projection:', proj);
            //
            //         // Now the map has load and we have a projection we can
            //         // get the info from the server
            //         t2es.start();
            //     }
            // );

            t2sls.init(svg, zoomLayer);
            t2fs.init(svg, forceG, uplink, dim, zoomer);
            t2bcs.init();
            t2kcs.init(t2fs);
            t2is.initInst({ showMastership: t2fs.showMastership });

            // === ORIGINAL CODE ===

            // setUpToolbar();
            // setUpNoDevs();

            // tes.bindHandlers();
            // setUpSprites();

            // forceG = zoomLayer.append('g').attr('id', 'topo-force');
            // tfs.initForce(svg, forceG, uplink, dim);
            // tis.initInst({ showMastership: tfs.showMastership });
            // tps.initPanels();

            // restoreConfigFromPrefs();
            // ttbs.setDefaultOverlay(prefsState.ovid);

            // $log.debug('registered overlays...', tov.list());

            summaryPanel.init();
            detailsPanel.init();

            // Now that we are initialized, ask the server for what we
            //  need to show.
            t2es.start();

            $log.log('OvTopo2Ctrl has been created');
        }]);
})();
