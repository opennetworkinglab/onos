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
 ONOS GUI -- Topology Zoom Module.
 Module that handles Zoom events.
 */

(function () {
    'use strict';

    // injected references
    var fs, zs, ps;

    // internal state
    var zoomer,
        zoomEventListeners = [];

    function createZoomer(options) {
        // need to wrap the original zoom callback to extend its behavior
        var origCallback = fs.isF(options.zoomCallback) || function () {};

        options.zoomCallback = function () {
            origCallback();

            angular.forEach(zoomEventListeners, function (ev) {
                ev(zoomer);
            });
        };

        zoomer = zs.createZoomer(options);
        return zoomer;
    }

    function getZoomer() {
        return zoomer;
    }

    function findZoomEventListener(ev) {
        for (var i = 0, len = zoomEventListeners.length; i < len; i++) {
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

    function adjustmentScale(min, max) {
        var _scale = 1,
            size = (min + max) / 2;

        if (size * scale() < max) {
            _scale = min / (size * scale());
        } else if (size * scale() > max) {
            _scale = min / (size * scale());
        }

        return _scale;
    }

    function scale() {
        return zoomer.scale();
    }

    function panAndZoom(translate, scale, transition) {
        zoomer.panZoom(translate, scale, transition);
    }

    angular.module('ovTopo2')
    .factory('Topo2ZoomService', [
        'FnService', 'ZoomService', 'PrefsService',
        function (_fs_, _zs_, _ps_) {

            fs = _fs_;
            zs = _zs_;
            ps = _ps_;

            return {
                getZoomer: getZoomer,
                createZoomer: createZoomer,
                addZoomEventListener: addZoomEventListener,
                removeZoomEventListener: removeZoomEventListener,

                scale: scale,
                adjustmentScale: adjustmentScale,
                panAndZoom: panAndZoom
            };
        }]);
})();
