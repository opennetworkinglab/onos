// sample topology overlay - client side
//
// This is the glue that binds our business logic (in sampleTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, stds;

    // internal state should be kept in the service module (not here)

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
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
            $log.debug("Sample topology overlay ACTIVATED");
        },
        deactivate: function () {
            stds.stopDisplay();
            $log.debug("Sample topology overlay DEACTIVATED");
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
            0: {
                cb: function () { stds.stopDisplay(); },
                tt: 'Cancel Display Mode',
                gid: 'xMark'
            },
            V: {
                cb: function () { stds.startDisplay('mouse'); },
                tt: 'Start Mouse Mode',
                gid: '*banner'
            },
            F: {
                cb: function () { stds.startDisplay('link'); },
                tt: 'Start Link Mode',
                gid: 'chain'
            },
            G: {
                cb: buttonCallback,
                tt: 'Uses the G key',
                gid: 'crown'
            },

            _keyOrder: [
                '0', 'V', 'F', 'G'
            ]
        },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return stds.stopDisplay();
            },

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
            },
            mouseover: function (m) {
                // m has id, class, and type properties
                $log.debug('mouseover:', m);
                stds.updateDisplay(m);
            },
            mouseout: function () {
                $log.debug('mouseout');
                stds.updateDisplay();
            }
        }
    };


    function buttonCallback(x) {
        $log.debug('Toolbar-button callback', x);
    }

    function selectionCallback(x, d) {
        $log.debug('Selection callback', x, d);
    }

    // invoke code to register with the overlay service
    angular.module('ovSampleTopov')
        .run(['$log', 'TopoOverlayService', 'SampleTopovDemoService',

        function (_$log_, _tov_, _stds_) {
            $log = _$log_;
            tov = _tov_;
            stds = _stds_;
            tov.register(overlay);
        }]);

}());
