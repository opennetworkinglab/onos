Run:

* curl --user onos:rocks -d @CreateMdDomainA.json http://localhost:8181/onos/cfm/md --header "Content-Type:application/json"
* curl --user onos:rocks -d @CreateMa1InDomainA.json http://localhost:8181/onos/cfm/md/DomainA/ma --header "Content-Type:application/json"
* curl --user onos:rocks -d @CreateMa2InDomainA.json http://localhost:8181/onos/cfm/md/DomainA/ma --header "Content-Type:application/json"
* curl --user onos:rocks http://localhost:8181/onos/cfm/md 

Create a Device that supports CFM Programmable, say at netconf:192.168.56.10:830
* curl --user onos:rocks -d @CreateMep10.json http://localhost:8181/onos/cfm/md/DomainA/ma/ma-vlan-1/mep --header "Content-Type:application/json"

Now call the RPC to create a Delay Measurement on that device
* curl --user onos:rocks -X PUT -d @CreateDM_Mep10.json http://localhost:8181/onos/cfm/md/DomainA/ma/ma-vlan-1/mep/10/dm --header "Content-Type:application/json"
