'use strict';

var http = require('http');
// var httpProxy = require('http-proxy');
var connect = require('connect');
var serveStatic = require('serve-static');
var path = require('path');

var conf = {
  paths: {
    root: '../../../../../'
  },
  port: '8182'
}

var httpProxyInit = function (baseDir) {

  var app = connect();

  app.use(serveStatic(path.join(__dirname, baseDir)));

  var server = http.createServer(app);

  server.listen(conf.port, function(){
    console.log('Dev server is up and listening on http://localhost:', conf.port);
  });
};

httpProxyInit(conf.paths.root);