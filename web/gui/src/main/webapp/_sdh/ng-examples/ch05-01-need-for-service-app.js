// ch05-01-need-for-service-app.js

angular.module('notesApp', [])
    .controller('MainCtrl', [function () {
        var self = this;
        self.tab = 'first';
        self.open = function (tab) {
            self.tab = tab;
        }
    }])
    .controller('SubCtrl', [function () {
        var self = this;
        self.list = [
            {id: 0, label: 'Item 0'},
            {id: 1, label: 'Item 1'}
        ];

        self.add = function () {
            var n = self.list.length;
            self.list.push({
                id: n,
                label: 'Item ' + n
            });
        }
    }]);

/*
 NOTE: When we use controllers, they are instances that get created and
       destroyed as we navigate across the application. Any state they
        hold is temporary at best, and cannot be communicated to other
        controllers.

        That's why we'd use "services" instead.
 */
