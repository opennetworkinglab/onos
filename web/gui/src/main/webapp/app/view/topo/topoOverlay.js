/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

/*
 ONOS GUI -- Topology Overlay Module.

 Provides overlay capabilities, allowing ONOS apps to provide additional
 custom data/behavior for the topology view.

 */

(function () {
    'use strict';

    // constants
    var tos = 'TopoOverlayService: ';

    // injected refs
    var $log, fs, gs, wss;

    // internal state
    var overlays = {},
        current = null;

    function error(fn, msg) {
        $log.error(tos + fn + '(): ' + msg);
    }

    function warn(fn, msg) {
        $log.warn(tos + fn + '(): ' + msg);
    }

    function handleGlyph(o) {
        var gdata = fs.isO(o.glyph),
            oid,
            data = {};

        if (!gdata) {
            o._glyphId = 'unknown';
        } else {
            if (gdata.id) {
                o._glyphId = gdata.id;
            } else if (gdata.vb && gdata.d) {
                oid = o.overlayId;
                data['_' + oid] = gdata.vb;
                data[oid] = gdata.d;
                gs.registerGlyphs(data);
                o._glyphId = oid;
                $log.debug('registered overlay glyph:', oid);
            } else {
                warn('registerGlyph', 'problem with glyph data');
            }
        }
    }

    function register(overlay) {
        var r = 'register',
            over = fs.isO(overlay),
            id = over ? over.overlayId : '';

        if (!id) {
            return error(r, 'not a recognized overlay');
        }
        if (overlays[id]) {
            return warn(r, 'already registered: "' + id + '"');
        }
        overlays[id] = overlay;
        handleGlyph(overlay);
        $log.debug(tos + 'registered overlay: ' + id, overlay);
    }

    // NOTE: unregister needs to be called if an app is ever
    //       deactivated/uninstalled via the applications view
    function unregister(overlay) {
        var u = 'unregister',
            over = fs.isO(overlay),
            id = over ? over.overlayId : '';

        if (!id) {
            return error(u, 'not a recognized overlay');
        }
        if (!overlays[id]) {
            return warn(u, 'not registered: "' + id + "'")
        }
        delete overlays[id];
        $log.debug(tos + 'unregistered overlay: ' + id);
        // TODO: rebuild the toolbar overlay radio button set
    }

    function list() {
        return d3.map(overlays).keys();
    }

    function overlay(id) {
        return overlays[id];
    }

    // an overlay was selected via toolbar radio button press from user
    function tbSelection(id) {
        var same = current && current.overlayId === id,
            payload = {};

        function doop(op) {
            var oid = current.overlayId;
            $log.debug('Overlay:', op, oid);
            current[op]();
            payload[op] = oid;
        }

        if (!same) {
            current && doop('deactivate');
            current = overlay(id);
            current && doop('activate');
            wss.sendEvent('topoSelectOverlay', payload);

            // TODO: refactor to emit "flush on overlay change" messages
            wss.sendEvent('requestSummary');
        }
    }

    angular.module('ovTopo')
    .factory('TopoOverlayService',
        ['$log', 'FnService', 'GlyphService', 'WebSocketService',

        function (_$log_, _fs_, _gs_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            gs = _gs_;
            wss = _wss_;

            return {
                register: register,
                unregister: unregister,
                list: list,
                overlay: overlay,
                tbSelection: tbSelection
            }
        }]);

}());