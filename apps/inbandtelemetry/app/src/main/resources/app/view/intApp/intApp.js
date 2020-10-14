(function() {
    'use strict';

    // injected refs
    let $log, $scope, $interval, $timeout, fs, wss, ks, ls;

    // constants
    let intIntentAddReq = 'intIntentAddRequest';
    let intIntentDelReq = 'intIntentDelRequest';

    let refreshInterval = 1000;

    let propOrder = ['id', 'srcAddr', 'dstAddr', 'srcPort', 'dstPort', 'insMask'];
    let friendlyProps = ['IntIntent ID', 'Src Address', 'Dst Address', 'Src Port', 'Dst Port', 'Ins Mask'];

    function checkArgAndShowMsg() {
        // Need to match at least one field
        if ($scope.ip4SrcPrefix === "" &&
            $scope.ip4DstPrefix === "" &&
            $scope.l4SrcPort === "" &&
            $scope.l4DstPort === "" ) {
            $scope.intAddMsg = "Nothing installed since there is no matching spec.";
            return false;
        }
        // IP address validation
        let ipv4Pattern = /^([0-9]{1,3}\.){3}[0-9]{1,3}(\/[0-9]{1,2})?$/;
        if ($scope.ip4SrcPrefix !== "" && !ipv4Pattern.test($scope.ip4SrcPrefix)) {
            $scope.intAddMsg = "Invalid source IP.";
            return false;
        }
        if ($scope.ip4DstPrefix !== "" && !ipv4Pattern.test($scope.ip4DstPrefix)) {
            $scope.intAddMsg = "Invalid destination IP.";
            return false;
        }
        // L4 port validation
        if ($scope.l4SrcPort !== "") {
            let l4SrcPort = parseInt($scope.l4SrcPort);
            if (isNaN(l4SrcPort)) {
                $scope.intAddMsg = "Invalid source port number.";
                return false;
            }
            if (l4SrcPort <= 0 || l4SrcPort > 65535) {
                $scope.intAddMsg = "Invalid source port number.";
                return false;
            }
            if ($scope.protocol === "") {
                $scope.intAddMsg = "protocol cannot be empty.";
                return false;
            }
        }
        if ($scope.l4DstPort !== "") {
            let l4DstPort = parseInt($scope.l4DstPort);
            if (isNaN(l4DstPort)) {
                $scope.intAddMsg = "Invalid destination port number.";
                return false;
            }
            if (l4DstPort <= 0 || l4DstPort > 65535) {
                $scope.intAddMsg = "Invalid destination port number.";
                return false;
            }
            if ($scope.protocol === "") {
                $scope.intAddMsg = "protocol cannot be empty.";
                return false;
            }
        }
        $scope.intAddMsg = "";
        return true;
    }

    function sendIntIntentString() {
        let inst = [];
        if ($scope.metaSwId) inst.push("SWITCH_ID");
        if ($scope.metaPortId) inst.push("PORT_ID");
        if ($scope.metaHopLatency) inst.push("HOP_LATENCY");
        if ($scope.metaQOccupancy) inst.push("QUEUE_OCCUPANCY");
        if ($scope.metaIngressTstamp) inst.push("INGRESS_TIMESTAMP");
        if ($scope.metaEgressTstamp) inst.push("EGRESS_TIMESTAMP");
        if ($scope.metaEgressTx) inst.push("EGRESS_TX_UTIL");

        let intentObjectNode = {
            "ip4SrcPrefix": $scope.ip4SrcPrefix,
            "ip4DstPrefix": $scope.ip4DstPrefix,
            "l4SrcPort": $scope.l4SrcPort,
            "l4DstPort": $scope.l4DstPort,
            "protocol": $scope.protocol,
            "metadata": inst,
            "telemetryMode": $scope.telemetryMode
        };
        if (checkArgAndShowMsg()) {
            wss.sendEvent(intIntentAddReq, intentObjectNode);
        }
    }

    function delIntIntent() {
        if ($scope.selId) {
            wss.sendEvent(intIntentDelReq, {
                "intentId": $scope.selId
            });
        }
    }

    function intIntentBuildTable(o) {
        let handlers = {},
            root = o.tag + 's',
            req = o.tag + 'DataRequest',
            resp = o.tag + 'DataResponse',
            onSel = fs.isF(o.selCb),
            onResp = fs.isF(o.respCb),
            idKey = o.idKey || 'id',
            oldTableData = [],
            refreshPromise;

        o.scope.tableData = [];
        o.scope.changedData = [];
        o.scope.sortParams = o.sortParams || {};
        o.scope.autoRefresh = true;
        o.scope.autoRefreshTip = 'Toggle auto refresh';

        // === websocket functions --------------------
        // response
        function respCb(data) {
            ls.stop();
            o.scope.tableData = data[root];
            o.scope.annots = data.annots;
            onResp && onResp();

            // checks if data changed for row flashing
            if (!angular.equals(o.scope.tableData, oldTableData)) {
                o.scope.changedData = [];
                // only flash the row if the data already exists
                if (oldTableData.length) {
                    angular.forEach(o.scope.tableData, function (item) {
                        if (!fs.containsObj(oldTableData, item)) {
                            o.scope.changedData.push(item);
                        }
                    });
                }
                angular.copy(o.scope.tableData, oldTableData);
            }
        }
        handlers[resp] = respCb;
        wss.bindHandlers(handlers);

        // request
        function sortCb(params) {
            let p = angular.extend({}, params, o.query);
            if (wss.isConnected()) {
                wss.sendEvent(req, p);
                ls.start();
            }
        }
        o.scope.sortCallback = sortCb;

        // === selecting a row functions ----------------
        function selCb($event, selRow) {
            let selId = selRow[idKey];
            o.scope.selId = (o.scope.selId === selId) ? null : selId;
            onSel && onSel($event, selRow);
        }
        o.scope.selectCallback = selCb;

        // === autoRefresh functions ------------------
        function fetchDataIfNotWaiting() {
            if (!ls.waiting()) {
                if (fs.debugOn('widget')) {
                    $log.debug('Refreshing ' + root + ' page');
                }
                sortCb(o.scope.sortParams);
            }
        }

        function startRefresh() {
            refreshPromise = $interval(fetchDataIfNotWaiting, refreshInterval);
        }

        function stopRefresh() {
            if (refreshPromise) {
                $interval.cancel(refreshPromise);
                refreshPromise = null;
            }
        }

        function toggleRefresh() {
            o.scope.autoRefresh = !o.scope.autoRefresh;
            o.scope.autoRefresh ? startRefresh() : stopRefresh();
        }
        o.scope.toggleRefresh = toggleRefresh;

        // === Cleanup on destroyed scope -----------------
        o.scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
            stopRefresh();
            ls.stop();
        });

        sortCb(o.scope.sortParams);
        startRefresh();
    }

    let app1 = angular.module('ovIntApp', []);
    app1.controller('OvIntAppCtrl',
        ['$log', '$scope', '$interval', '$timeout', 'TableBuilderService',
            'FnService', 'WebSocketService', 'KeyService', 'LoadingService',

            function(_$log_, _$scope_, _$interval_, _$timeout_, tbs, _fs_, _wss_, _ks_, _ls_) {
                $log = _$log_;
                $scope = _$scope_;
                $interval = _$interval_;
                $timeout = _$timeout_;
                fs = _fs_;
                wss = _wss_;
                ks = _ks_;
                ls = _ls_;

                // custom selection callback
                function selCb($event, row) {
                }
                intIntentBuildTable({
                    scope: $scope,
                    tag: 'intAppIntIntent'
                    // selCb: selCb
                });

                $scope.sendIntIntentString = sendIntIntentString;
                $scope.delIntIntent = delIntIntent;
                $scope.intAddMsg = "";
                $scope.ip4SrcPrefix = "";
                $scope.ip4DstPrefix = "";
                $scope.l4SrcPort = "";
                $scope.l4DstPort = "";
                $scope.protocol = "";
                $scope.telemetryMode = "POSTCARD";

                // get data the first time...
                // getData();

                // cleanup
                $scope.$on('$destroy', function() {
                    // wss.unbindHandlers(handlers);
                    /*ks.unbindKeys();*/
                    $log.log('OvIntAppCtrl has been destroyed');
                });

                $log.log('OvIntAppCtrl has been created');
            }
        ]);
}());
