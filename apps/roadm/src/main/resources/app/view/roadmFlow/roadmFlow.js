// js for roadm flow table view
(function () {
    'use strict';

    var SET_ATT_REQ = "roadmSetAttenuationRequest";
    var SET_ATT_RESP = "roadmSetAttenuationResponse";
    var DELETE_FLOW_REQ = "roadmDeleteFlowRequest";
    var CREATE_FLOW_REQ = "roadmCreateFlowRequest";
    var CREATE_FLOW_RESP = "roadmCreateFlowResponse";
    var SHOW_ITEMS_REQ = "roadmShowFlowItemsRequest";
    var SHOW_ITEMS_RESP = "roadmShowFlowItemsResponse";

    // injected references
    var $log, $scope, $location, fs, tbs, wss, ns;

    // used to map id to a request call function
    var flowCbTable = {};

    function setAttenuation(flow, targetVal, cb) {
        flowCbTable[flow.id] = cb;
        wss.sendEvent(SET_ATT_REQ,
            {
                devId: $scope.devId,
                flowId: flow.id,
                attenuation: targetVal
            });
    }

    function queryShowItems() {
        wss.sendEvent(SHOW_ITEMS_REQ,
            {
                devId: $scope.devId,
            });
    }
    
    function showItemsCb(data) {
        $scope.showChannel = data.showChannel;
        $scope.showAttenuation = data.showAttenuation;
        $scope.$apply();
    }

    function attenuationCb(data) {
        flowCbTable[data.flowId](data.valid, data.message);
    }

    // check if value is an integer
    function isInteger(val) {
        var INTEGER_REGEXP = /^\-?\d+$/;
        if (INTEGER_REGEXP.test(val)) {
            return true;
        }
        return false;
    }

    angular.module('ovRoadmFlow', [])
    .controller('OvRoadmFlowCtrl',
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

            $scope.addFlowTip = 'Create a flow';
            $scope.deviceTip = 'Show device table';
            $scope.flowTip = 'Show flow view for this device';
            $scope.portTip = 'Show port view for this device';

            $scope.showFlowForm = false;

            var handlers = {};
            handlers[SET_ATT_RESP] = attenuationCb;
            handlers[SHOW_ITEMS_RESP] = showItemsCb;
            wss.bindHandlers(handlers);

            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'roadmFlow',
                query: params
            });

            $scope.displayFlowForm = function () {
                $scope.showFlowForm = true;
            }

            $scope.hideFlowForm = function () {
                $scope.showFlowForm = false;
            }

            $scope.queryShowItems = queryShowItems;

            $scope.setAttenuation = setAttenuation;

            $scope.deleteFlow = function ($event, row) {
                wss.sendEvent(DELETE_FLOW_REQ,
                    {
                        devId: $scope.devId,
                        id: row.id
                    });
            }

            $scope.createFlow = function(flow) {
                wss.sendEvent(CREATE_FLOW_REQ,
                    {
                        devId: $scope.devId,
                        flow: flow
                    });
            }

            $scope.fakeCurrentPower = function(flow) {
                if (!isNaN(flow.currentPower)) {
                    var val = parseInt(flow.attenuation);
                    return val + (val % 5 - 2);
                } else {
                    return flow.currentPower;
                }
            }

            $scope.nav = function (path) {
                if ($scope.devId) {
                    ns.navTo(path, { devId: $scope.devId });
                }
            };

            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
            });

            $log.log('OvRoadmFlowCtrl has been created');
        }])

    .directive('roadmAtt', ['WebSocketService', function() {

        var retTemplate =
            '<span class="attenuation" ng-show="!editMode" ng-click="enableEdit()">{{currItem.attenuation}}</span>' +
            '<form ng-show="editMode" name="form" novalidate>' +
                '<input type="number" name="formVal" ng-model="formVal">' +
                '<button type="submit" class="submit" ng-click="send()">Set</button>' +
                '<button type="button" class="cancel" ng-click="cancel()">Cancel</button>' +
                '<span class="input-error" ng-show="showError">{{errorMessage}}</span>' +
            '</form>';

        return {
            restrict: 'A',
            scope: {
                currItem: '=roadmAtt',
                roadmSetAtt: '&'
            },
            template: retTemplate,
            link: function ($scope, $element) {
                $scope.editMode = false;
                $scope.showError = false;
                $scope.errorMessage = "Invalid attenuation"
            },
            controller: function($scope, $timeout) {
                $scope.enableEdit = function() {
                    // connection must support attenuation to be editable
                    if ($scope.currItem.hasAttenuation === 'true' && $scope.editMode === false) {
                        // Ensure that the entry being edited remains the same even
                        // if the table entries are shifted around.
                        $scope.targetItem = $scope.currItem;
                        // Ensure the value seen in the field remains the same
                        $scope.formVal = parseInt($scope.currItem.attenuation);
                        $scope.editMode = true;
                        $timeout(function () {
                            $scope.$apply()
                        });
                    }
                };
                $scope.send = function() {
                    // check input is an integer
                    if (!isInteger($scope.formVal)) {
                        $scope.sendCb(false, "Attenuation must be an integer");
                        return;
                    }
                    $scope.roadmSetAtt({flow: $scope.targetItem, targetVal: $scope.formVal, cb: $scope.sendCb});
                };
                // Callback for server-side validation. Displays the error message
                // if the input is invalid.
                $scope.sendCb = function(valid, message) {
                    if (valid) {
                        // check if it's still pointing to the same item
                        // reordering the entries may change the binding
                        if ($scope.currItem.id === $scope.targetItem.id) {
                            // update the ui to display the new attenuation value
                            $scope.currItem.attenuation = $scope.formVal;
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
                $scope.cancel = function() {
                    $scope.editMode = false;
                    $scope.showError = false;
                }
            }
        };
    }])

    .controller('FlowFormController', function($timeout) {
        var notIntegerError = "Must be an integer.";

        this.clearErrors = function() {
            this.priorityError = false;
            this.timeoutError = false;
            this.isPermanentError = false;
            this.inPortError = false;
            this.outPortError = false;
            this.spacingError = false;
            this.multiplierError = false;
            this.attenuationError = false;
            this.connectionError = false;
            this.channelError = false;
        }
        this.clearErrors();

        this.spacings = [
            {index: 0, freq: "100 GHz"},
            {index: 1, freq: "50 GHz"},
            {index: 2, freq: "25 GHz"},
            {index: 3, freq: "12.5 GHz"}
        ];

        this.flow = {};
        //this.flow.priority = 88;
        this.flow.permanent = true;
        this.flow.timeout = 0;
        //this.flow.inPort = 2;
        //this.flow.outPort = 2;
        this.flow.spacing = this.spacings[1];
        //this.flow.multiplier = 0;
        this.flow.channelFrequency = "";
        this.flow.attenuation = 0;

        var parent = this;

        function createFlowCb(data) {
            if (!data.inPort.valid) {
                parent.inPortMessage = data.inPort.message;
                parent.inPortError = true;
            }
            if (!data.outPort.valid) {
                parent.outPortMessage = data.outPort.message;
                parent.outPortError = true;
            }
            if (!data.connection.valid) {
                parent.connectionMessage = data.connection.message;
                parent.connectionError = true;
            }
            if (!data.spacing.valid) {
                parent.spacingMessage = data.spacing.message;
                parent.spacingError = true;
            }
            if ($scope.includeChannel)
            {
                if (!data.multiplier.valid) {
                    parent.multiplierMessage = data.multiplier.message;
                    parent.multiplierError = true;
                }
                if (!data.channelAvailable.valid) {
                    parent.channelMessage = data.channelAvailable.message;
                    parent.channelError = true;
                }
            }
            if ($scope.includeAttenuation && !data.attenuation.valid) {
                parent.attenuationMessage = data.attenuation.message;
                parent.attenuationError = true;
            }
            $timeout(function () {
                $scope.$apply()
            });
        }

        var handlers = {}
        handlers[CREATE_FLOW_RESP] = createFlowCb;
        wss.bindHandlers(handlers);
        this.createFlow = function(connection) {
            this.clearErrors();

            var error = false;
            if (!isInteger(connection.priority)) {
                this.priorityMessage = notIntegerError;
                this.priorityError = true;
                error = true;
            }
            if (!connection.permanent && !isInteger(connection.timeout)) {
                this.timeoutMessage = notIntegerError;
                this.timeoutError = true;
                error = true;
            }
            if (!isInteger(connection.inPort)) {
                this.inPortMessage = notIntegerError;
                this.inPortError = true;
                error = true;
            }
            if (!isInteger(connection.outPort)) {
                this.outPortMessage = notIntegerError;
                this.outPortError = true;
                error = true;
            }
            if ($scope.includeChannel && !isInteger(connection.multiplier)) {
                this.multiplierMessage = notIntegerError;
                this.multiplierError = true;
                error = true;
            }
            if ($scope.includeAttenuation && !isInteger(connection.attenuation)) {
                this.attenuationMessage = notIntegerError;
                this.attenuationError = true;
                error = true;
            }

            if (!error) {
                wss.sendEvent(CREATE_FLOW_REQ,
                    {
                        devId: $scope.devId,
                        formData: connection
                    });
                $log.log('Request to create connection has been sent');
            }
        }

        $scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
        });
    });

}());
