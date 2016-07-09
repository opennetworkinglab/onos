package org.onosproject.netl3vpn.util;

import org.onosproject.netl3vpn.entity.WebAc;
import org.onosproject.netl3vpn.entity.WebL2Access;
import org.onosproject.netl3vpn.entity.WebL2Access.L2AccessType;
import org.onosproject.netl3vpn.entity.WebL3Access;
import org.onosproject.netl3vpn.entity.WebPort;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.Ac;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac.L2Access;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac.L3Access;
import org.onosproject.yang.gen.v1.net.l3vpn.type.rev20160701.netl3vpntype.l2access.Port;

public final class ConvertUtil {
    private ConvertUtil() {
        
    }
    
    public static String handleEnumValue(String enumValue) {
        StringBuffer strBuf = new StringBuffer();
        if(enumValue != null && !enumValue.equals("")) {
            if(enumValue.contains("-")) {
                String[] enumValues = enumValue.split("-");
                for(String str : enumValues) {
                    char ch = str.charAt(0);
                    strBuf.append(Character.toUpperCase(ch));
                    strBuf.append(str.subSequence(1, str.length()));
                }
            } else {
                char ch = enumValue.charAt(0);
                strBuf.append(Character.toUpperCase(ch));
                strBuf.append(enumValue.subSequence(1, enumValue.length()));
            }
            return strBuf.toString();
        }
        return null;
    }
    
    public static WebAc convertToWebAc(Ac ac) {
        WebAc webAc = new WebAc();
        webAc.setId(ac.id());
        webAc.setNeId(ac.neId());
        webAc.setL2Access(convertToWebL2Access(ac.l2Access()));
        webAc.setL3Access(convertToWebL3Access(ac.l3Access()));
        return webAc;
    }
    
    public static WebL2Access convertToWebL2Access(L2Access l2Access) {
        WebL2Access webL2Access = new WebL2Access();
        webL2Access.setAccessType(L2AccessType.valueOf(handleEnumValue(l2Access.accessType())));
        webL2Access.setPort(convertToWebPort(l2Access.port()));
        return webL2Access;
    }
    
    public static WebPort convertToWebPort(Port port) {
        WebPort webPort = new WebPort();
        webPort.setLtpId(port.ltpId());
        return webPort;
    }
    
    public static WebL3Access convertToWebL3Access(L3Access l3Access) {
        WebL3Access webL3Access = new WebL3Access();
        webL3Access.setAddress(l3Access.address().string());
        return webL3Access;
    }
}
