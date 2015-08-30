#!/usr/bin/env node

// === Mock Web Socket Server - for testing the topology view

var fs = require('fs'),
    readline = require('readline'),
    http = require('http'),
    WebSocketServer = require('websocket').server,
    port = 8123,
    scenarioRoot = 'ev/',
    verbose = false,         // show received messages from client
    extraVerbose = false;    // show ALL received messages from client

var lastcmd,        // last command executed
    lastargs,       // arguments to last command
    connection,     // ws connection
    origin,         // origin of connection
    scid,           // scenario ID
    scdata,         // scenario data
    scdone,         // shows when scenario is over
    eventsById,     // map of event file names
    maxEvno,        // highest loaded event number
    autoLast,       // last event number for auto-advance
    evno,           // next event number
    evdata;         // event data


process.argv.forEach(function (val) {
    switch (val) {
        case '-v': verbose = true; break;
        case '-v!': extraVerbose = true; break;
    }
});

var scFiles = fs.readdirSync(scenarioRoot);
console.log();
console.log('Mock Server v1.0');
if (verbose || extraVerbose) {
    console.log('Verbose=' + verbose, 'ExtraVerbose=' + extraVerbose);
}
console.log('================');
listScenarios();

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
    console.log('OK, server is running');
    console.log('(? for help)');
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

// displays the message if our arguments say we should
function displayMsg(msg) {
    var ev = JSON.parse(msg);
    switch (ev.event) {
        case 'topoHeartbeat': return extraVerbose;
        default: return true;
    }
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
        if (verbose || extraVerbose) {
            if (message.type === 'utf8') {
                if (displayMsg(message.utf8Data)) {
                    console.log(); // newline after prompt
                    console.log('Received Message: ' + message.utf8Data);
                }
                //connection.sendUTF(message.utf8Data);
                rl.prompt();
            }
            else if (message.type === 'binary') {
                console.log('Received Binary Message of ' + message.binaryData.length + ' bytes');
                //connection.sendBytes(message.binaryData);
            }
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
            case 'l': listScenarios(); break;
            case 'c': connStatus(); break;
            case 'm': customMessage(str); break;
            case 's': setScenario(str); break;
            case 'a': autoAdvance(); break;
            case 'n': nextEvent(); break;
            case 'r': restartScenario(); break;
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
        'l        - list scenarios\n' +
        'c        - show connection status\n' +
        'm {text} - send custom message to client\n' +
        's {id}   - load scenario {id}\n' +
        's        - show scenario status\n' +
        'a        - auto-send events\n' +
        'n        - send next event\n' +
        'r        - restart the scenario\n' +
        'q        - exit the server\n' +
        '?        - display this help text\n';

function showHelp() {
    console.log(helptext);
}

function listScenarios() {
    console.log('Scenarios ...');
    console.log(scFiles.join(', '));
    console.log();
}

function connStatus() {
    if (connection) {
        console.log('Connection from ' + origin + ' established.');
    } else {
        console.log('No connection.');
    }
}

function quit() {
    console.log('Quitting...');
    process.exit(0);
}

function customMessage(m) {
    if (connection) {
        console.log('Sending message: ' + m);
        connection.sendUTF(m);
    } else {
        console.warn('No current connection.');
    }
}

function showScenarioStatus() {
    var msg;
    if (!scid) {
        console.log('No scenario loaded.');
    } else {
        msg = 'Scenario: "' + scid + '", ' +
                (scdone ? 'DONE' : 'next event: ' + evno);
        console.log(msg);
    }
}

function scenarioPath(evno) {
    var file = evno ? ('/' + eventsById[evno].fname) : '/scenario.json';
    return scenarioRoot + scid + file;
}


function initScenario(verb) {
    console.log(); // get past prompt
    console.log(verb + ' scenario "' + scid + '"');
    console.log(scdata.title);
    scdata.description.forEach(function (d) {
        console.log('  ' + d);
    });
    autoLast = (scdata.params && scdata.params.lastAuto) || 0;
    if (autoLast) {
        console.log('[auto-advance: ' + autoLast + ']');
    }
    evno = 1;
    scdone = false;
    readEventFilenames();
}

function readEventFilenames() {
    var files = fs.readdirSync(scenarioRoot + scid),
        eventCount = 0,
        match, id, tag;

    maxEvno = 0;

    eventsById = {};
    files.forEach(function (f) {
        match = /^ev_(\d+)_(.*)\.json$/.exec(f);
        if (match) {
            eventCount++;
            id = match[1];
            tag = match[2];
            eventsById[id] = {
                fname: f,
                num: id,
                tag: tag
            };
            if (Number(id) > Number(maxEvno)) {
                maxEvno = id;
            }
        }

    });
    console.log('[' + eventCount + ' events loaded, (max=' + maxEvno + ')]');
}

function setScenario(id) {
    if (!id) {
        return showScenarioStatus();
    }

    evdata = null;
    scid = id;
    fs.readFile(scenarioPath(), 'utf8', function (err, data) {
        if (err) {
            console.warn('No scenario named "' + id + '"', err);
            scid = null;
        } else {
            scdata = JSON.parse(data);
            initScenario('Loading');
        }
        rl.prompt();
    });
}

function restartScenario() {
    if (!scid) {
        console.log('No scenario loaded.');
    } else {
        initScenario('Restarting');
    }
    rl.prompt();
}

function eventAvailable() {
    if (!scid) {
        console.log('No scenario loaded.');
        rl.prompt();
        return false;
    }

    if (!connection) {
        console.log('No current connection.');
        rl.prompt();
        return false;
    }

    if (Number(evno) > Number(maxEvno)) {
        scdone = true;
        console.log('Scenario DONE.');
        return false;
    }
    return true;
}

function autoAdvance() {
    if (evno > autoLast) {
        console.log('[auto done]');
        return;
    }

    // need to recurse with a callback, since each event send relies
    // on an async load of event data...
    function callback() {
        if (eventAvailable() && evno <= autoLast) {
            _nextEvent(callback);
        }
    }

    callback();
}

function nextEvent() {
    if (eventAvailable()) {
        _nextEvent();
    }
}

function _nextEvent(callback) {
    var path = scenarioPath(evno);

    fs.readFile(path, 'utf8', function (err, data) {
        if (err) {
            console.error('Oops error: ' + err);
        } else {
            evdata = JSON.parse(data);
            console.log(); // get past prompt
            console.log('Sending event #' + evno + ' [' + evdata.event +
                    '] from ' + eventsById[evno].fname);
            connection.sendUTF(data);
            evno++;
            if (callback) {
                callback();
            }
        }
        rl.prompt();
    });
}
