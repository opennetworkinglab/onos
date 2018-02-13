/*
 * Copyright 2017-present Open Networking Foundation
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

(function () {
    'use strict';

    // constants
    var t2os = 'Topo2OverlayService: ';

    // injected refs
    var $log, fs, t2kcs, t2rs, t2lc, LinkLabel;

    // internal state
    var overlays = {},
        current = null,
        reset = true;

    function error(fn, msg) {
        $log.error(t2os + fn + '(): ' + msg);
    }

    function warn(fn, msg) {
        $log.warn(t2os + fn + '(): ' + msg);
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
            kb = over ? fs.isO(overlay.keyBindings) : null,
            id = over ? over.overlayId : '';

        if (!id) {
            return error(r, 'not a recognized overlay');
        }
        if (overlays[id]) {
            return warn(r, 'already registered: "' + id + '"');
        }
        overlays[id] = overlay;
        handleGlyphs(overlay);

        if (kb) {
            if (!fs.isA(kb._keyOrder)) {
                warn(r, 'no _keyOrder array defined on keyBindings');
            } else {
                kb._keyOrder.forEach(function (k) {
                    if (k !== '-' && !kb[k]) {
                        warn(r, 'no "' + k + '" property defined on keyBindings');
                    }
                });
            }
        }

        $log.debug(t2os + 'registered overlay: ' + id, overlay);
    }

    // add a radio button for each registered overlay
    // return an overlay id to index map
    function augmentRbset(rset, switchFn) {
        var map = {},
            idx = 1;

        angular.forEach(overlays, function (ov) {
            rset.push({
                gid: ov._glyphId,
                tooltip: (ov.tooltip || ''),
                cb: function () {
                    tbSelection(ov.overlayId, switchFn);
                },
            });
            map[ov.overlayId] = idx++;
        });
        return map;
    }

    // an overlay was selected via toolbar radio button press from user
    function tbSelection(id, switchFn) {
        var same = current && current.overlayId === id,
            payload = {},
            actions;

        function doop(op) {
            var oid = current.overlayId;
            $log.debug('Overlay:', op, oid);
            current[op]();
            payload[op] = oid;
        }

        if (reset || !same) {
            current && doop('deactivate');
            current = overlays[id];
            current && doop('activate');
            actions = current && fs.isO(current.keyBindings);
            switchFn(id, actions);
            // TODO: Update Summary Panel
        }
    }

    // TODO: check topoOverlay.js for more code
    // TODO: medium term -- factor out common code
    // TODO: longer term -- deprecate classic topology view

    // === -----------------------------------------------------
    //  Hooks for overlays

    function _hook(x) {
        var h = current && current.hooks;
        return h && fs.isF(h[x]);
    }

    function escapeHook() {
        var eh = _hook('escape');
        return eh ? eh() : false;
    }

    function emptySelectHook() {
        var cb = _hook('empty');
        cb && cb();
    }

    function singleSelectHook(data) {
        var cb = _hook('single');
        cb && cb(data);
    }

    function multiSelectHook(selectOrder) {
        var cb = _hook('multi');
        cb && cb(selectOrder);
    }

    function mouseOverHook(what) {
        var cb = _hook('mouseover');
        cb && cb(what);
    }

    function mouseOutHook() {
        var cb = _hook('mouseout');
        cb && cb();
    }

    // NOTE: modifyLinkData (on classic topo) should not be necessary, as
    //       we should have a way of doing that server side

    // NOTE: while classic topology view persists, it should be the one to
    //       handle "visualization of intents" from intent view


    // === -----------------------------------------------------
    //  Event Handlers (events from server)

    function setOverlay(ovid) {
        var ov = overlays[ovid];
        if (!ov) {
            $log.error('setOverlay: no such overlay ID: ' + ovid);
        } else {
            current = ov;
            t2kcs.bindCommands(current.keyBindings);
        }
    }

    function showHighlights(data) {
        $log.info('+++ TOPO 2 +++ show highlights', data);
        t2lc.empty();
        var linkLabelsDOM = d3.select('.topo2-linkLabels');
        _.each(data.links, function (link) {
            // TODO: Inconsistent host id's (currentRegion and LinkLabel)
            var id = link.id.replace('/None/0', '/None').replace('-', '~'),
                nodeLink = t2rs.getLink(id);
                if (nodeLink) {
                    t2lc.addLabel(LinkLabel, link, linkLabelsDOM, {
                        link: nodeLink,
                    });
                }
        });
    }

    // ========================================================================

    angular.module('ovTopo2')
    .factory('Topo2OverlayService', [
        '$log', 'FnService', 'Topo2KeyCommandService',
        'Topo2RegionService', 'Topo2LabelCollection', 'Topo2LinkLabel',

        function (_$log_, _fs_, _t2kcs_, _t2rs_,
            _t2lc_, _t2ll_) {
            $log = _$log_;
            fs = _fs_;
            t2kcs = _t2kcs_;
            t2rs = _t2rs_;
            t2lc = _t2lc_;
            LinkLabel = _t2ll_;

            return {
                register: register,
                setOverlay: setOverlay,
                augmentRbset: augmentRbset,
                tbSelection: tbSelection,
                mkGlyphId: mkGlyphId,

                hooks: {
                    escape: escapeHook,
                    emptySelect: emptySelectHook,
                    singleSelect: singleSelectHook,
                    multiSelect: multiSelectHook,
                    mouseOver: mouseOverHook,
                    mouseOut: mouseOutHook,
                },
                showHighlights: showHighlights,
            };
        },
    ]);
}());
