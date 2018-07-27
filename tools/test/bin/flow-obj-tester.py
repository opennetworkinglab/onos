import concurrent.futures
import requests, json
from optparse import OptionParser

def run(url, request):
    data = json.dumps(request)
    r = requests.post(url, data, auth=("onos", "rocks"))
    return r

def runTasks(flowObjPerDevice, typeObj, neighbours, url, servers, doJson, remove):
    # We can use a with statement to ensure threads are cleaned up promptly
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        # Start the load operations and mark each future with its URL
        request = { "flowObjPerDevice" : flowObjPerDevice, "typeObj" : typeObj, "neighbours" : neighbours, "remove" : remove }
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
                            default="http://%s:8181/onos/demo/intents/flowObjTest")
    parser.add_option("-f", "--flowObj", dest="flowObj", help="Number of flow objectives to install per device",
                            default=100, type="int")
    parser.add_option("-n", "--neighbours", dest="neighs", help="Number of neighbours to communicate to",
                            default=0, type="int")
    parser.add_option("-s", "--servers", dest="servers", help="List of servers to hit",
                            default=[], action="append")
    parser.add_option("-r", "--remove", dest="remove", help="Whether to remove flow objectives after installation",
                            default=True, action="store_false")
    parser.add_option("-j", "--json", dest="doJson", help="Print results in json",
                            default=False, action="store_true")
    parser.add_option("-t", "--typeObj", dest="typeObj", help="Type of Objective to install",
                            default="forward", type="string")

    (options, args) = parser.parse_args()
    if (len(options.servers) == 0):
        options.servers.append("localhost")
    runTasks(options.flowObj, options.typeObj, options.neighs, options.url, options.servers, options.doJson, options.remove)

