// js for patch panel app custom view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, wss, ks;

    // constants
    var dataReq = 'sampleCustomDataRequest',
        dataResp = 'sampleCustomDataResponse';
    var dataReq2 = 'sampleCustomDataRequest2',
        dataResp2 = 'sampleCustomDataResponse2';
    var dataReq3 = 'sampleCustomDataRequest3',
        dataResp3 = 'sampleCustomDataResponse3';


    function addKeyBindings() {
        var map = {space: [getData, 'Fetch data from server'], _helpFormat: [['space']]};
        ks.keyBindings(map);

    }

    function getData() {
       wss.sendEvent(dataReq);
    }
    function used() {
        wss.sendEvent(dataReq3);
    }
    function loadPorts(){
        $scope.ports = [];
        var i;
        var index;
        for(i = 0; i < $scope.cps.length ; i++){
            if($scope.cps[i] == $scope.myDev){
                index = i;
            }
        }
        var j = index+1;
        while( $scope.data.cps[j].indexOf("o") != 0){
            var tempi = {name : $scope.data.cps[j]};
            $scope.ports.push(tempi);
            j++;
        }
    }
    function done(){
        var temp = [$scope.myDev.name, $scope.myPort1.name, $scope.myPort2.name];
        var temp1 = {result : temp};
        wss.sendEvent(dataReq2, temp1);

    }
    function respDataCb(data) {
        $scope.data = data;
        $scope.cps = [];
        $scope.devices = [];
        var i;
        for(i = 0; i < $scope.data.cps.length; i++){
            $scope.cps.push(temp);
            if($scope.data.cps[i].indexOf("o") == 0){
                var temp = {name : $scope.data.cps[i]};
                $scope.devices.push(temp);
            }
        }
        $scope.$apply();
    }
    function respDataCb2(data) {
        $scope.data = data;
        $scope.$apply();
    }
    function respDataCb3(data) {
        $scope.data = data;
        $scope.$apply();
    }

    var app = angular.module('ovSampleCustom', [])
        .controller('OvSampleCustomCtrl',
        ['$log', '$scope', 'WebSocketService', 'KeyService',

        function (_$log_, _$scope_, _wss_, _ks_) {
            $log = _$log_;
            $scope = _$scope_;
            wss = _wss_;
            ks = _ks_;

            $scope.cps = [];
            $scope.devices = [];
            $scope.ports = [];
            $scope.myDev = $scope.devices[0];
            $scope.myPort1 = $scope.ports[0];
            $scope.myPort2 = $scope.ports[0];


            var handlers = {};
            $scope.data = {};
            
            // data response handler
            handlers[dataResp] = respDataCb;
            handlers[dataResp2] = respDataCb2;
            handlers[dataResp3] = respDataCb3;
            wss.bindHandlers(handlers);

            addKeyBindings();

            // custom click handler
            $scope.getData = getData;
            $scope.loadPorts = loadPorts;
            $scope.used = used;
            $scope.done = done;

            // cleanup
            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
                ks.unbindKeys();
                $log.log('OvSampleCustomCtrl has been destroyed');
            });

            $log.log('OvSampleCustomCtrl has been created');
        }]);



}());

