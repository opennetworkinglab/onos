// Karma configuration

module.exports = function(config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    // the path is relative to this (karma.conf.js) file
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],


    // list of files / patterns to load in the browser
    files: [
        // library code...
        '../tp/angular.js',
        '../tp/angular-mocks.js',
        '../tp/angular-route.js',
        '../tp/angular-cookies.js',
        '../tp/d3.js',
        '../tp/topojson.v1.min.js',

        // production code...
        // make sure modules are defined first...
        '../onos.js',

        '../app/fw/util/util.js',
        '../app/fw/svg/svg.js',
        '../app/fw/remote/remote.js',
        '../app/fw/widget/widget.js',
        '../app/fw/layer/layer.js',

        '../app/view/topo/topo.js',

        // now load services etc. that augment the modules
        '../app/**/*.js',

        // unit test code...
        'app/*-spec.js',
        'app/**/*-spec.js',

        // server mock
        './server.mock.js'
    ],


    // list of files to exclude
    exclude: [
    ],


    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
    },


    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['mocha'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_DISABLE,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['PhantomJS'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true
  });
};
