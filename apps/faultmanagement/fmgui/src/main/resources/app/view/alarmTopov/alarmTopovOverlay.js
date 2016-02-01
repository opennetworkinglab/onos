// alarm topology overlay - client side
//
// This is the glue that binds our business logic (in alarmTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, stds, ns;

    // internal state should be kept in the service module (not here)

    // alarm clock glyph (vbox 110x110)
    var clock = 'M92.9,61.3a39,39,0,1,1-39-39,39,39,0,0,1,39,39h0Z' +
        'M44,19.3c-4.4-7.4-14.8-9.3-23.2-4.2S9.1,30.2,13.5,37.6m80.8,0' +
        'c4.4-7.4,1.2-17.5-7.3-22.5s-18.8-3.2-23.3,4.2m-8.4,1.8V16.5h4.4' +
        'V11.9H48.2v4.6h4.6v4.6M51.6,56.4H51.5' +
        'a5.4,5.4,0,0,0,2.4,10.3,4.7,4.7,0,0,0,4.9-3.1H74.5' +
        'a2.2,2.2,0,0,0,2.4-2.2,2.4,2.4,0,0,0-2.4-2.3H58.8' +
        'a5.3,5.3,0,0,0-2.5-2.6H56.2V32.9' +
        'a2.3,2.3,0,0,0-.6-1.7,2.2,2.2,0,0,0-1.6-.7,2.4,2.4,0,0,0-2.4,2.4' +
        'h0V56.4M82.2,91.1l-7.1,5.3-0.2.2-1.2,2.1a0.6,0.6,0,0,0,.2.8' +
        'h0.2c2.6,0.4,10.7.9,10.3-1.2m-60.8,0c-0.4,2.1,7.7,1.6,10.3,1.2' +
        'h0.2a0.6,0.6,0,0,0,.2-0.8l-1.2-2.1-0.2-.2-7.1-5.3';

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'alarmsTopo-overlay',
        glyphId: '*clock',
        tooltip: 'Alarms Overlay',
        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'star4' is installed as 'alarmsTopo-overlay-star4'
        // They can be referenced (from this overlay) as '*star4'
        // That is, the '*' prefix stands in for 'alarmsTopo-overlay-'
        glyphs: {
            clock: {
                vb: '0 0 110 110',
                d: clock
            },
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
