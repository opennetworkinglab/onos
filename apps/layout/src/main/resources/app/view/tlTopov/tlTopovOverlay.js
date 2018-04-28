// path painter topology overlay - client side
//
// This is the glue that binds our business logic (in ppTopov.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, lts;

    // our overlay definition
    var overlay = {
        overlayId: 'tl-overlay',
        glyphId: 'm_disjointPaths',
        tooltip: 'Algorithmic Layout Overlay',

        activate: function () {
            $log.debug("Layout topology overlay ACTIVATED");
        },
        deactivate: function () {
            lts.clear();
            $log.debug("Layout topology overlay DEACTIVATED");
        },

        keyBindings: {
            0: {
                cb: function () {
                    lts.doLayout('default', 'Default (force-based) Layout');
                },
                tt: 'Default (force-based) layout',
                gid: 'm_fiberSwitch'
            },
            1: {
                cb: function () {
                    lts.doLayout('access', 'Access Network Layout - separate service leafs');
                },
                tt: 'Access layout - separate service leafs',
                gid: 'm_disjointPaths'
            },

            _keyOrder: [
                '0', '1'
            ]
        }
    };

    // invoke code to register with the overlay service
    angular.module('ovTlTopov')
        .run(['$log', 'TopoOverlayService', 'LayoutTopovService',

            function (_$log_, _tov_, _lts_) {
                $log = _$log_;
                tov = _tov_;
                lts = _lts_;
                tov.register(overlay);
            }]);
}());
