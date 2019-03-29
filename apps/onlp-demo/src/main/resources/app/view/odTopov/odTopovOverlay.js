// path painter topology overlay - client side
//
// This is the glue that binds our business logic (in ppTopov.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, ns, tts, lts, sel;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#ttrafov#' + x + '#';
    };

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
            },
            showDeviceFlows: {
                gid: 'm_flows',
                tt: function () { return topoLion('tr_btn_show_device_flows'); },
                cb: function (data) { tts.showDeviceLinkFlows(); },
            },
        },

        // key bindings for traffic overlay toolbar buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            0: {
                cb: function () { tts.cancelTraffic(true); },
                tt: function () { return topoLion('tr_btn_cancel_monitoring'); },
                gid: 'm_xMark',
            },

            A: {
                cb: function () { tts.showAllTraffic(); },
                tt: function () { return topoLion('tr_btn_monitor_all'); },
                gid: 'm_allTraffic',
            },
            F: {
                cb: function () { tts.showDeviceLinkFlows(); },
                tt: function () { return topoLion('tr_btn_show_dev_link_flows'); },
                gid: 'm_flows',
            },

            _keyOrder: [
                '0', 'A', 'F',
            ],
        },

        hooks: {
            // hook for handling escape key
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return tts.cancelTraffic(true);
            },

            // hooks for when the selection changes...
            empty: function () {
                tts.cancelTraffic();
            },
            // hooks for when the selection changes...
            single: function (data) {
                $log.debug('selection data:', data);
                sel = data;
                tov.addDetailButton('showOnlpView');
                tov.addDetailButton('showDeviceFlows');
                tts.requestTrafficForMode();
            },
            multi: function (selectOrder) {
                tts.requestTrafficForMode();
                tov.addDetailButton('showRelatedTraffic');
            },

            // mouse hooks
            mouseover: function (m) {
                // m has id, class, and type properties
                tts.requestTrafficForMode(true);
            },
            mouseout: function () {
                tts.requestTrafficForMode(true);
            },

            // localization bundle injection hook
            injectLion: function (bundle) {
                topoLion = bundle;
                tts.setLionBundle(bundle);
            },
        },
    };

    // invoke code to register with the overlay service
    angular.module('ovOdTopov')
        .run(['$log', 'TopoOverlayService', 'NavService', 'TopoTrafficService',
              'OnlpDemoTopovService',

            function (_$log_, _tov_, _ns_, _tts_, _lts_) {
                $log = _$log_;
                tov = _tov_;
                ns = _ns_;
                tts = _tts_;
                lts = _lts_;
                tov.register(overlay);
            }]);
}());
