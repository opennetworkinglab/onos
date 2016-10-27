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
 ONOS GUI -- Topology Breadcrumb Module.
 Module that renders the breadcrumbs for regions
 */

(function () {

    'use strict';

    var zs, ps;

    var zoomer,
        zoomEventListeners = [];

    function createZoomer(options) {
        var settings = angular.extend({}, options, {
            zoomCallback: zoomCallback
        });

        zoomer = zs.createZoomer(settings);
        return zoomer;
    }

    function zoomCallback() {
        var sc = zoomer.scale(),
            tr = zoomer.translate();

        ps.setPrefs('topo_zoom', { tx: tr[0], ty: tr[1], sc: sc });

        angular.forEach(zoomEventListeners, function (ev) {
            ev(zoomer);
        });
    }

    function findZoomEventListener(ev) {
        for (var i = 0, l = zoomEventListeners.length; i < l; i++) {
            if (zoomEventListeners[i] === ev) return i;
        }

        return -1;
    }

    function addZoomEventListener(callback) {
        zoomEventListeners.push(callback);
    }

    function removeZoomEventListener(callback) {

        var evIndex = findZoomEventListener(callback);

        if (evIndex !== -1) {
            zoomEventListeners.splice(evIndex);
        }
    }

    function scale() {
        return zoomer.scale();
    }

    function panAndZoom(translate, scale) {
        zoomer.panZoom(translate, scale, 1000);
    }

    angular.module('ovTopo2')
    .factory('Topo2ZoomService',
        ['ZoomService', 'PrefsService',
        function (_zs_, _ps_) {

            zs = _zs_;
            ps = _ps_;

            return {
                createZoomer: createZoomer,
                addZoomEventListener: addZoomEventListener,
                removeZoomEventListener: removeZoomEventListener,

                scale: scale,
                panAndZoom: panAndZoom
            };
        }]);
})();
