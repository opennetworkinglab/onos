// alarm topology overlay - client side
//
// This is the glue that binds our business logic (in alarmTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, stds, ns;

    // internal state should be kept in the service module (not here)

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'alarmsTopo-overlay',
        glyphId: '*star4',
        tooltip: 'Alarms Overlay',
        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'star4' is installed as 'alarmsTopo-overlay-star4'
        // They can be referenced (from this overlay) as '*star4'
        // That is, the '*' prefix stands in for 'alarmsTopo-overlay-'
        glyphs: {
            star4: {
                vb: '0 0 8 8',
                // TODO new icon needed
                d: 'M1,4l2,-1l1,-2l1,2l2,1l-2,1l-1,2l-1,-2z'
            },
            banner: {
                vb: '0 0 6 6',
                // TODO new icon needed
                d: 'M1,1v4l2,-2l2,2v-4z'
            }
        },
        activate: function () {
            $log.debug("Alarm topology overlay ACTIVATED");
        },
        deactivate: function () {
            stds.stopDisplay();
            $log.debug("Alarm topology overlay DEACTIVATED");
        },
        // detail panel button definitions
        buttons: {
            alarm1button: {
                gid: 'chain',
                tt: 'Show alarms for this device',
                cb: function (data) {
                    $log.debug('Show alarms for selected device. data:', data);
                    ns.navTo("alarmTable", {devId: data.id});

                }
            },
            alarm2button: {
                gid: '*banner',
                tt: 'Show alarms for all devices',
                cb: function (data) {
                    $log.debug('Show alarms for all devices. data:', data);
                    ns.navTo("alarmTable");

                }
            }
        },
        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            0: {
                cb: function () {
                    stds.stopDisplay();
                },
                tt: 'Cancel Alarm Count on Device',
                gid: 'xMark'
            },
            V: {
                cb: function () {
                    stds.startDisplay('mouse');
                },
                tt: 'Start Alarm Count on Device',
                gid: '*banner'
            },
            _keyOrder: [
                '0', 'V'
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
                tov.addDetailButton('alarm1button');
                tov.addDetailButton('alarm2button');
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
    angular.module('ovAlarmTopov')
            .run(['$log', 'TopoOverlayService', 'AlarmTopovDemoService', 'NavService',
                function (_$log_, _tov_, _stds_, _ns_) {
                    $log = _$log_;
                    tov = _tov_;
                    stds = _stds_;
                    ns = _ns_;
                    tov.register(overlay);
                }]);

}());
