// js for roadm device table view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, $loc, wss, ns;

    // constants
    var detailsReq = 'roadmDeviceDetailsRequest';

    angular.module('ovRoadmDevice', [])
        .controller('OvRoadmDeviceCtrl',
        ['$log', '$scope', '$location', 'TableBuilderService', 'WebSocketService',
            'NavService',

            function (_$log_, _$scope_, _$loc_, tbs, _wss_, _ns_) {
                $log = _$log_;
                $scope = _$scope_;
                $loc = _$loc_;
                wss = _wss_;
                ns = _ns_;

                // query for if a certain device needs to be highlighted
                var params = $loc.search();
                if (params.hasOwnProperty('devId')) {
                    $scope.selId = params['devId'];
                }

                // TableBuilderService creating a table for us
                tbs.buildTable({
                    scope: $scope,
                    tag: 'roadmDevice'
                });

                $scope.nav = function (path) {
                    if ($scope.selId) {
                        ns.navTo(path, { devId: $scope.selId });
                    }
                };

                // cleanup
                $scope.$on('$destroy', function () {
                    //wss.unbindHandlers(handlers);
                    $log.log('OvRoadmDeviceCtrl has been destroyed');
                });

                $log.log('OvRoadmDeviceCtrl has been created');
            }]);
}());
