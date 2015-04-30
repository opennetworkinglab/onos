// js for sample app view
(function () {
    'use strict';

    angular.module('ovSample', [])
        .controller('OvSampleCtrl',
        ['$log', '$scope',

            function ($log, $scope) {
                var self = this;

                self.msg = 'A message from our app...';

                $log.log('OvSampleCtrl has been created');
            }]);
}());
