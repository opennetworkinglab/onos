// sample topology overlay - client side
(function () {
    'use strict';

    // injected refs
    var $log;

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

        activate: activateOverlay,
        deactivate: deactivateOverlay,

        // button descriptors - these can be added to overview or detail panels
        buttons: {
            foo: {
                gid: 'chain',
                tt: 'a FOO action',
                cb: fooCb
            },
            bar: {
                gid: '*banner',
                tt: 'a BAR action',
                cb: barCb
            }
        }
    };

    function fooCb(data) {
        $log.debug('FOO callback with data:', data);
    }

    function barCb(data) {
        $log.debug('BAR callback with data:', data);
    }

    // === implementation of overlay API (essentially callbacks)
    function activateOverlay() {
        $log.debug("sample topology overlay ACTIVATED");
    }

    function deactivateOverlay() {
        $log.debug("sample topology overlay DEACTIVATED");
    }


    // invoke code to register with the overlay service
    angular.module('ovSample')
        .run(['$log', 'TopoOverlayService',

            function (_$log_, tov) {
                $log = _$log_;
                tov.register(overlay);
            }]);

}());
