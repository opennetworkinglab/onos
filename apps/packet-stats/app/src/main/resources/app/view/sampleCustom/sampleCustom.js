// js for patch panel app custom view
/**
IMPORTANT CHANGES THAT MUST BE MADE BEFORE SUBMITTING

-Graph is not being updated with packet counters. The counters is only set to 0.
 This is a New bug, after individual buttons were removed.
-HTML Page is not being resized depending on the the size of the screen.
-Must Enable Scrolling
-Can Remove Redundant Code and create one json file, instead of multiple.
-Add comments and work on the code formatting.

Due to a paucity of time, I couldn't work on some of these important changes.
The first bug is very important to correct and the code should not be in the ONOS
codebase without the change.

Feel free to contact me for any issues.
**/
var strdhcpval = "h";
 (function () {
     'use strict';
     // injected refs
     var $log, $scope, wss, d3Elem;

     // constants remember to change this stuff!!s
     var arpReq = 'arpRequest',
         arpResp = 'arpResponse';
     var dhcpReq = 'dhcpRequest',
         dhcpResp = 'dhcpResponse';
     var icmpReq = 'icmpRequest',
         icmpResp = 'icmpResponse';
     var lldpReq = 'lldpRequest',
         lldpResp = 'lldpResponse';
     var vlanReq = 'vlanRequest',
         vlanResp = 'vlanResponse';
     var igmpReq = 'igmpRequest',
         igmpResp = 'igmpResponse';
     var pimReq = 'pimRequest',
         pimResp = 'pimResponse';
     var bsnReq = 'bsnRequest',
         bsnResp = 'bsnResponse';
     var unknownReq = 'unknownRequest',
         unknownResp = 'unknownResponse';
     var mplsReq = 'mplsRequest',
         mplsResp = 'mplsResponse';
     var arpval = 0;
     var dhcpval = 0;
     var icmpval = 0;
     var lldpval = 0;
     var vlanval = 0;
     var igmpval = 0;
     var pimval = 0;
     var bsnval =0;
     var unknownval =0;
     var mplsval = 0;

     var arpMsg  = "";
     var dhcpMsg = "";
     var icmpMsg = "";
     var lldpMsg = "";
     var vlanMsg = "";
     var igmpMsg = "";
     var pimMsg = "";
     var bsnMsg = "";
     var unknownMsg = "";
     var mplsMsg = "";

     function arpGetter() {
        wss.sendEvent(arpReq);
     }
     function icmpGetter() {
         wss.sendEvent(icmpReq);
     }
     function lldpGetter() {
         wss.sendEvent(lldpReq);
     }
     function dhcpGetter() {
         wss.sendEvent(dhcpReq);
     }
     function vlanGetter() {
         wss.sendEvent(vlanReq);
     }

     function igmpGetter() {
         wss.sendEvent(igmpReq);
     }
     function pimGetter() {
         wss.sendEvent(pimReq);
     }
     function bsnGetter() {
         wss.sendEvent(bsnReq);
     }
     function unknownGetter() {
         wss.sendEvent(unknownReq);
     }
     function mplsGetter() {
         wss.sendEvent(mplsReq);
     }


     function dthreestuff() {
     wss.sendEvent(dhcpReq);
     wss.sendEvent(unknownReq);
     wss.sendEvent(mplsReq);
    var data = [
        {label:"ARP", value:arpval},
        {label:"ICMP", value:icmpval},
        {label:"DHCP", value:dhcpval},
        {label:"PIM", value:pimval},
        {label:"BSN", value:bsnval},
        {label:"IGMP", value:igmpval},
        {label:"LLDP", value:lldpval},
        {label:"VLAN", value:vlanval},
        {label:"MPLS", value:mplsval},
        {label:"Unknown", value:unknownval}
    ];


    var div = d3.select("body").append("div").attr("class", "toolTip");

    var axisMargin = 20,
            margin = 40,
            valueMargin = 4,
            width = parseInt(d3.select('body').style('width'), 10),
            height = parseInt(d3.select('body').style('height'), 10),
            barHeight = (height-axisMargin-margin*2)* 0.4/data.length,
            barPadding = (height-axisMargin-margin*2)*0.6/data.length,
            data, bar, svg, scale, xAxis, labelWidth = 0;

    var max = d3.max(data, function(d) { return d.value; });

    var svg = d3.select('body')
            .append("svg")
            .attr("width", width)
            .attr("height", height);


    var bar = svg.selectAll("g")
            .data(data)
            .enter()
            .append("g");

    bar.attr("class", "bar")
            .attr("cx",0)
            .attr("transform", function(d, i) {
                return "translate(" + margin + "," + (i * (barHeight + barPadding) + barPadding) + ")";
            });

    bar.append("text")
            .attr("class", "label")
            .attr("y", barHeight / 2)
            .attr("dy", ".35em") //vertical align middle
            .text(function(d){
                return d.label;
            }).each(function() {
        labelWidth = Math.ceil(Math.max(labelWidth, this.getBBox().width));
    });

    var scale = d3.scale.linear()
            .domain([0, max])
            .range([0, width - margin*2 - labelWidth]);

    var xAxis = d3.svg.axis()
            .scale(scale)
            .tickSize(-height + 2*margin + axisMargin)
            .orient("bottom");

    bar.append("rect")
            .attr("transform", "translate("+labelWidth+", 0)")
            .attr("height", barHeight)
            .attr("width", function(d){
                return scale(d.value);
            });

    bar.append("text")
            .attr("class", "value")
            .attr("y", barHeight / 2)
            .attr("dx", -valueMargin + labelWidth) //margin right
            .attr("dy", ".35em") //vertical align middle
            .attr("text-anchor", "end")
            .text(function(d){
                return (d.value+" Packets");
            })
            .attr("x", function(d){
                var width = this.getBBox().width;
                return Math.max(width + valueMargin, scale(d.value));
            });

    bar
            .on("mousemove", function(d){
                div.style("left", d3.event.pageX+10+"px");
                div.style("top", d3.event.pageY-25+"px");
                div.style("display", "inline-block");
                div.html((d.label)+"<br>"+(d.value)+" Packets");
            });
    bar
            .on("mouseout", function(d){
                div.style("display", "none");
            });

    svg.insert("g",":first-child")
            .attr("class", "axisHorizontal")
            .attr("transform", "translate(" + (margin + labelWidth) + ","+ (height - axisMargin - margin)+")")
            .call(xAxis);

     }

     function responseArp(data) {
        arpval = data.DhcpCounter;
     }


     function responseDhcp(data) {
        dhcpval = data.DhcpCounter;
     }

     function responseIcmp(data) {
        icmpval = data.IcmpCounter;
     }
     function responseLldp(data) {
        lldpval = data.LldpCounter;
     }
     function responseVlan(data) {
        vlanval = data.VlanCounter;
     }
     function responseIgmp(data) {
        igmpval = data.IgmpCounter;
     }
     function responsePim(data) {
        pimval = data.PimCounter;
     }
     function responseBsn(data) {
        bsnval = data.BsnCounter;
     }
     function responseUnknown(data) {
        unknownval = data.UnknownCounter;
     }

      function responseMPLS(data) {
        mplsval = data.MplsCounter;
      }

     var app = angular.module('ovSampleCustom', [])
         .controller('OvSampleCustomCtrl',
         ['$log', '$scope', 'WebSocketService',

         function (_$log_, _$scope_, _wss_, ) {
             $log = _$log_;
             $scope = _$scope_;
             wss = _wss_;


             var handlers = {};
             $scope.data = {};

             // data response handler
             handlers[arpResp] = responseArp;
             handlers[dhcpResp] = responseDhcp;
             handlers[icmpResp] = responseIcmp;
             handlers[lldpResp] = responseLldp;
             handlers[vlanResp] = responseVlan;
             handlers[igmpResp] = responseIgmp;
             handlers[pimResp] = responsePim;
             handlers[bsnResp] = responseBsn;
             handlers[unknownResp] = responseUnknown;
             handlers[mplsResp] = responseMPLS;
             wss.bindHandlers(handlers);


             // custom click handler
             $scope.arpGetter = arpGetter;
             $scope.icmpGetter = icmpGetter;
             $scope.dhcpGetter = dhcpGetter;
             $scope.lldpGetter = lldpGetter;
             $scope.vlanGetter = vlanGetter;
             $scope.igmpGetter = igmpGetter;
             $scope.pimGetter = pimGetter;
             $scope.bsnGetter = bsnGetter;
             $scope.unknownGetter = unknownGetter;
             $scope.mplsGetter = mplsGetter;
             $scope.dthreestuff = dthreestuff;



             // cleanup
             $scope.$on('$destroy', function () {
                 wss.unbindHandlers(handlers);
                 $log.log('OvSampleCustomCtrl has been destroyed');
             });

             $log.log('OvSampleCustomCtrl has been created');
         }]);



 }());
