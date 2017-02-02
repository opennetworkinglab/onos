// path painter topology overlay - client side
//
// This is the glue that binds our business logic (in ppTopov.js)
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
        glyphId: 'm_topo',
        tooltip: 'Path Painter Overlay',

        activate: function () {
            $log.debug("Path painter topology overlay ACTIVATED");
        },
        deactivate: function () {
            pps.clear();
            $log.debug("Path painter topology overlay DEACTIVATED");
        },


        // detail panel button definitions
        buttons: {
            src: {
                gid: 'm_source',
                tt: 'Set source node',
                cb: function (data) {
                    $log.debug('Set src action invoked with data:', data);
                    pps.setSrc(selection);
                }
            },
            dst: {
                gid: 'm_destination',
                tt: 'Set destination node',
                cb: function (data) {
                    $log.debug('Set dst action invoked with data:', data);
                    pps.setDst(selection);
                }
            }
        },
        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        // FIXME: find better keys for shortest paths & disjoint paths modes
        keyBindings: {
            openBracket: {
                cb: function () {
                    pps.setSrc(selection);
                },
                tt: 'Set source node',
                gid: 'm_source'
            },
            closeBracket: {
                cb: function () {
                    pps.setDst(selection);
                },
                tt: 'Set destination node',
                gid: 'm_destination'
            },
            3: {
                cb: function () {
                    pps.swapSrcDst();
                },
                tt: 'Swap source and destination nodes',
                gid: 'm_swap'
            },
            4: {
                cb: function () {
                    pps.setMode("shortest");
                },
                tt: 'Set shortest paths mode',
                gid: 'm_shortestPath'
            },
            5: {
                cb: function () {
                    pps.setMode("disjoint");
                },
                tt: 'Set disjoint paths mode',
                gid: 'm_disjointPaths'
            },
            6: {
                cb: function () {
                    pps.setMode("geodata");
                },
                tt: 'Set geodata path weight mode',
                gid: 'm_shortestGeoPath'
            },
            leftArrow: {
                cb: function () {
                    pps.prevPath();
                },
                tt: 'Highlight previous path',
                gid: 'm_prev'
            },
            rightArrow: {
                cb: function () {
                    pps.nextPath();
                },
                tt: 'Highlight next path',
                gid: 'm_next'
            },
            0: {
                cb: function () {
                    pps.clear();
                },
                tt: 'Clear source and destination',
                gid: 'm_xMark'
            },

            _keyOrder: [
                'openBracket', 'closeBracket', '3', '4', '5', '6', 'leftArrow', 'rightArrow', '0'
            ]
        },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: function () {
                selectionCallback();
                return pps.clear();
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
