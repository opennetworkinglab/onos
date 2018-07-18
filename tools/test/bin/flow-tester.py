import concurrent.futures
import requests, json
from optparse import OptionParser

def run(url, request):
    data = json.dumps(request)
    r = requests.post(url, data, auth=("onos", "rocks"))
    return r

def runTasks(flowPerDevice, neighbours, url, servers, doJson, remove):
    # We can use a with statement to ensure threads are cleaned up promptly
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        # Start the load operations and mark each future with its URL
        request = { "flowsPerDevice" : flowPerDevice, "neighbours" : neighbours, "remove" : remove }
        future_to_url = {executor.submit(run, url % (server), request) for server in servers}
        for f in concurrent.futures.as_completed(future_to_url):
            try:
                response = f.result()
                server = response.url.split('//')[1].split(':')[0]
                if (doJson):
                    print (json.dumps({ "server" : server, "elapsed" : response.json()['elapsed'] }))
                else:
                    print ("%s -> %sms" % (server, response.json()['elapsed']))
            except Exception as exc:
                print("Execution failed -> %s" % exc)

if __name__ == "__main__":
    parser = OptionParser()
    parser.add_option("-u", "--url", dest="url", help="set the url for the request",
                            default="http://%s:8181/onos/demo/intents/flowTest")
    parser.add_option("-f", "--flows", dest="flows", help="Number of flows to install per device",
                            default=100, type="int")
    parser.add_option("-n", "--neighbours", dest="neighs", help="Number of neighbours to communicate to",
                            default=0, type="int")
    parser.add_option("-s", "--servers", dest="servers", help="List of servers to hit",
                            default=[], action="append")
    parser.add_option("-r", "--remove", dest="remove", help="Do not remove flows after installation",
                            default=True, action="store_false")
    parser.add_option("-j", "--json", dest="doJson", help="Print results in json",
                            default=False, action="store_true")

    (options, args) = parser.parse_args()
    if (len(options.servers) == 0):
        options.servers.append("localhost")
    runTasks(options.flows, options.neighs, options.url, options.servers, options.doJson, options.remove)
