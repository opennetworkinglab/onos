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

    function mkGlyphId(oid, gid) {
        return (gid[0] === '*') ? oid + '-' + gid.slice(1) : gid;
    }

    function handleGlyphs(o) {
        var gdata = fs.isO(o.glyphs),
            oid = o.overlayId,
            gid = o.glyphId || 'unknown',
            data = {},
            note = [];

        o._glyphId = mkGlyphId(oid, gid);

        o.mkGid = function (g) {
            return mkGlyphId(oid, g);
        };
        o.mkId = function (s) {
            return oid + '-' + s;
        };

        // process glyphs if defined
        if (gdata) {
            angular.forEach(gdata, function (value, key) {
                var fullkey = oid + '-' + key;
                data['_' + fullkey] = value.vb;
                data[fullkey] = value.d;
                note.push('*' + key);
            });
            gs.registerGlyphs(data);
            $log.debug('registered overlay glyphs:', oid, note);
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
        handleGlyphs(overlay);
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

    // install buttons from the current overlay
    function installButtons(bids, addFn, data) {
        if (current) {
            bids.forEach(function (bid) {
                var btn = current.buttons[bid],
                    funcWrap = function () {
                        btn.cb(data);
                    };

                if (btn) {
                    addFn({
                        id: current.mkId(bid),
                        gid: current.mkGid(btn.gid),
                        cb: funcWrap,
                        tt: btn.tt
                    });
                }
            });
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
                tbSelection: tbSelection,
                installButtons: installButtons
            }
        }]);

}());