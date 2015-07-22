// sample topology overlay - client side
(function () {
    'use strict';

    // injected refs
    var $log;

    // our overlay definition
    var overlay = {
        overlayId: 'sampleTopoOver',

        // NOTE: for the glyph, could alternately use id: <existingGlyphId>
        //       instead of defining viewbox and data (vb, d)
        //       glyph: { id: 'crown' }
        glyph: {
            vb: '0 0 8 8',
            d: 'M1,4l2,-1l1,-2l1,2l2,1l-2,1l-1,2l-1,-2z'
        },
        tooltip: 'Sample Topology Overlay',

        activate: activateOverlay,
        deactivate: deactivateOverlay
    };

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
