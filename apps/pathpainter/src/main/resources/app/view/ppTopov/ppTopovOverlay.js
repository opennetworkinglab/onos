// path painter topology overlay - client side
//
// This is the glue that binds our business logic (in ppTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, pps;

    // internal state should be kept in the service module (not here)
    var selection;

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'pp-overlay',
        glyphId: 'topo',
        tooltip: 'Path Painter Topo Overlay',

        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'star4' is installed as 'meowster-overlay-star4'
        // They can be referenced (from this overlay) as '*star4'
        // That is, the '*' prefix stands in for 'meowster-overlay-'
        glyphs: {
            star4: {
                vb: '0 0 8 8',
                d: 'M1,4l2,-1l1,-2l1,2l2,1l-2,1l-1,2l-1,-2z'
            },
            banner: {
                vb: '0 0 6 6',
                d: 'M1,1v4l2,-2l2,2v-4z'
            }
        },

        activate: function () {
            $log.debug("Path painter topology overlay ACTIVATED");
        },
        deactivate: function () {
            $log.debug("Path painter topology overlay DEACTIVATED");
        },

        // detail panel button definitions
        // FIXME: new icons for src/dst
        buttons: {
            src: {
                gid: 'triangleUp',
                tt: 'Set source node',
                cb: function (data) {
                    $log.debug('Set src action invoked with data:', data);
                    pps.setSrc(selection);
                }
            },
            dst: {
                gid: 'triangleDown',
                tt: 'Set destination node',
                cb: function (data) {
                    $log.debug('Set dst action invoked with data:', data);
                    pps.setDst(selection);
                }
            }
        },

        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        // FIXME: use into [ and ] instead of 1 and 2
        // FIXME: new icons for src/dst
        // TODO: add keys for shortest paths & disjoint paths modes
        keyBindings: {
            1: {
                cb: function () { pps.setSrc(selection); },
                tt: 'Set source node',
                gid: 'triangleUp'
            },
            2: {
                cb: function () { pps.setDst(selection); },
                tt: 'Set destination node',
                gid: 'triangleDown'
            },
            3: {
                cb: function () { pps.swapSrcDst(); },
                tt: 'Swap source and destination nodes',
                gid: 'refresh'
            },
            leftArrow: {
                cb: function () { pps.prevPath(); },
                tt: 'Highlight previous path',
                gid: 'prevIntent'
            },
            rightArrow: {
                cb: function () { pps.nextPath(); },
                tt: 'Highlight next path',
                gid: 'nextIntent'
            },

            _keyOrder: [
                '1', '2', '3', 'leftArrow', 'rightArrow'
            ]
        },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: function () {
                selectionCallback();
                pps.setSrc();
                pps.setDst();
            },

            // hooks for when the selection changes...
            empty: function () {
                selectionCallback();
            },
            single: function (data) {
                selectionCallback(data);
            }
        }
    };


    function buttonCallback(x) {
        $log.debug('Toolbar-button callback', x);
    }

    function selectionCallback(d) {
        $log.debug('Selection callback', d);
        selection = d;
    }

    // invoke code to register with the overlay service
    angular.module('ovPpTopov')
        .run(['$log', 'TopoOverlayService', 'PathPainterTopovService',

        function (_$log_, _tov_, _pps_) {
            $log = _$log_;
            tov = _tov_;
            pps = _pps_;
            tov.register(overlay);
        }]);

}());
