// alarm topology overlay - client side
//
// This is the glue that binds our business logic (in alarmTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, atds, ns;

    // internal state should be kept in the service module (not here)

    var viewbox = '0 0 110 110';

    // alarm clock glyph (view box 110x110)
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

    // overlapping alarm clocks glyph (view box 110x110)
    var clocks = "M65.7,26.6A34.7,34.7,0,0,0,31,61.5c0,19,16,35.2,35.5,34.8" +
        "a35.1,35.1,0,0,0,34-35.1A34.7,34.7,0,0,0,65.7,26.6Z" +
        "m8.6-2.7,27.4,16.4a31.3,31.3,0,0,0,1.2-3.1c1.3-5,0-9.4-3.2-13.2" +
        "a16.9,16.9,0,0,0-16-6.2A12.8,12.8,0,0,0,74.3,23.9Z" +
        "M57,23.9L56.4,23a12.9,12.9,0,0,0-10.7-5.5,16.9,16.9,0,0,0-15.1,8," +
        "14.1,14.1,0,0,0-2.3,11.2,10.4,10.4,0,0,0,1.4,3.5Z" +
        "M26.9,31.6A33.7,33.7,0,0,0,9.7,59.3C9.2,72.8,14.9,83.1,26,90.7l1-1.9" +
        "a0.6,0.6,0,0,0-.2-1A34.2,34.2,0,0,1,15.5,50.4" +
        "a33.8,33.8,0,0,1,10.8-16,1.2,1.2,0,0,0,.5-0.6" +
        "C26.9,33.1,26.9,32.4,26.9,31.6Z" +
        "m1,8.1C14.6,55.9,19.2,81,37.6,91.1l1-2.3-2.8-2.4" +
        "C26.4,77.6,22.9,66.7,25.1,54" +
        "a31.6,31.6,0,0,1,4.2-10.8,0.8,0.8,0,0,0,.1-0.6Z" +
        "M12,38.4a11.2,11.2,0,0,1-1.4-5.8A13.7,13.7,0,0,1,14.3,24" +
        "a17.3,17.3,0,0,1,10.5-5.7h0.4C19,18,13.7,20,9.9,25.2" +
        "a14.5,14.5,0,0,0-3,11,11.2,11.2,0,0,0,1.6,4.3Z" +
        "m5.5-2.7L21,33" +
        "a1,1,0,0,0,.3-0.7,14,14,0,0,1,3.9-8.6,17.3,17.3,0,0,1,10.2-5.4" +
        "l0.6-.2C24.4,17.3,16.4,27,17.4,35.7Z" +
        "M70.9,17.2H60.7v4.1h4v4.2H67V21.3h3.9V17.2Z" +
        "M90.9,87.9l-0.5.3L86,91.5a7.9,7.9,0,0,0-2.6,3.1" +
        "c-0.3.6-.2,0.8,0.5,0.9a27.9,27.9,0,0,0,6.8.2l1.6-.4" +
        "a0.8,0.8,0,0,0,.6-1.2l-0.4-1.4Z" +
        "m-50.2,0-1.8,6c-0.3,1.1-.1,1.4.9,1.7h0.7a26.3,26.3,0,0,0,7.2-.1" +
        "c0.8-.1.9-0.4,0.5-1.1a8.2,8.2,0,0,0-2.7-3.1Z" +
        "m-10.8-.4-0.2.6L28,93.5a0.9,0.9,0,0,0,.7,1.3,7.7,7.7,0,0,0,2.2.4" +
        "l5.9-.2c0.5,0,.7-0.3.5-0.8a7.6,7.6,0,0,0-2.2-2.9Z" +
        "m-11.3,0-0.2.7-1.6,5.4c-0.2.8-.1,1.2,0.7,1.4a8,8,0,0,0,2.2.4l6-.2" +
        "c0.4,0,.7-0.3.5-0.6a7.1,7.1,0,0,0-1.9-2.7l-2.8-2.1Z" +
        "M65.7,26.6m-2,30.3a4.4,4.4,0,0,0-2.2,2,4.8,4.8,0,0,0,4,7.2," +
        "4.1,4.1,0,0,0,4.3-2.3,0.8,0.8,0,0,1,.8-0.5H84.1" +
        "a1.9,1.9,0,0,0,2-1.7,2.1,2.1,0,0,0-1.7-2.2H70.8" +
        "a1,1,0,0,1-1-.5,6.4,6.4,0,0,0-1.5-1.6,1.1,1.1,0,0,1-.5-1" +
        "q0-10.1,0-20.3a1.9,1.9,0,0,0-2.2-2.1,2.1,2.1,0,0,0-1.8,2.2" +
        "q0,6.1,0,12.2C63.7,51.2,63.7,54,63.7,56.9Z";

    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'alarmsTopo-overlay',
        glyphId: '*clock',
        tooltip: 'Alarms Overlay',
        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'clock' is installed as 'alarmsTopo-overlay-clock'
        // They can be referenced (from this overlay) as '*clock'
        // That is, the '*' prefix stands in for 'alarmsTopo-overlay-'
        glyphs: {
            clock: {
                vb: viewbox,
                d: clock
            },
            clocks: {
                vb: viewbox,
                d: clocks
            }
        },
        activate: function () {
            $log.debug("Alarm topology overlay ACTIVATED");
            atds.startDisplay();
        },
        deactivate: function () {
            atds.stopDisplay();
            $log.debug("Alarm topology overlay DEACTIVATED");
        },
        // detail panel button definitions
        buttons: {
            alarm1button: {
                gid: '*clock',
                tt: 'Show alarms for this device',
                cb: function (data) {
                    $log.debug('Show alarms for selected device. data:', data);
                    ns.navTo("alarmTable", {devId: data.id});
                }
            }
        },
        hooks: {
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
            function (_$log_, _tov_, _atds_, _ns_) {
                $log = _$log_;
                tov = _tov_;
                atds = _atds_;
                ns = _ns_;
                tov.register(overlay);
            }]);

}());
