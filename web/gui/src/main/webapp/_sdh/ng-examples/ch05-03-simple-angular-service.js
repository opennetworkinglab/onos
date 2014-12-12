// ch05-03-simple-angular-service.js

// this example shows three different ways of defining our own "service"...

// use 'factory()' for functions/plain objects API
// use 'service()' for JS class object API
// use 'provider()' for configurable service API


// this is a service definition
function ItemServiceTwo() {
    var items = [
        {id: 0, label: 'Item 0'},
        {id: 1, label: 'Item 1'}
    ];
    this.list = function () {
        return items;
    };
    this.add = function (item) {
        items.push(item);
    };
}

// this is a provider definition
function ItemServiceThree(optItems) {
    var items = optItems || [];

    this.list = function () {
        return items;
    };
    this.add = function (item) {
        items.push(item);
    }
}

angular.module('notesApp', [])

    // [provider] define item service as configurable provider
    .provider('ItemServiceThree', function () {
        var haveDefaultItems = true;

        this.disableDefaultItems = function () {
            haveDefaultItems = false;
        };

        // this function gets our dependencies..
        this.$get = [function () {
            var optItems = [];
            if (haveDefaultItems) {
                optItems = [
                    {id: 0, label: 'Item 0'},
                    {id: 1, label: 'Item 1'}
                ];
            }
            return new ItemServiceThree(optItems);
        }];
    })

    // [provider] define configuration for provider
    .config(['ItemServiceThreeProvider', function (ItemServiceThreeProvider) {
        // to see how the provider can change configuration
        // change the value of shouldHaveDefaults to true and
        // try running the example
        var shouldHaveDefaults = false;

        // get configuration from server.
        // set shouldHaveDefaults somehow
        // assume it magically changes for now
        if (!shouldHaveDefaults) {
            ItemServiceThreeProvider.disableDefaultItems();
        }
    }])

    // [service] define item service as a JS class
    .service('ItemServiceTwo', [ItemServiceTwo])

    // [factory] define item service factory
    .factory('ItemService', [function () {
        var items = [
            {id: 0, label: 'Item 0'},
            {id: 1, label: 'Item 1'}
        ];
        return {
            list: function () {
                return items;
            },
            add: function (item) {
                items.push(item);
            }
        };
    }])

    // ======================================================================
    // define controllers...
    .controller('MainCtrl', [function () {
        var self = this;
        self.tab = 'first';
        self.open = function (tab) {
            self.tab = tab;
        };
    }])

    .controller('SubCtrl', ['ItemService', function (ItemService) {
        var self = this;
        self.list = function () {
            return ItemService.list();
        };
        self.add = function () {
            var n = self.list().length;
            ItemService.add({
                id: n,
                label: 'Item ' + n
            });
        };
    }]);
