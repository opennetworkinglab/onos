// path painter topology overlay - client side
//
// This is the glue that binds our business logic (in ppTopov.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, ns, lts, sel;

    // our overlay definition
    var overlay = {
        overlayId: 'od-overlay',
        glyphId: 'm_disjointPaths',
        tooltip: 'ONLP Data Overlay',

        activate: function () {
            $log.debug("ONLP data topology overlay ACTIVATED");
        },
        deactivate: function () {
            lts.clear();
            $log.debug("ONLP data topology overlay DEACTIVATED");
        },

        // detail panel button definitions
        buttons: {
            showOnlpView: {
                gid: 'chain',
                tt: 'ONLP data',
                cb: function (data) {
                    $log.debug('ONLP action invoked on selection:', sel);
                    ns.navTo('onlp', { devId: sel.id });
                }
            }
        },

        hooks: {
            // hooks for when the selection changes...
            single: function (data) {
                $log.debug('selection data:', data);
                sel = data;
                tov.addDetailButton('showOnlpView');
            }
        }
    };

    // invoke code to register with the overlay service
    angular.module('ovOdTopov')
        .run(['$log', 'TopoOverlayService', 'NavService', 'OnlpDemoTopovService',

            function (_$log_, _tov_, _ns_, _lts_) {
                $log = _$log_;
                tov = _tov_;
                ns = _ns_;
                lts = _lts_;
                tov.register(overlay);
            }]);
}());
