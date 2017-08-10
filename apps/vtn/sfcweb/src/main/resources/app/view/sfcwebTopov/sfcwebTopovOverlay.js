// sfcweb topology overlay - client side
//
// This is the glue that binds our business logic (in sfcwebTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, pps;
    var longPrefix = 'M15.9,19.1h-8v-13h8V19.1z M90.5,6.1H75.6v13h14.9V6.1' +
            'z M71.9,6.1H56.9v13h14.9V6.1z M53.2,6.1H38.3v13h14.9V6.1z M34.5,' +
            '6.1H19.6v13h14.9V6.1z M102.2,6.1h-8v13h8V6.1z' ;

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in SfcWebUiTopovOverlay
        overlayId: 'SFC-Service-overlay',
        glyphId: '*star4',
        tooltip: 'SFC web service Topo Overlay',

        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'star4' is installed as 'meowster-overlay-star4'
        // They can be referenced (from this overlay) as '*star4'
        // That is, the '*' prefix stands in for 'meowster-overlay-'
        glyphs: {
            star4: {
                vb: '0 0 8 8',
                d: 'M1,4l2,-1l1,-2l1,2l2,1l-2,1l-1,2l-1,-2z'
            }
        },
        activate: function () {
            $log.debug("SFC service topology overlay ACTIVATED");
        },
        deactivate: function () {
            pps.clear();
            $log.debug("SFC service topology overlay DEACTIVATED");
        },

        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        // FIXME: find better keys for shortest paths & disjoint paths modes
        keyBindings: {
            4: {
                cb: function () {
                    pps.start();
                },
                tt: 'Query SFP active list information',
                gid: 'summary'
            },

            _keyOrder: [
                '4'
            ]
        }

    };

    // invoke code to register with the overlay service
    angular.module('ovSfcwebTopov')
        .run(['$log', 'TopoOverlayService', 'SfcwebTopovDemoService',

        function (_$log_, _tov_, _pps_) {
            $log = _$log_;
            tov = _tov_;
            pps = _pps_;
            tov.register(overlay);
            $log.debug('ovSfcwebTopov run');
        }]);

}());
