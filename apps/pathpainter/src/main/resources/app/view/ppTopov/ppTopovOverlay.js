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
        // e.g. 'src' is installed as 'pp-overlay-src'
        // They can be referenced (from this overlay) as '*src'
        // That is, the '*' prefix stands in for 'pp-overlay-'
        glyphs: {
            src: {
                vb: '0 0 110 110',
                d: 'M26.9,57 M27,86c0.5-9.6,5.8-25.3,13.6-35.2c4.2-5.4,6.2-10.6,6.2-13.8' +
                'c0-11-8.9-20-19.9-20h0C15.9,17,7,26,7,37c0,3.2,1.9,8.4,6.2,13.8c7.8,9.9,13.1,25.5,13.6,35.2H27z M27.7,86.7l0-0.7' +
                'c0.5-9.3,5.6-24.8,13.4-34.7c4.7-5.9,6.3-11.3,6.3-14.3c0-11.4-9.3-20.7-20.6-20.7C15.5,16.3,6.2,25.6,6.2,37c0,3,1.7,8.3,6.3,14.3' +
                'c7.8,9.9,13,25.5,13.4,34.7l0,0.7H27.7L27.7,86.7z M26.9,17.8C37.4,17.8,46,26.4,46,37c0,2.6-1.6,7.7-6,13.3' +
                'c-6.6,8.4-11.4,20.8-13.1,30.2c-1.7-9.4-6.5-21.8-13.1-30.2c-4.4-5.6-6-10.7-6-13.3C7.7,26.4,16.3,17.8,26.9,17.8L26.9,17.8z' +
                'M87.3,35.5H46.9v2h40.4V35.5z M26.8,27.9c-4.7,0-8.4,3.8-8.4,8.4c0,4.6,3.8,8.4,8.4,8.4s8.4-3.7,8.4-8.4' +
                'C35.2,31.6,31.4,27.9,26.8,27.9z M87.3,44.9 M87.3,28.1c-0.1,3.1-0.1,12.6,0,16.7c0.1,4.1,13.9-5.5,13.8-8.4' +
                'C101,33.6,87.3,24.2,87.3,28.1z'
            },
            dst: {
                vb: '0 0 110 110',
                d: 'M80.6,57 M80.7,86c0.5-9.6,5.8-25.3,13.5-35.2c4.2-5.4,6.1-10.6,6.1-13.8' +
                'c0-11-8.8-20-19.7-20h0C69.7,17,60.8,26,60.8,37c0,3.2,1.9,8.4,6.1,13.8c7.7,9.9,13,25.5,13.5,35.2H80.7z M81.4,86.7l0-0.7' +
                'c0.5-9.3,5.6-24.8,13.4-34.7c4.6-5.9,6.3-11.3,6.3-14.3c0-11.4-9.2-20.7-20.5-20.7S60.1,25.6,60.1,37c0,3,1.7,8.3,6.3,14.3' +
                'c7.8,9.9,12.9,25.5,13.4,34.7l0,0.7H81.4L81.4,86.7z M80.6,17.8c10.5,0,19,8.6,19,19.2c0,2.6-1.6,7.7-6,13.3' +
                'c-6.6,8.4-11.3,20.8-13,30.2c-1.7-9.4-6.4-21.8-13-30.2c-4.4-5.6-6-10.7-6-13.3C61.6,26.4,70.1,17.8,80.6,17.8L80.6,17.8z' +
                'M46.3,35.2H6.2v2h40.1V35.2z M80.5,27.9c-4.6,0-8.4,3.7-8.4,8.4c0,4.6,3.7,8.4,8.4,8.4s8.4-3.7,8.4-8.4' +
                'C88.9,31.6,85.1,27.9,80.5,27.9z M46.3,44.6 M46.3,27.9c-0.1,3.1-0.1,12.6,0,16.7c0.1,4.1,13.9-5.5,13.7-8.4' +
                'C60,33.3,46.3,24,46.3,27.9z'
            },
            jp: {
                vb: '0 0 110 110',
                d: 'M27,26.8H7.4V7.6H27V26.8z M102.1,79.5H82.6v19.2h19.5V79.5z M59.3,47.1H39.8v19.2' +
                'h19.5V47.1z M41.7,47.5L17.1,25.2l-1.3,1.4l24.6,22.3L41.7,47.5z M84.5,89.5L59,64.3l-1.4,1.4l25.4,25.2L84.5,89.5z'
            },
            djp: {
                vb: '0 0 110 110',
                d: 'M27.6,28.8H7.7V9h19.9V28.8z M103,37.2H83.1V57H103V37.2z M36.2,84.5H16.3v19.9' +
                'h19.9V84.5z M27.9,85.7L18.7,28l-2,0.3L25.9,86L27.9,85.7z M92.2,59.1L91,57.4L34.6,96.8l1.1,1.6L92.2,59.1z M28.5,80.7' +
                'c-0.8,0.2-3.5,0.7-4.6,1c-1.1,0.3,2.2,3.1,3,2.9S29.6,80.5,28.5,80.7z M88.8,57.5c0.5,0.7,2,2.9,2.7,3.8c0.7,0.9,2-3.2,1.5-3.9' +
                'C92.5,56.8,88.2,56.6,88.8,57.5z M97.7,42.5L27.2,17.9l-0.7,1.9l70.5,24.6L97.7,42.5z M30.7,22.6c0.3-0.8,1.2-3.3,1.5-4.4' +
                'c0.4-1.1-3.8,0.3-4,1.1C27.9,20.1,30.3,23.7,30.7,22.6z'
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
                gid: '*src',
                tt: 'Set source node',
                cb: function (data) {
                    $log.debug('Set src action invoked with data:', data);
                    pps.setSrc(selection);
                }
            },
            dst: {
                gid: '*dst',
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
        // FIXME: find better keys for shortest paths & disjoint paths modes
        keyBindings: {
            1: {
                cb: function () {
                    pps.setSrc(selection);
                },
                tt: 'Set source node',
                gid: '*src'
            },
            2: {
                cb: function () {
                    pps.setDst(selection);
                },
                tt: 'Set destination node',
                gid: '*dst'
            },
            3: {
                cb: function () {
                    pps.swapSrcDst();
                },
                tt: 'Swap source and destination nodes',
                gid: 'refresh'
            },
            4: {
                cb: function () {
                    pps.setMode("shortest");
                },
                tt: 'Set shortest paths mode',
                gid: '*jp'
            },
            5: {
                cb: function () {
                    pps.setMode("disjoint");
                },
                tt: 'Set disjoint paths mode',
                gid: '*djp'
            },
            leftArrow: {
                cb: function () {
                    pps.prevPath();
                },
                tt: 'Highlight previous path',
                gid: 'prevIntent'
            },
            rightArrow: {
                cb: function () {
                    pps.nextPath();
                },
                tt: 'Highlight next path',
                gid: 'nextIntent'
            },

            _keyOrder: [
                '1', '2', '3', '4', '5', 'leftArrow', 'rightArrow'
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
