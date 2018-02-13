/*
* Copyright 2016-present Open Networking Foundation
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
    var $scope, $log, fs, mast, ks, wss,
        gs, sus, t2es, t2fs, t2is, t2bcs, t2kcs, t2ms, t2zs, t2ts;

    // DOM elements
    var ovtopo2, svg, defs, zoomLayer, forceG;

    // Internal state
    var zoomer;

    // --- Glyphs, Icons, and the like -----------------------------------

    function setUpDefs() {
        defs = svg.append('defs');
        gs.loadDefs(defs);

        // TODO: consider using something other than the "glow" styles
        sus.loadGlowDefs(defs);
    }

    // callback invoked when the SVG view has been resized..
    function svgResized(s) {
        t2fs.newDim([s.width, s.height]);
    }

    // --- Pan and Zoom --------------------------------------------------

    // zoom enabled predicate. ev is a D3 source event.
    function zoomEnabled(ev) {
        return fs.isMobile() || (ev.metaKey || ev.altKey);
    }

    function zoomCallback() {
        var sc = zoomer.scale(),
            tr = zoomer.translate(),
            metaUi = isNaN(sc) ? {
                useCfg: 1,
            } : {
                scale: sc,
                offsetX: tr[0],
                offsetY: tr[1],
            };

        // Allow map service to react to change in zoom parameters
        t2ms.zoomCallback(sc, tr);

        // Note: Meta data stored in the context of the current layout,
        //       automatically, by the server

        wss.sendEvent('updateMeta2', {
            id: 'layoutZoom',
            memento: metaUi,
        });
    }

    function setUpZoom() {
        zoomLayer = svg.append('g').attr('id', 'topo2-zoomlayer');

        zoomer = t2zs.createZoomer({
            svg: svg,
            zoomLayer: zoomLayer,
            zoomEnabled: zoomEnabled,
            zoomCallback: zoomCallback,
        });
    }

    // === Controller Definition -----------------------------------------

    angular.module('ovTopo2', ['onosUtil', 'onosSvg', 'onosRemote'])
    .controller('OvTopo2Ctrl', [
        '$scope', '$log', '$location',
        'FnService', 'MastService', 'KeyService', 'GlyphService', 'MapService',
        'SvgUtilService', 'FlashService', 'WebSocketService', 'ThemeService',
        'Topo2EventService', 'Topo2ForceService', 'Topo2InstanceService',
        'Topo2BreadcrumbService', 'Topo2KeyCommandService', 'Topo2MapService',
        'Topo2ZoomService', 'Topo2SpriteLayerService',
        'Topo2SummaryPanelService', 'Topo2DeviceDetailsPanel', 'Topo2ToolbarService',
        'Topo2NoDevicesConnectedService', 'Topo2OverlayService', 'Topo2TrafficService',

        function (
            _$scope_, _$log_, _$loc_,
            _fs_, _mast_, _ks_, _gs_, _ms_,
            _sus_, _flash_, _wss_, _th_,
            _t2es_, _t2fs_, _t2is_,
            _t2bcs_, _t2kcs_, _t2ms_,
            _t2zs_, t2sls,
            summaryPanel, detailsPanel, t2tbs, t2ndcs, t2os,
            _t2ts_
        ) {
            var params = _$loc_.search(),
                dim,
                wh,
                uplink = {
                    zoomLayer: function () { return zoomLayer; },
                    zoomer: function () { return zoomer; },
                };

            $scope = _$scope_;
            $log = _$log_;

            fs = _fs_;
            mast = _mast_;
            ks = _ks_;
            wss = _wss_;
            gs = _gs_;
            sus = _sus_;

            t2es = _t2es_;
            t2fs = _t2fs_;
            t2is = _t2is_;
            t2bcs = _t2bcs_;
            t2kcs = _t2kcs_;
            t2ms = _t2ms_;
            t2zs = _t2zs_;
            t2ts = _t2ts_;

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
                    intentType: params.intentType,
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
            setUpZoom();
            setUpDefs();

            t2ndcs.init();

            // make sure we can respond to topology events from the server
            t2es.bindHandlers();

            t2bcs.init();
            t2kcs.init(t2fs, t2tbs, svg);
            t2is.initInst({ showMastership: t2fs.showMastership });
            t2fs.init(svg, forceG, uplink, dim, zoomer);

            // === ORIGINAL CODE ===
            // restoreConfigFromPrefs();
            // ttbs.setDefaultOverlay(prefsState.ovid);

            // ++ TEMPORARY HARD-CODE TRAFFIC OVERLAY ++
            t2os.setOverlay('traffic-2-overlay');

            summaryPanel.init(detailsPanel);
            detailsPanel.init(summaryPanel);

            // Now that we are initialized, ask the server for what we
            // need to show.
            t2es.start();

            $log.log('OvTopo2Ctrl has been created');
        }]);
})();
