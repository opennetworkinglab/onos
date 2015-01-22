#!/usr/bin/env node

// === Mock Web Socket Server - for testing the topology view

var fs = require('fs'),
    readline = require('readline'),
    http = require('http'),
    WebSocketServer = require('websocket').server,
    port = 8123;

var lastcmd,        // last command executed
    lastargs,       // arguments to last command
    connection,     // ws connection
    origin,         // origin of connection
    scenario,       // test scenario name
    scdone,         // shows when scenario is over
    evno,           // next event number
    evdata;         // event data



var rl = readline.createInterface(process.stdin, process.stdout);
rl.setPrompt('ws> ');


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

wsServer.on('request', function(request) {
    console.log(); // newline after prompt
    console.log("Origin: ", request.origin);

    if (!originIsAllowed(request.origin)) {
        // Make sure we only accept requests from an allowed origin
        request.reject();
        console.log((new Date()) + ' Connection from origin ' + request.origin + ' rejected.');
        return;
    }

    origin = request.origin;
    connection = request.accept(null, origin);


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
        connection = null;
        origin = null;
    });
});


setTimeout(doCli, 10); // allow async processes to write to stdout first

function doCli() {
    rl.prompt();
    rl.on('line', function (line) {
        var words = line.trim().split(' '),
            cmd = words.shift(),
            str = words.join(' ');

        if (!cmd) {
            // repeat last command
            cmd = lastcmd;
            str = lastargs;
        }

        switch(cmd) {
            case 'c': connStatus(); break;
            case 'm': customMessage(str); break;
            case 's': setScenario(str); break;
            case 'n': nextEvent(); break;
            case 'q': quit(); break;
            case '?': showHelp(); break;
            default: console.log('Say what?!  (? for help)'); break;
        }
        lastcmd = cmd;
        lastargs = str;
        rl.prompt();

    }).on('close', function () {
        quit();
    });
}

var helptext = '\n' +
        'c        - show connection status\n' +
        'm {text} - send custom message to client\n' +
        's {id}   - set scenario\n' +
        's        - show scenario staus\n' +
        //'a        - auto-send events\n' +
        'n        - send next event\n' +
        'q        - exit the server\n' +
        '?        - display this help text\n';

function showHelp() {
    console.log(helptext);
}

function connStatus() {
    if (connection) {
        console.log('Connection from ' + origin + ' established.');
    } else {
        console.log('No connection.');
    }
}

function quit() {
    console.log('quitting...');
    process.exit(0);
}

function customMessage(m) {
    if (connection) {
        console.log('sending message: ' + m);
        connection.sendUTF(m);
    } else {
        console.warn('No current connection.');
    }
}

function showScenarioStatus() {
    var msg;
    if (!scenario) {
        console.log('No scenario selected.');
    } else {
        msg = 'Scenario: "' + scenario + '", ' +
                (scdone ? 'DONE' : 'next event: ' + evno);
        console.log(msg);
    }
}

function scenarioPath(evno) {
    var file = evno ? ('/ev_' + evno + '_onos.json') : '/scenario.json';
    return 'ev/' + scenario + file;
}

function setScenario(id) {
    if (!id) {
        return showScenarioStatus();
    }

    evdata = null;
    scenario = id;
    fs.readFile(scenarioPath(), 'utf8', function (err, data) {
        if (err) {
            console.warn('No scenario named "' + id + '"', err);
            scenario = null;
        } else {
            evdata = JSON.parse(data);
            console.log(); // get past prompt
            console.log('setting scenario to "' + id + '"');
            console.log(evdata.title);
            evdata.description.forEach(function (d) {
                console.log('  ' + d);
            });
            evno = 1;
            scdone = false;
        }
        rl.prompt();
    });
}

function nextEvent() {
    var path;

    if (!scenario) {
        console.log('No scenario selected.');
        rl.prompt();
    } else if (!connection) {
        console.warn('No current connection.');
        rl.prompt();
    } else {
        path = scenarioPath(evno);
        fs.readFile(path, 'utf8', function (err, data) {
            if (err) {
                console.log('No event #' + evno);
                scdone = true;
                console.log('Scenario DONE');
            } else {
                evdata = JSON.parse(data);
                console.log(); // get past prompt
                console.log('sending event #' + evno + ' [' + evdata.event + ']');
                connection.sendUTF(data);
                evno++;
            }
            rl.prompt();
        });
    }
}
