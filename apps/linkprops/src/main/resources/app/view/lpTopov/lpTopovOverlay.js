// Link props topology overlay - client side
//
// This is the glue that binds our business logic (in lpTopov.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, stds;

    // internal state should be kept in the service module (not here)

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'linkprops-overlay',
        glyphId: 'topo',
        tooltip: 'Link Properties',

        activate: function () {
            $log.debug("Link Props topology overlay ACTIVATED");
        },
        deactivate: function () {
            stds.stopDisplay();
            $log.debug("Link Props topology overlay DEACTIVATED");
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
                cb: function () { stds.startDisplay('rate'); },
                tt: 'Start Rate Mode',
                gid: 'ports'
            },
            F: {
                cb: function () { stds.startDisplay('byte'); },
                tt: 'Start Total Byte Mode',
                gid: 'allTraffic'
            },
            C: {
                cb: function () { stds.startDisplay('band'); },
                tt: 'Start Available Bandwidth Mode',
                gid: 'triangleUp'
            },
            G: {
                cb: buttonCallback,
                tt: 'Uses the G key',
                gid: 'crown'
            },

            _keyOrder: [
                '0', 'V', 'F', 'C', 'G'
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
    angular.module('ovLpTopov')
        .run(['$log', 'TopoOverlayService', 'LinkPropsTopovService',

        function (_$log_, _tov_, _stds_) {
            $log = _$log_;
            tov = _tov_;
            stds = _stds_;
            tov.register(overlay);
        }]);

}());