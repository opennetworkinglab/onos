// js for alarm app table view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, $loc, devId, fs, wss;

    // constants
    var detailsReq = 'alarmTableDetailsRequest',
            detailsResp = 'alarmTableDetailsResponse',
            pName = 'ov-alarm-table-item-details-panel',
            propOrder = [
                'id', 'alarmDeviceId', 'alarmDesc', 'alarmSource',
                'alarmTimeRaised', 'alarmTimeUpdated', 'alarmTimeCleared',
                'alarmSeverity'
            ],
            friendlyProps = [
                'Alarm Id', 'Device Id', 'Description', 'Source',
                'Time Raised', 'Time Updated', 'Time Cleared', 'Severity'
            ];


    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function populatePanel(panel) {
        var title = panel.append('h3'),
                tbody = panel.append('table').append('tbody');

        title.text('Alarm Details');

        propOrder.forEach(function (prop, i) {
            addProp(tbody, i, $scope.panelDetails[prop]);
        });

        panel.append('hr');
        panel.append('h4').text('Comments');
        panel.append('p').text($scope.panelDetails.comment);
    }

    function respDetailsCb(data) {
        $scope.panelDetails = data.details;
        $scope.$apply();
    }

    angular.module('ovAlarmTable', [])
        .run(['IconService', function (is) {
            // we want to be able to re-use the clock glyph in our nav icon...
            is.registerIconMapping('nav_alarms', 'alarmsTopo-overlay-clock');
        }])

        .controller('OvAlarmTableCtrl',
            ['$log', '$scope', '$location', 'TableBuilderService',
                'FnService', 'WebSocketService',

            function (_$log_, _$scope_, _$location_, tbs, _fs_, _wss_) {
                var params;

                $log = _$log_;
                $scope = _$scope_;
                $loc = _$location_;

                fs = _fs_;
                wss = _wss_;

                params = $loc.search();
                if (params.hasOwnProperty('devId')) {
                    $scope.devId = params['devId'];
                }

                var handlers = {};
                $scope.panelDetails = {};

                // details response handler
                handlers[detailsResp] = respDetailsCb;
                wss.bindHandlers(handlers);

                // custom selection callback
                function selCb($event, row) {
                    $log.debug("selCb row=" + JSON.stringify(row, null, 4) +
                            ", $event=" + JSON.stringify($event, null, 4));
                    $log.debug('$scope.selId=', $scope.selId);
                    if ($scope.selId) {
                        $log.debug('send');
                        wss.sendEvent(detailsReq, {id: row.id});
                    } else {
                        $log.debug('hidePanel');
                        $scope.hidePanel();
                    }
                    $log.debug('Got a click on:', row);
                }

                // TableBuilderService creating a table for us
                tbs.buildTable({
                    scope: $scope,
                    tag: 'alarmTable',
                    selCb: selCb,
                    query: params
                });

                // cleanup
                $scope.$on('$destroy', function () {
                    wss.unbindHandlers(handlers);
                    $log.log('OvAlarmTableCtrl has been destroyed');
                });

                $log.log('OvAlarmTableCtrl has been created');
            }])

        .directive('ovAlarmTableItemDetailsPanel',
            ['PanelService', 'KeyService',

            function (ps, ks) {
                return {
                    restrict: 'E',
                    link: function (scope, element, attrs) {
                        // insert details panel with PanelService
                        // create the panel
                        var panel = ps.createPanel(pName, {
                            width: 400,
                            margin: 20,
                            hideMargin: 0
                        });
                        panel.hide();
                        scope.hidePanel = function () {
                            panel.hide();
                        };

                        function closePanel() {
                            if (panel.isVisible()) {
                                $scope.selId = null;
                                panel.hide();
                                return true;
                            }
                            return false;
                        }

                        // create key bindings to handle panel
                        ks.keyBindings({
                            esc: [closePanel, 'Close the details panel'],
                            _helpFormat: ['esc']
                        });
                        ks.gestureNotes([
                            ['click', 'Select a row to show item details']
                        ]);

                        // update the panel's contents when the data is changed
                        scope.$watch('panelDetails', function () {
                            if (!fs.isEmptyObject(scope.panelDetails)) {
                                panel.empty();
                                populatePanel(panel);
                                panel.show();
                            }
                        });

                        // cleanup on destroyed scope
                        scope.$on('$destroy', function () {
                            ks.unbindKeys();
                            ps.destroyPanel(pName);
                        });
                    }
                };
            }]);
}());
