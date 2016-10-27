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
 ONOS GUI -- Topology View Module

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
    var zoomer;

    // --- Glyphs, Icons, and the like -----------------------------------

    function setUpDefs() {
        defs = svg.append('defs');
        gs.loadDefs(defs);
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
            tr = zoomer.translate();

        ps.setPrefs('topo_zoom', { tx: tr[0], ty: tr[1], sc: sc });
    }

    function setUpZoom() {
        zoomLayer = svg.append('g').attr('id', 'topo-zoomlayer');

        zoomer = t2zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomEnabled: zoomEnabled,
            zoomCallback: zoomCallback
        });
    }

    // === Controller Definition -----------------------------------------

    angular.module('ovTopo2', ['onosUtil', 'onosSvg', 'onosRemote'])
    .controller('OvTopo2Ctrl',
        ['$scope', '$log', '$location',
        'FnService', 'MastService', 'KeyService',
        'GlyphService', 'MapService', 'SvgUtilService', 'FlashService',
        'WebSocketService', 'PrefsService', 'ThemeService',
        'Topo2EventService', 'Topo2ForceService', 'Topo2InstanceService',
        'Topo2BreadcrumbService', 'Topo2KeyCommandService', 'Topo2MapService',
        'Topo2MapConfigService', 'Topo2ZoomService',
        'Topo2SummaryPanelService', 'Topo2DeviceDetailsPanel',

        function (_$scope_, _$log_, _$loc_,
            _fs_, _mast_, _ks_,
            _gs_, _ms_, _sus_, _flash_,
            _wss_, _ps_, _th_,
            _t2es_, _t2fs_, _t2is_, _t2bcs_, _t2kcs_, _t2ms_, _t2mcs_,
            _t2zs_, summaryPanel, detailsPanel
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
                    // topoStartDone: topoStartDone
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
                    appName: params.intentAppName
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

            // Add the map SVG Group
            t2ms.init(zoomLayer, zoomer).then(
                function (proj) {
                    var z = ps.getPrefs('topo_zoom', { tx: 0, ty: 0, sc: 1 });
                    zoomer.panZoom([z.tx, z.ty], z.sc);

                    t2mcs.projection(proj);
                    $log.debug('** Zoom restored:', z);
                    $log.debug('** We installed the projection:', proj);

                    // Now the map has load and we have a projection we can
                    // get the info from the server
                    t2es.start();
                }
            );

            // initialize the force layout, ready to render the topology
            forceG = zoomLayer.append('g').attr('id', 'topo-force');

            t2fs.init(svg, forceG, uplink, dim);
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
            // ttbs.setDefaultOverlay(prefsState.ovidx);

            // $log.debug('registered overlays...', tov.list());

            summaryPanel.init();
            detailsPanel.init();

            $log.log('OvTopo2Ctrl has been created');
        }]);
})();
