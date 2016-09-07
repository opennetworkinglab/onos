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
    var $scope, $log, $loc,
        fs, mast, ks, zs,
        gs, ms, sus, flash,
        wss, ps, th,
        t2es, t2fs, t2is, t2bcs;

    // DOM elements
    var ovtopo2, svg, defs, zoomLayer, mapG, spriteG, forceG, noDevsLayer;

    // Internal state
    var zoomer, actionMap;


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

        ps.setPrefs('topo_zoom', {tx:tr[0], ty:tr[1], sc:sc});

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


    // === Controller Definition -----------------------------------------

    angular.module('ovTopo2', ['onosUtil', 'onosSvg', 'onosRemote'])
    .controller('OvTopo2Ctrl',
        ['$scope', '$log', '$location',
        'FnService', 'MastService', 'KeyService', 'ZoomService',
        'GlyphService', 'MapService', 'SvgUtilService', 'FlashService',
        'WebSocketService', 'PrefsService', 'ThemeService',
        'Topo2EventService', 'Topo2ForceService', 'Topo2InstanceService',
        'Topo2BreadcrumbService',

        function (_$scope_, _$log_, _$loc_,
            _fs_, _mast_, _ks_, _zs_,
            _gs_, _ms_, _sus_, _flash_,
            _wss_, _ps_, _th_,
            _t2es_, _t2fs_, _t2is_, _t2bcs_) {

            var params = _$loc_.search(),
                projection,
                dim,
                wh,
                uplink = {
                    // provides function calls back into this space
                    // showNoDevs: showNoDevs,
                    // projection: function () { return projection; },
                    zoomLayer: function () { return zoomLayer; },
                    zoomer: function () { return zoomer; },
                    // opacifyMap: opacifyMap,
                    // topoStartDone: topoStartDone
                };

            $scope = _$scope_;
            $log = _$log_;
            $loc = _$loc_;

            fs = _fs_;
            mast = _mast_;
            ks = _ks_;
            zs = _zs_;

            gs = _gs_;
            ms = _ms_;
            sus = _sus_;
            flash = _flash_;

            wss = _wss_;
            ps = _ps_;
            th = _th_;

            t2es = _t2es_;
            t2fs = _t2fs_;
            t2is = _t2is_;
            t2bcs = _t2bcs_;

            // capture selected intent parameters (if they are set in the
            //  query string) so that the traffic overlay can highlight
            //  the path for that intent
            if (params.intentKey && params.intentAppId && params.intentAppName) {
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

            // initialize the force layout, ready to render the topology
            forceG = zoomLayer.append('g').attr('id', 'topo-force');
            t2fs.init(svg, forceG, uplink, dim);
            t2bcs.init();


            // =-=-=-=-=-=-=-=-
            // TODO: in future, we will load background map data
            //  asynchronously (hence the promise) and then chain off
            //  there to send the topo2start event to the server.
            // For now, we'll send the event inline...
            t2es.start();



            t2is.initInst({ showMastership: t2fs.showMastership });



            // === ORIGINAL CODE ===

            // setUpKeys();
            // setUpToolbar();
            // setUpDefs();
            // setUpZoom();
            // setUpNoDevs();
            /*
            setUpMap().then(
                function (proj) {
                    var z = ps.getPrefs('topo_zoom', { tx:0, ty:0, sc:1 });
                    zoomer.panZoom([z.tx, z.ty], z.sc);
                    $log.debug('** Zoom restored:', z);

                    projection = proj;
                    $log.debug('** We installed the projection:', proj);
                    flash.enable(false);
                    toggleMap(prefsState.bg);
                    flash.enable(true);
                    mapShader(true);

                    // now we have the map projection, we are ready for
                    //  the server to send us device/host data...
                    tes.start();
                    // need to do the following so we immediately get
                    //  the summary panel data back from the server
                    restoreSummaryFromPrefs();
                }
            );
            */
            // tes.bindHandlers();
            // setUpSprites();

            // forceG = zoomLayer.append('g').attr('id', 'topo-force');
            // tfs.initForce(svg, forceG, uplink, dim);
            // tis.initInst({ showMastership: tfs.showMastership });
            // tps.initPanels();

            // restoreConfigFromPrefs();
            // ttbs.setDefaultOverlay(prefsState.ovidx);

            // $log.debug('registered overlays...', tov.list());

            $log.log('OvTopo2Ctrl has been created');
        }]);
}());
