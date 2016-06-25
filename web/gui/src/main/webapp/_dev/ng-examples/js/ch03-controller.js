// Simple controller

angular.module('notesApp', [])
    .controller('ListCtrl', [function () {
        var self = this;
        self.items = [
            {id: 1, label: 'First', done: true},
            {id: 2, label: 'Second', done: false}
        ];

        self.getDoneClass = function (item) {
            return {
                finished: item.done,
                unfinished: !item.done
            };
        };
    }]);
