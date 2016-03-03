// js for driver view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, wss;

    // constants
    var detailsReq = 'driverDataRequest',
        detailsResp = 'driverDataResponse',
        // TODO: deal with details panel
        pName = 'ov-driver-matrix-item-details-panel',
        propOrder = ['id', 'label', 'code'],
        friendlyProps = ['Item ID', 'Item Label', 'Special Code'];


    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).html(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function populatePanel(panel) {
        var title = panel.append('h3'),
            tbody = panel.append('table').append('tbody');

        title.text('Item Details');

        propOrder.forEach(function (prop, i) {
            addProp(tbody, i, $scope.panelDetails[prop]);
        });

        panel.append('hr');
        panel.append('h4').text('Comments');
        panel.append('p').text($scope.panelDetails.comment);
    }

    function respDetailsCb(data) {
        //$log.debug('Matrix Data', data);
        $scope.behaviours = data.behaviours;
        $scope.drivers = data.drivers;
        $scope.matrix = data.matrix;
        $scope.$apply();
    }

    angular.module('ovDriverMatrix', [])
        .run(['IconService', function (is) {
            // Create our icon-to-glyph binding here:
            is.registerIconMapping('nav_drivers', 'cog');
        }])
        .controller('OvDriverMatrixCtrl',
        ['$log', '$scope', 'TableBuilderService',
            'FnService', 'WebSocketService',

            function (_$log_, _$scope_, tbs, _fs_, _wss_) {
                $log = _$log_;
                $scope = _$scope_;
                fs = _fs_;
                wss = _wss_;

                var handlers = {};
                $scope.behaviours = [];
                $scope.drivers = [];
                $scope.matrix = {};

                // details response handler
                handlers[detailsResp] = respDetailsCb;
                wss.bindHandlers(handlers);

                wss.sendEvent(detailsReq);

                //// custom selection callback
                //function selCb($event, row) {
                //    if ($scope.selId) {
                //        wss.sendEvent(detailsReq, { id: row.id });
                //    } else {
                //        $scope.hidePanel();
                //    }
                //    $log.debug('Got a click on:', row);
                //}

                function cellHit(d, b) {
                    var drec = $scope.matrix[d],
                        brec = drec && drec[b];
                    return !!brec;
                }

                $scope.cellMarked = cellHit;

                $scope.cellValue = function(d, b) {
                    return cellHit(d, b) ? 'x' : '';
                };

                // cleanup
                $scope.$on('$destroy', function () {
                    wss.unbindHandlers(handlers);
                    $log.log('OvDriverMatrixCtrl has been destroyed');
                });

                $log.log('OvDriverMatrixCtrl has been created');
            }])

        // TODO: implement row selection to show details panel
        .directive('ovDriverMatrixItemDetailsPanel', ['PanelService', 'KeyService',
            function (ps, ks) {
            return {
                restrict: 'E',
                link: function (scope, element, attrs) {
                    // insert details panel with PanelService
                    // create the panel
                    var panel = ps.createPanel(pName, {
                        width: 200,
                        margin: 20,
                        hideMargin: 0
                    });
                    panel.hide();
                    scope.hidePanel = function () { panel.hide(); };

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
