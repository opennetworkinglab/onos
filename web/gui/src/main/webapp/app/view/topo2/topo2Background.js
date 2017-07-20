/*
 * Copyright 2017-present Open Networking Laboratory
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
 ONOS GUI -- Topology Background Module.
 Module that maintains the map and sprite layers
 */

(function () {

    var $log;

    var instance;

    function getZoom(z, useCfg) {
        var u = z.usr,
            c = z.cfg;
        return (u && !u.useCfg && !useCfg) ? u : c;
    }

    // returns the pan (offset) values as an array [x, y]
    function zoomPan(z, useCfg) {
        var zoom = getZoom(z, useCfg);
        return [zoom.offsetX, zoom.offsetY];
    }

    // returns the scale value
    function zoomScale(z, useCfg) {
        var zoom = getZoom(z, useCfg);
        return zoom.scale;
    }

    angular.module('ovTopo2')
        .factory('Topo2BackgroundService', [
            '$log', 'Topo2ViewController', 'Topo2SpriteLayerService', 'Topo2MapService',
            'Topo2MapConfigService', 'Topo2ZoomService',
            function (_$log_, ViewController, t2sls, t2ms, t2mcs, t2zs) {

                $log = _$log_;

                var BackgroundView = ViewController.extend({

                    id: 'topo2-background',
                    displayName: 'Background',

                    init: function () {
                        instance = this;
                        this.appendElement('#topo2-zoomlayer', 'g');
                        t2sls.init();
                        t2ms.init();
                        this.zoomer = t2zs.getZoomer();
                    },

                    addLayout: function (data) {
                        this.background = data;
                        this.bgType = data.bgType;
                        this.bgId = data.bgId;
                        this.zoomData = data.bgZoom;

                        var _this = this,
                            pan = zoomPan(this.zoomData),
                            scale = zoomScale(this.zoomData);

                        if (this.bgType === 'geo') {
                            // Hide Sprite Layer and show Map
                            t2sls.hide();
                            t2ms.show();

                            t2ms.setUpMap(data.bgId, data.bgFilePath)
                            .then(function (proj) {
                                t2mcs.projection(proj);
                                $log.debug('** We installed the projection:', proj);
                                _this.region.loaded('bgRendered', true);
                                t2zs.panAndZoom(pan, scale, 1000);
                            });
                        } else if (this.bgType === 'grid') {

                            // Hide Sprite Layer and show Map
                            t2ms.hide();
                            t2sls.show();

                            t2sls.loadLayout(data.bgId).then(function (spriteLayout) {
                                _this.background.layout = spriteLayout;
                                _this.region.loaded('bgRendered', true);
                                t2zs.panAndZoom(pan, scale, 1000);
                            });
                        } else {
                            // No background type - Tell the region the background is ready for placing nodes
                            t2ms.hide();
                            t2sls.hide();

                            _this.region.loaded('bgRendered', true);

                            // Use default zoom and pan
                            t2zs.panAndZoom([0, 0], 1);
                        }
                    },

                    getBackgroundType: function () {
                        return this.bgType;
                    },

                    resetZoom: function () {
                        var pan = zoomPan(this.zoomData, true);
                        t2zs.panAndZoom(pan, zoomScale(this.zoomData, true), 1000);
                    }
                });

                return instance || new BackgroundView();
            }
        ]);
})();