package org.ctpd.closfwd;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;



import org.ctpd.closfwd.OltEndpoint;
import org.ctpd.closfwd.OltControlEndpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ServiceEndpoint;
import org.ctpd.closfwd.StorageEndpoint;
import org.ctpd.closfwd.VpdcEndpoint;
import org.ctpd.closfwd.VpdcHostEndpoint;
import org.ctpd.closfwd.VoltEndpoint;
import org.ctpd.closfwd.ExternalServiceEndpoint;
import org.ctpd.closfwd.ClosFwdWebResource;
import org.ctpd.closfwd.CustomiceException;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.rest.AbstractWebResource;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class  ControllerEndpoint{

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

    public static void EntryArgument(List<Endpoint> endpoint) {
        String type;
        for(Endpoint i : endpoint){
            
            if( i instanceof OltControlEndpoint){
                OltControlEndpoint oltControl = (OltControlEndpoint) i;
                type="OltCOntrolEndpoint";
                if(i.getNode().toString()==""){
                    throw new CustomiceException("Attribute device can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                    "vlan:"+oltControl.getVlan().toString()+"\\"+
                                                   "volt_id:"+oltControl.getVoltUUID().toString()+"\\"+
                                                   "explicit_vlan:"+oltControl.getExplicitVlan().toString());
                }
                if(i.getPort().toString()=="0"){
                    throw new CustomiceException("Attribute port can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                    "vlan:"+oltControl.getVlan().toString()+"\\"+
                                                   "volt_id:"+oltControl.getVoltUUID().toString()+"\\"+
                                                   "explicit_vlan:"+oltControl.getExplicitVlan().toString());
                }
                if(oltControl.getVlan().toString()=="None"){
                    throw new CustomiceException("Attribute vlan can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "volt_id:"+oltControl.getVoltUUID().toString()+"\\"+
                                                   "explicit_vlan:"+oltControl.getExplicitVlan().toString());
                }
                if(oltControl.getVoltUUID()==null){
                     throw new CustomiceException("Attribute volt_id can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "vlan:"+oltControl.getVlan().toString()+"\\"+
                                                   "explicit_vlan:"+oltControl.getExplicitVlan().toString());
                }
                
                if(oltControl.getExplicitVlan()==null){
                     throw new CustomiceException("Attribute explicit_vlan can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "volt_id:"+oltControl.getVoltUUID().toString()+"\\"+
                                                   "vlan:"+oltControl.getVlan().toString());
                }            
            }
           
             if (i instanceof ServiceEndpoint){
                 ServiceEndpoint service=(ServiceEndpoint) i;
                 type="ServiceEndpoint";
                 if(i.getNode().toString()==""){
                    throw new CustomiceException("Attribute device can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+service.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                   "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                   "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                    "vlan:"+service.getVlan().toString()+"\\"+
                                                   "src_mac:"+service.getSrcMacSer());
                }
                if(i.getPort().toString()=="0"){
                    throw new CustomiceException("Attribute port can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "ip:"+service.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                   "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                   "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                    "vlan:"+service.getVlan().toString()+"\\"+
                                                   "src_mac:"+service.getSrcMacSer());
                }
                 if(service.getVlan().toString()=="None"){
                     throw new CustomiceException("Attribute vlan can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+service.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                   "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                   "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                   "src_mac:"+service.getSrcMacSer());
                }
                
                 if(service.getIpPrefix()==null){
                      throw new CustomiceException("Attribute ip can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "vlan:"+service.getVlan().toString()+"\\"+
                                                   "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                   "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                   "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                   "src_mac:"+service.getSrcMacSer());
                }
                 if(service.getSrcMacMask().toString()=="00:00:00:00:00:00"){
                      throw new CustomiceException("Attribute src_mac_mask can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+service.getIpPrefix().toString()+"\\"+
                                                   "vlan:"+service.getVlan().toString()+"\\"+
                                                   "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                   "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                   "src_mac:"+service.getSrcMacSer());
                }
                 if(service.getExternalAccessFlagSer()==null){
                      throw new CustomiceException("Attribute external_access can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+service.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                   "vlan:"+service.getVlan().toString()+"\\"+
                                                   "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                   "src_mac:"+service.getSrcMacSer());
                }
                 if(service.getBypassEnabledSer()==""|| service.getBypassEnabledSer()==null){
                      throw new CustomiceException("Attribute bypass_enabled can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+service.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                   "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                   "vlan:"+service.getVlan().toString()+"\\"+
                                                   "src_mac:"+service.getSrcMacSer());
                 }
                 if(service.getSrcMacSer()=="00:00:00:00:00:00"){
                      throw new CustomiceException("Attribute mac_mask can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+service.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                   "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                   "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                   "vlan:"+service.getVlan().toString());
                 }
                

            }

            if (i instanceof StorageEndpoint){
                StorageEndpoint service=(StorageEndpoint) i;
                type="StorageEndpoint";
                if(i.getNode().toString()==""){
                   throw new CustomiceException("Attribute device can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "port:"+i.getPort().toString()+"\\"+
                                                  "ip:"+service.getIpPrefix().toString()+"\\"+
                                                  "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                  "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                  "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                   "vlan:"+service.getVlan().toString()+"\\"+
                                                  "src_mac:"+service.getSrcMacSer());
               }
               if(i.getPort().toString()=="0"){
                   throw new CustomiceException("Attribute port can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "device:"+i.getNode().toString()+"\\"+
                                                  "ip:"+service.getIpPrefix().toString()+"\\"+
                                                  "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                  "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                  "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                   "vlan:"+service.getVlan().toString()+"\\"+
                                                  "src_mac:"+service.getSrcMacSer());
               }
                if(service.getVlan().toString()=="None"){
                    throw new CustomiceException("Attribute vlan can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "device:"+i.getNode().toString()+"\\"+
                                                  "port:"+i.getPort().toString()+"\\"+
                                                  "ip:"+service.getIpPrefix().toString()+"\\"+
                                                  "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                  "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                  "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                  "src_mac:"+service.getSrcMacSer());
               }
               
                if(service.getIpPrefix()==null){
                     throw new CustomiceException("Attribute ip can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "device:"+i.getNode().toString()+"\\"+
                                                  "port:"+i.getPort().toString()+"\\"+
                                                  "vlan:"+service.getVlan().toString()+"\\"+
                                                  "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                  "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                  "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                  "src_mac:"+service.getSrcMacSer());
               }
                if(service.getSrcMacMask().toString()=="00:00:00:00:00:00"){
                     throw new CustomiceException("Attribute src_mac_mask can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "device:"+i.getNode().toString()+"\\"+
                                                  "port:"+i.getPort().toString()+"\\"+
                                                  "ip:"+service.getIpPrefix().toString()+"\\"+
                                                  "vlan:"+service.getVlan().toString()+"\\"+
                                                  "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                  "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                  "src_mac:"+service.getSrcMacSer());
               }
                if(service.getExternalAccessFlagSer()==null){
                     throw new CustomiceException("Attribute external_access can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "device:"+i.getNode().toString()+"\\"+
                                                  "port:"+i.getPort().toString()+"\\"+
                                                  "ip:"+service.getIpPrefix().toString()+"\\"+
                                                  "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                  "vlan:"+service.getVlan().toString()+"\\"+
                                                  "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                  "src_mac:"+service.getSrcMacSer());
               }
                if(service.getBypassEnabledSer()==""|| service.getBypassEnabledSer()==null){
                     throw new CustomiceException("Attribute bypass_enabled can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "device:"+i.getNode().toString()+"\\"+
                                                  "port:"+i.getPort().toString()+"\\"+
                                                  "ip:"+service.getIpPrefix().toString()+"\\"+
                                                  "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                  "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                  "vlan:"+service.getVlan().toString()+"\\"+
                                                  "src_mac:"+service.getSrcMacSer());
                }
                if(service.getSrcMacSer()=="00:00:00:00:00:00"){
                     throw new CustomiceException("Attribute mac_mask can not be null \\"+
                                                  "Endpoint:"+type+"\\"+
                                                  "device:"+i.getNode().toString()+"\\"+
                                                  "port:"+i.getPort().toString()+"\\"+
                                                  "ip:"+service.getIpPrefix().toString()+"\\"+
                                                  "src_mac_mask:"+service.getSrcMacMask().toString()+"\\"+
                                                  "external_access:"+service.getExternalAccessFlagSer()+"\\"+
                                                  "bypass_enabled:"+service.getBypassEnabledSer()+"\\"+
                                                  "vlan:"+service.getVlan().toString());
                }
               

           }
             if (i instanceof ExternalServiceEndpoint){
                type="ExternalServiceEndpoint";
                ExternalServiceEndpoint externalService=(ExternalServiceEndpoint) i;

                if(i.getNode().toString()==""){
                    throw new CustomiceException("Attribute device can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+externalService.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+externalService.getSrcMacMask().toString()+"\\"+
                                                   "vlan:"+externalService.getVlan().toString()+"\\"+
                                                     "mac:"+i.getMac().toString()+"\\"+
                                                   "src_mac:"+externalService.getSrcMacSer());
                }
                if(i.getPort().toString()=="0"){
                    throw new CustomiceException("Attribute port can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "ip:"+externalService.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+externalService.getSrcMacMask().toString()+"\\"+
                                                   "vlan:"+externalService.getVlan().toString()+"\\"+
                                                     "mac:"+i.getMac().toString()+"\\"+
                                                   "src_mac:"+externalService.getSrcMacSer());
                }

                 if(i.getMac().toString()=="00:00:00:00:00:00"){
                      throw new CustomiceException("Attribute mac can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+externalService.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+externalService.getSrcMacMask().toString()+"\\"+
                                                   "vlan:"+externalService.getVlan().toString()+"\\"+
                                                   "src_mac:"+externalService.getSrcMacSer());
                 }
                
                 if(externalService.getVlan().toString()=="None"){
                    throw new CustomiceException("Attribute vlan can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+externalService.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+externalService.getSrcMacMask().toString()+"\\"+
                                                   "mac:"+i.getMac().toString()+"\\"+
                                                   "src_mac:"+externalService.getSrcMacSer());
                }
                
                 if(externalService.getIpPrefix()==null){
                     throw new CustomiceException("Attribute ip can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "mac:"+i.getMac().toString()+"\\"+
                                                   "src_mac_mask:"+externalService.getSrcMacMask().toString()+"\\"+
                                                   "vlan:"+externalService.getVlan().toString()+"\\"+
                                                   "src_mac:"+externalService.getSrcMacSer());
                }
                 if(externalService.getSrcMacMask().toString()=="00:00:00:00:00:00"){
                     throw new CustomiceException("Attribute src_mac_mask can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+externalService.getIpPrefix().toString()+"\\"+
                                                   "mac:"+i.getMac().toString()+"\\"+
                                                   "vlan:"+externalService.getVlan().toString()+"\\"+
                                                   "src_mac:"+externalService.getSrcMacSer());
                }
                 
            
                 if(externalService.getSrcMacSer()=="00:00:00:00:00:00"){
                     throw new CustomiceException("Attribute src_mac can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip:"+externalService.getIpPrefix().toString()+"\\"+
                                                   "src_mac_mask:"+externalService.getSrcMacMask().toString()+"\\"+
                                                   "vlan:"+externalService.getVlan().toString()+"\\"+
                                                   "mac:"+i.getMac().toString());
                 }
                

            }
             if (i instanceof VpdcEndpoint){
                 type="VpdcEndpoint";
                 VpdcEndpoint vpdc=(VpdcEndpoint) i;
                 if(i.getNode().toString()==""){
                    throw new CustomiceException("Attribute device can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "mac:"+i.getMac().toString()+"\\"+
                                                   "ip_client:"+vpdc.getIpClient().toString()+"\\"+
                                                   "ip_service:"+vpdc.getIpService().toString());
                }
                if(i.getPort().toString()=="0"){
                   throw new CustomiceException("Attribute port can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "mac:"+i.getMac().toString()+"\\"+
                                                   "ip_client:"+vpdc.getIpClient().toString()+"\\"+
                                                   "ip_service:"+vpdc.getIpService().toString());
                }
                 if(i.getMac().toString()=="00:00:00:00:00:00"){
                     throw new CustomiceException("Attribute mac can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip_client:"+vpdc.getIpClient().toString()+"\\"+
                                                   "ip_service:"+vpdc.getIpService().toString());
    
                 }
                 

                 if(vpdc.getIpClient()==null){
                     throw new CustomiceException("Attribute ip_client can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "mac:"+i.getMac().toString()+"\\"+
                                                   "ip_service:"+vpdc.getIpService().toString());
                }
                 if(vpdc.getIpService()==null){
                     throw new CustomiceException("Attribute ip_service can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip_client:"+vpdc.getIpClient().toString()+"\\"+
                                                   "mac:"+i.getMac().toString());
                }
                

            }
             if (i instanceof VpdcHostEndpoint){
                 type="VpdcHostEndpoint";
                 VpdcHostEndpoint vpdcHost=(VpdcHostEndpoint) i;
                 if(i.getNode().toString()==""){
                    throw new CustomiceException("Attribute device not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "vlan:"+vpdcHost.getVlan().toString()+"\\"+
                                                   "ip_client_list:"+vpdcHost.getIpClientList().toString()+"\\"+
                                                   "ip_service_list:"+vpdcHost.getIpServiceList().toString()+"\\"+
                                                   "external_access_clients:"+vpdcHost.getExternalAccessFlagSer()+"\\"+
                                                   "olt_id:"+vpdcHost.getOltUUID().toString());
                }
                if(i.getPort().toString()=="0"){
                    throw new CustomiceException("Attribute port can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "vlan:"+vpdcHost.getVlan().toString()+"\\"+
                                                   "ip_client_list:"+vpdcHost.getIpClientList().toString()+"\\"+
                                                   "ip_service_list:"+vpdcHost.getIpServiceList().toString()+"\\"+
                                                   "external_access_clients:"+vpdcHost.getExternalAccessFlagSer()+"\\"+
                                                   "olt_id:"+vpdcHost.getOltUUID().toString());
                }

                 if(vpdcHost.getVlan().toString()=="None"){
                    throw new CustomiceException("Attribute vlan can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip_client_list:"+vpdcHost.getIpClientList().toString()+"\\"+
                                                   "ip_service_list:"+vpdcHost.getIpServiceList().toString()+"\\"+
                                                   "external_access_clients:"+vpdcHost.getExternalAccessFlagSer()+"\\"+
                                                   "olt_id:"+vpdcHost.getOltUUID().toString());
                }
                 if(vpdcHost.getOltUUID()==null){
                     throw new CustomiceException("Attribute olt_id can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip_client_list:"+vpdcHost.getIpClientList().toString()+"\\"+
                                                   "ip_service_list:"+vpdcHost.getIpServiceList().toString()+"\\"+
                                                   "external_access_clients:"+vpdcHost.getExternalAccessFlagSer()+"\\"+
                                                   "vlan:"+vpdcHost.getVlan().toString());
                 }
                  if(vpdcHost.getIpClientList()==null){
                     throw new CustomiceException("Attribute ip_client_list can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "vlan:"+vpdcHost.getVlan().toString()+"\\"+
                                                   "ip_service_list:"+vpdcHost.getIpServiceList().toString()+"\\"+
                                                   "external_access_clients:"+vpdcHost.getExternalAccessFlagSer()+"\\"+
                                                   "olt_id:"+vpdcHost.getOltUUID().toString());
                 }
                  if(vpdcHost.getIpServiceList()==null){
                     throw new CustomiceException("Attribute ip_service_list can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip_client_list:"+vpdcHost.getIpClientList().toString()+"\\"+
                                                   "service:"+vpdcHost.getVlan().toString()+"\\"+
                                                   "external_access_clients:"+vpdcHost.getExternalAccessFlagSer()+"\\"+
                                                   "olt_id:"+vpdcHost.getOltUUID().toString());
                 }
                  if(vpdcHost.getExternalAccessFlagSer()==null){
                     throw new CustomiceException("Attribute external_access_clients can not be null \\"+
                                                   "Endpoint:"+type+"\\"+
                                                   "device:"+i.getNode().toString()+"\\"+
                                                   "port:"+i.getPort().toString()+"\\"+
                                                   "ip_client_list:"+vpdcHost.getIpClientList().toString()+"\\"+
                                                   "ip_service_list:"+vpdcHost.getIpServiceList().toString()+"\\"+
                                                   "vlan:"+vpdcHost.getVlan().toString()+"\\"+
                                                   "olt_id:"+vpdcHost.getOltUUID().toString());
                 }

            }
             
            
        }
    }

}