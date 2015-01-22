#!/usr/bin/env node
// === Mock Web Socket Server - for testing the topology view
//

var readline = require('readline');
var WebSocketServer = require('websocket').server;
var http = require('http');
var port = 8123;


var server = http.createServer(function(request, response) {
    console.log((new Date()) + ' Received request for ' + request.url);
    response.writeHead(404);
    response.end();
});

server.listen(port, function() {
    console.log((new Date()) + ' Server is listening on port ' + port);
});

server.on('listening', function () {
    console.log('ok, server is running');
});

var wsServer = new WebSocketServer({
    httpServer: server,
    // You should not use autoAcceptConnections for production
    // applications, as it defeats all standard cross-origin protection
    // facilities built into the protocol and the browser.  You should
    // *always* verify the connection's origin and decide whether or not
    // to accept it.
    autoAcceptConnections: false
});

function originIsAllowed(origin) {
    // put logic here to detect whether the specified origin is allowed.
    return true;
}

var connection;

wsServer.on('request', function(request) {
    console.log(); // newline after prompt
    console.log("Origin: ", request.origin);

    if (!originIsAllowed(request.origin)) {
        // Make sure we only accept requests from an allowed origin
        request.reject();
        console.log((new Date()) + ' Connection from origin ' + request.origin + ' rejected.');
        return;
    }

    connection = request.accept(null, request.origin);


    console.log((new Date()) + ' Connection accepted.');
    rl.prompt();


    connection.on('message', function(message) {
        if (message.type === 'utf8') {
            console.log(); // newline after prompt
            console.log('Received Message: ' + message.utf8Data);
            //connection.sendUTF(message.utf8Data);
            rl.prompt();
        }
        else if (message.type === 'binary') {
            console.log('Received Binary Message of ' + message.binaryData.length + ' bytes');
            //connection.sendBytes(message.binaryData);
        }
    });
    connection.on('close', function(reasonCode, description) {
        console.log((new Date()) + ' Peer ' + connection.remoteAddress + ' disconnected.');
    });
});


var rl = readline.createInterface(process.stdin, process.stdout);
rl.setPrompt('ws> ');

setTimeout(doCli, 10);

function doCli() {
    rl.prompt();
    rl.on('line', function (line) {
        var words = line.trim().split(' '),
            cmd = words.shift(),
            str = words.join(' ');

        switch(cmd) {
            case 'hello':
                console.log('hello back: ' + str);
                break;

            case 'quit':
                process.exit(0);
                break;

            case 'm':
                console.log('sending message: ' + str);
                connection.sendUTF(str);
                break;

            default:
                console.log('Say what?! [' + line.trim() + ']');
                break;
        }
        rl.prompt();
    }).on('close', function () {
        console.log('quitting...');
        process.exit(0);
    });

}
