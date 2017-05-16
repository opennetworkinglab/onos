// js for roadm port table view
(function () {
    'use strict';

    var SET_TARGET_POWER_REQ = "roadmSetTargetPowerRequest";
    var SET_TARGET_POWER_RESP = "roadmSetTargetPowerResponse";
    var SHOW_ITEMS_REQ = "roadmShowPortItemsRequest";
    var SHOW_ITEMS_RESP = "roadmShowPortItemsResponse";
    var SET_OPS_MODE_REQ = "roadmSetOpsModeRequest";
    var SET_OPS_MODE_RESP = "roadmSetOpsModeResponse";

    // injected references
    var $log, $scope, $location, fs, tbs, wss, ns;

    var portCbTable = {};

    function setPortPower(port, targetVal, cb) {
        var id = port.id;
        portCbTable[id] = cb;
        wss.sendEvent(SET_TARGET_POWER_REQ,
            {
                devId: $scope.devId,
                id: port.id,
                targetPower: targetVal
            });
    }

    function portPowerCb(data) {
        portCbTable[data.id](data.valid, data.message);
    }

    function queryShowItems() {
        wss.sendEvent(SHOW_ITEMS_REQ,
            {
                devId: $scope.devId,
            });
    }

    function showItemsCb(data) {
        $scope.showTargetPower = data.showTargetPower;
        $scope.showServiceState = data.showServiceState;
        $scope.showFlowIcon = data.showFlowIcon;
        $scope.opsModeTypes = data.opsOperations;
        if ($scope.opsModeType == null) {
            $scope.opsModeType = $scope.opsModeTypes[0];
        }
        $scope.$apply();
    }

    function changeOpsMode() {
        wss.sendEvent(SET_OPS_MODE_REQ,
            {
                devId: $scope.devId,
                index: $scope.opsModeType.index,
                operation: $scope.opsModeType.operation
            });
    }

    function changeOpsModeCb(data) {
        $scope.changeModeFail = !data.valid;
        if ($scope.changeModeFail) {
            $scope.changeModeFailMsg = data.message;
        }
        $scope.$apply();
    }

    // check if value is an integer
    function isInteger(val) {
        var INTEGER_REGEXP = /^\-?\d+$/;
        if (INTEGER_REGEXP.test(val)) {
            return true;
        }
        return false;
    }

    angular.module('ovRoadmPort', [])
    .controller('OvRoadmPortCtrl',
        ['$log', '$scope', '$location',
            'FnService', 'TableBuilderService', 'WebSocketService', 'NavService',

        function (_$log_, _$scope_, _$location_, _fs_, _tbs_, _wss_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            tbs = _tbs_;
            wss = _wss_;
            ns = _ns_;

            $scope.deviceTip = 'Show device table';
            $scope.flowTip = 'Show flow view for this device';
            $scope.portTip = 'Show port view for this device';
            $scope.opsModeType = null;

            var handlers = {};
            handlers[SET_TARGET_POWER_RESP] = portPowerCb;
            handlers[SHOW_ITEMS_RESP] = showItemsCb;
            handlers[SET_OPS_MODE_RESP] = changeOpsModeCb;
            wss.bindHandlers(handlers);

            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'roadmPort',
                query: params
            });

            $scope.setPortPower = setPortPower;
            $scope.queryShowItems = queryShowItems;
            $scope.changeOpsMode = changeOpsMode;

            $scope.setTargetPower = function (port, targetVal) {
                wss.sendEvent(SET_TARGET_POWER_REQ,
                    {
                        devId: $scope.devId,
                        id: port.id,
                        targetPower: targetVal
                    });
                $log.debug('Got a click on:', port);
            }

            $scope.nav = function (path) {
                if ($scope.devId) {
                    ns.navTo(path, { devId: $scope.devId });
                }
            };

            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
            });

            $log.log('OvRoadmPortCtrl has been created');
        }])

    .directive('roadmPower', ['WebSocketService', function() {

        var retTemplate =
            '<span class="target-power" ng-show="!editMode" ng-click="enableEdit()">{{currItem.targetPower}}</span>' +
            '<form ng-show="editMode" name="form" novalidate>' +
                '<input type="number" name="formVal" ng-model="formVal">' +
                '<button type="submit" ng-click="send()">Set</button>' +
                '<button type="button" ng-click="cancel()">Cancel</button>' +
                '<span class="input-error" ng-show="showError">{{errorMessage}}</span>' +
            '</form>';

        return {
            restrict: 'A',
            scope: {
                currItem: '=roadmPower',
                roadmSetPower: '&'
            },
            template: retTemplate,
            link: function ($scope, $element) {
                $scope.editMode = false;
                $scope.showError = false;
                $scope.errorMessage = "Invalid target power";
            },
            controller: function($scope, $timeout) {
                $scope.enableEdit = function() {
                    if ($scope.currItem.hasTargetPower === "true" && $scope.editMode === false) {
                        // Ensure that the entry being edited remains the same even
                        // if the table entries are shifted around.
                        $scope.targetItem = $scope.currItem;
                        // Ensure the value seen in the field remains the same
                        $scope.formVal = parseInt($scope.currItem.targetPower);
                        $scope.editMode = true;
                        $timeout(function () {
                            $scope.$apply()
                        });
                    }
                };
                // Callback for server-side validation. Displays the error message
                // if the input is invalid.
                $scope.sendCb = function(valid, message) {
                    if (valid) {
                        // check if it's still pointing to the same item
                        // reordering the entries may change the binding
                        if ($scope.currItem.id === $scope.targetItem.id) {
                            // update the ui to display the new attenuation value
                            $scope.currItem.targetPower = $scope.formVal;
                        }
                        $scope.cancel();
                    } else {
                        $scope.errorMessage = message;
                        $scope.showError = true;
                    }
                    $timeout(function () {
                        $scope.$apply()
                    });
                }
                $scope.send = function() {
                    // check input is an integer
                    if (!isInteger($scope.formVal)) {
                        $scope.sendCb(false, "Target power must be an integer");
                        return;
                    }
                    $scope.roadmSetPower({port: $scope.targetItem, targetVal: $scope.formVal, cb: $scope.sendCb});
                };
                $scope.cancel = function() {
                    $scope.editMode = false;
                    $scope.showError = false;
                }
            }
        };
    }]);
}());
