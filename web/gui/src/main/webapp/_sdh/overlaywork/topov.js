// sample topology overlay - client side
(function () {
    'use strict';

    // injected refs
    var $log, tov;

    // internal state
    var someStateValue = true;

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopoOverlay
        overlayId: 'meowster-overlay',
        glyphId: '*star4',
        tooltip: 'Sample Meowster Topo Overlay',

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
            $log.debug("sample topology overlay ACTIVATED");
        },
        deactivate: function () {
            $log.debug("sample topology overlay DEACTIVATED");
        },





        // detail panel button definitions
        buttons: {
            foo: {
                gid: 'chain',
                tt: 'A FOO action',
                cb: function (data) {
                    $log.debug('FOO action invoked with data:', data);
                }
            },
            bar: {
                gid: '*banner',
                tt: 'A BAR action',
                cb: function (data) {
                    $log.debug('BAR action invoked with data:', data);
                }
            }
        },

        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            V: {
                cb: buttonCallback,
                tt: 'Uses the V key',
                gid: '*banner'
            },
            F: {
                cb: buttonCallback,
                tt: 'Uses the F key',
                gid: 'chain'
            },
            G: {
                cb: buttonCallback,
                tt: 'Uses the G key',
                gid: 'crown'
            },

            T: {
                cb: buttonCallback,
                tt: 'Uses the T key',
                gid: 'switch'
            },

            R: {
                cb: buttonCallback,
                tt: 'Uses the R key',
                gid: 'endstation'
            },

            0: {
                cb: buttonCallback,
                tt: 'Uses the ZERO key',
                gid: 'xMark'
            },

            _keyOrder: [
                '0', 'V', 'F', 'G', 'T', 'R'
            ]

            // NOTE: T and R should be rejected (not installed)
            //       T is reserved for 'toggle Theme'
            //       R is reserved for 'Reset pan and zoom'
        },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: cancelState,

            // hooks for when the selection changes...
            empty: function () {
                selectionCallback('empty');
            },
            single: function (data) {
                selectionCallback('single', data);
            },
            multi: function (selectOrder) {
                selectionCallback('multi', selectOrder);
                tov.addDetailButton('foo');
                tov.addDetailButton('bar');
            }
        }

    };

    // invoked when the escape key is pressed
    function cancelState() {
        if (someStateValue) {
            someStateValue = false;
            // we consumed the ESC event
            return true;
        }
        return false;
    }

    function buttonCallback(x) {
        $log.debug('Toolbar-button callback', x);
    }

    function selectionCallback(x, d) {
        $log.debug('Selection callback', x, d);
    }

    // invoke code to register with the overlay service
    angular.module('ovSample')
        .run(['$log', 'TopoOverlayService',

            function (_$log_, _tov_) {
                $log = _$log_;
                tov = _tov_;
                tov.register(overlay);
            }]);

}());
