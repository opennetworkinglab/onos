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
                d: 'M28.7,59.3 M14.9,53 M8.7,39 M32.4,92.5H25l-0.2-3.6' +
                'c-0.5-9-5.4-23.9-12.9-33.5c-5.2-6.6-7-12.8-7-16.3c0-13.3,10.7-24,23.8-24c13.1,0,23.8,10.8,23.8,24c0,3.5-1.8,9.7-7,16.3' +
                'C38,65,33.1,80,32.6,88.9L32.4,92.5z M27.9,89.5h1.7l0-0.7c0.5-9.4,5.7-25.2,13.5-35.2c4.7-6,6.4-11.4,6.4-14.5' +
                'c0-11.6-9.3-21-20.8-21C17.3,18,7.9,27.5,7.9,39c0,3,1.7,8.4,6.4,14.5c7.9,10.1,13.1,25.8,13.5,35.2L27.9,89.5z M28.7,83.2' +
                'M28.6,29.8c-4.7,0-8.5,3.8-8.5,8.5c0,4.7,3.8,8.5,8.5,8.5s8.5-3.8,8.5-8.5C37.1,33.6,33.3,29.8,28.6,29.8z M89.6,47 M89.6,29.5' +
                'c-0.1,3.1-0.1,12.8,0,17c0.1,4.2,14.1-5.5,13.9-8.5C103.4,35.1,89.6,25.6,89.6,29.5z M51,38.1L89.5,38 M89.5,39.5l0-3L51,36.5l0,3' +
                'L89.5,39.5z'
            },
            dst: {
                vb: '0 0 110 110',
                d: 'M80.3,59.8 M85.8,92.5h-7.2L78.4,89c-0.4-8.8-5.2-23.6-12.3-33' +
                'c-4.9-6.5-6.7-12.5-6.7-16c0-13,10.2-23.7,22.7-23.7c12.5,0,22.7,10.6,22.7,23.7c0,3.5-1.8,9.5-6.7,16C91.2,65.4,86.4,80.1,86,89' +
                'L85.8,92.5z M81.4,89.5H83l0-0.7c0.5-9.3,5.4-24.8,12.9-34.7c4.5-5.9,6.1-11.2,6.1-14.2c0-11.4-8.9-20.7-19.8-20.7' +
                'c-10.9,0-19.8,9.3-19.8,20.7c0,3,1.6,8.3,6.1,14.2C76,64,80.9,79.5,81.4,88.8L81.4,89.5z M82.1,30.8c-4.5,0-8.1,3.7-8.1,8.4' +
                's3.6,8.4,8.1,8.4c4.5,0,8.1-3.7,8.1-8.4S86.6,30.8,82.1,30.8z M47.2,47.5 M45.2,30.8c-0.1,3.1-0.1,12.6,0,16.7' +
                'c0.1,4.1,13.4-5.4,13.3-8.4C58.4,36.2,45.2,26.9,45.2,30.8z M45.2,39.1L6.7,39.2 M45.2,40.6l0-3L6.7,37.7l0,3L45.2,40.6z'
            },
            jp: {
                vb: '0 0 110 110',
                d: 'M84.3,89.3L58.9,64.2l-1.4,1.4L83,90.7L84.3,89.3z M27,7.6H7.4v19.2H27V7.6z' +
                'M59.3,47.1H39.8v19.2h19.5V47.1z M102.1,79.5H82.6v19.2h19.5V79.5z M41.7,47.6L19,25.1l-1.2,1.2l22.7,22.5L41.7,47.6z'
            },
            djp: {
                vb: '0 0 110 110',
                d: 'M25.8,84l-9.2-57 M27.3,83.8l-9.2-57l-3,0.5l9.2,57L27.3,83.8z M83.2,37.7L26.8,15.5 M83.7,36.1L26.6,14' +
                'l-1,3.2l57,22.1L83.7,36.1z M34.1,95l61.4-40.6 M96.4,55.7l-1.9-2.5L33.2,93.8l1.9,2.5L96.4,55.7z M26.6,27.6H6.7V7.7h19.9V27.6z' +
                'M102.1,36H82.2v19.9h19.9V36z M35.3,83.5H15.3v19.9h19.9V83.5z'
            }

        },

        activate: function () {
            $log.debug("Path painter topology overlay ACTIVATED");
        },
        deactivate: function () {
            $log.debug("Path painter topology overlay DEACTIVATED");
        },

        // detail panel button definitions
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
