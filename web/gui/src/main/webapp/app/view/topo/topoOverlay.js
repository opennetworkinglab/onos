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
    var $log, fs, gs, wss, ns, tss, tps, api;

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

        $log.debug(tos + 'registered overlay: ' + id, overlay);
    }

    // TODO: remove this redundant code.......
    // NOTE: unregister needs to be called if an app is ever
    //       deactivated/uninstalled via the applications view
/*
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
    }
*/


    // returns the list of overlay identifiers
    function list() {
        return d3.map(overlays).keys();
    }

    // add a radio button for each registered overlay
    function augmentRbset(rset, switchFn) {
        angular.forEach(overlays, function (ov) {
            rset.push({
                gid: ov._glyphId,
                tooltip: (ov.tooltip || '(no tooltip)'),
                cb: function () {
                    tbSelection(ov.overlayId, switchFn);
                }
            });
        });
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

        if (!same) {
            current && doop('deactivate');
            current = overlays[id];
            current && doop('activate');
            actions = current && fs.isO(current.keyBindings);
            switchFn(id, actions);

            wss.sendEvent('topoSelectOverlay', payload);

            // Ensure summary and details panels are updated immediately..
            wss.sendEvent('requestSummary');
            tss.updateDetail();
        }
    }

    var coreButtons = {
        showDeviceView: {
            gid: 'switch',
            tt: 'Show Device View',
            path: 'device'
        },
        showFlowView: {
            gid: 'flowTable',
            tt: 'Show Flow View for this Device',
            path: 'flow'
        },
        showPortView: {
            gid: 'portTable',
            tt: 'Show Port View for this Device',
            path: 'port'
        },
        showGroupView: {
            gid: 'groupTable',
            tt: 'Show Group View for this Device',
            path: 'group'
        }
    };

    // retrieves a button definition from the current overlay and generates
    //  a button descriptor to be added to the panel, with the data baked in
    function _getButtonDef(id, data) {
        var btns = current && current.buttons,
            b = btns && btns[id],
            cb = fs.isF(b.cb),
            f = cb ? function () { cb(data); } : function () {};

        return b ? {
            id: current.mkId(id),
            gid: current.mkGid(b.gid),
            tt: b.tt,
            cb: f
        } : null;
    }

    // install core buttons, and include any additional from the current overlay
    function installButtons(buttons, data, devId) {
        buttons.forEach(function (id) {
            var btn = coreButtons[id],
                gid = btn && btn.gid,
                tt = btn && btn.tt,
                path = btn && btn.path;

            if (btn) {
                tps.addAction({
                    id: 'core-' + id,
                    gid: gid,
                    tt: tt,
                    cb: function () { ns.navTo(path, {devId: devId }); }
                });
            } else if (btn = _getButtonDef(id, data)) {
                tps.addAction(btn);
            }
        });
    }

    function addDetailButton(id) {
        var b = _getButtonDef(id);
        if (b) {
            tps.addAction({
                id: current.mkId(id),
                gid: current.mkGid(b.gid),
                cb: b.cb,
                tt: b.tt
            });
        }
    }


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

    // === -----------------------------------------------------
    //  Event (from server) Handlers

    function setApi(_api_, _tss_) {
        api = _api_;
        tss = _tss_;
    }

    function showHighlights(data) {
        var less;

        /*
           API to topoForce
             clearLinkTrafficStyle()
             removeLinkLabels()
             findLinkById( id )
             findNodeById( id )
             updateLinks()
             updateNodes()
             supLayers( bool, [less] )
             unsupNode( id, [less] )
             unsupLink( key, [less] )
         */

        // TODO: clear node highlighting
        api.clearLinkTrafficStyle();
        api.removeLinkLabels();

        // handle element suppression
        if (data.subdue) {
            less = data.subdue === 'min';
            api.supLayers(true, less);

        } else {
            api.supLayers(false);
            api.supLayers(false, true);
        }

        data.hosts.forEach(function (host) {
            var hdata = api.findNodeById(host.id);
            if (hdata && !hdata.el.empty()) {
                if (!host.subdue) {
                    api.unsupNode(hdata.id, less);
                }
                // TODO: further highlighting?
            }
        });

        data.devices.forEach(function (device) {
            var ddata = api.findNodeById(device.id);
            if (ddata && !ddata.el.empty()) {
                if (!device.subdue) {
                    api.unsupNode(ddata.id, less);
                }
                // TODO: further highlighting?
            }
        });

        data.links.forEach(function (link) {
            var ldata = api.findLinkById(link.id),
                lab = link.label,
                units, portcls, magnitude;

            if (ldata && !ldata.el.empty()) {
                if (!link.subdue) {
                    api.unsupLink(ldata.key, less);
                }
                ldata.el.classed(link.css, true);
                ldata.label = lab;

                // TODO: this needs to be pulled out into traffic overlay
                // inject additional styling for port-based traffic
                if (fs.endsWith(lab, 'bps')) {
                    units = lab.substring(lab.length-4);
                    portcls = 'port-traffic-' + units;

                    // for GBps
                    if (units.substring(0,1) === 'G') {
                        magnitude = fs.parseBitRate(lab);
                        if (magnitude >= 9) {
                            portcls += '-choked'
                        }
                    }
                    ldata.el.classed(portcls, true);
                }
            }
        });

        api.updateNodes();
        api.updateLinks();
    }

    // ========================================================================

    angular.module('ovTopo')
    .factory('TopoOverlayService',
        ['$log', 'FnService', 'GlyphService', 'WebSocketService', 'NavService',
            'TopoPanelService',

        function (_$log_, _fs_, _gs_, _wss_, _ns_, _tps_) {
            $log = _$log_;
            fs = _fs_;
            gs = _gs_;
            wss = _wss_;
            ns = _ns_;
            tps = _tps_;

            return {
                register: register,
                //unregister: unregister,
                setApi: setApi,
                list: list,
                augmentRbset: augmentRbset,
                mkGlyphId: mkGlyphId,
                tbSelection: tbSelection,
                installButtons: installButtons,
                addDetailButton: addDetailButton,
                hooks: {
                    escape: escapeHook,
                    emptySelect: emptySelectHook,
                    singleSelect: singleSelectHook,
                    multiSelect: multiSelectHook,
                    mouseOver: mouseOverHook,
                    mouseOut: mouseOutHook
                },

                showHighlights: showHighlights
            }
        }]);

}());