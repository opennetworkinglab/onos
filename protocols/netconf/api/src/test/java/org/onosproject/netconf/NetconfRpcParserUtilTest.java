/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.netconf;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.onosproject.netconf.rpc.ErrorSeverity;
import org.onosproject.netconf.rpc.ErrorTag;
import org.onosproject.netconf.rpc.ErrorType;
import org.w3c.dom.Node;

public class NetconfRpcParserUtilTest {

    static final String OK_DATA1 =
    "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"3\">\n" +
    "  <ok/>\n" +
    "</rpc-reply>\n";

    static final String ERROR_XPATH1 =
    "/rpc/edit-config/config/oc-if:interfaces/oc-if:interface[oc-if:name='foo']/oc-if:config/oc-if:type";

    static final String ERROR_DATA1 =
    "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"4\">\n" +
    "  <rpc-error>\n" +
    "    <error-type>application</error-type>\n" +
    "    <error-tag>invalid-value</error-tag>\n" +
    "    <error-severity>error</error-severity>\n" +
    "    <error-path xmlns:oc-if=\"http://openconfig.net/yang/interfaces\">\n" +
    "    /rpc/edit-config/config/oc-if:interfaces/oc-if:interface[oc-if:name='foo']/oc-if:config/oc-if:type\n" +
    "  </error-path>\n" +
    "    <error-message xml:lang=\"en\">\"fastEther\" is not a valid value.</error-message>\n" +
    "    <error-info>\n" +
    "      <bad-element>type</bad-element>\n" +
    "      <something>value</something>" +
    "    </error-info>\n" +
    "  </rpc-error>\n" +
    "</rpc-reply>";

    static final String RESPONSE_BODY1 =
    "<data>\n" +
    "    <interfaces xmlns=\"http://openconfig.net/yang/interfaces\">\n" +
    "      <interface>\n" +
    "        <name>foo</name>\n" +
    "        <config>\n" +
    "          <name>foo</name>\n" +
    "          <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:fastEther</type>\n" +
    "        </config>\n" +
    "      </interface>\n" +
    "    </interfaces>\n" +
    "    <components xmlns=\"http://openconfig.net/yang/platform\">\n" +
    "      <component>\n" +
    "        <name>comp1</name>\n" +
    "        <config>\n" +
    "          <name>comp1</name>\n" +
    "        </config>\n" +
    "      </component>\n" +
    "      <component>\n" +
    "        <name>comp2</name>\n" +
    "        <config>\n" +
    "          <name>comp2</name>\n" +
    "        </config>\n" +
    "      </component>\n" +
    "    </components>\n" +
    "    <aaa xmlns=\"http://tail-f.com/ns/aaa/1.1\">\n" +
    "      <authentication>\n" +
    "        <users>\n" +
    "          <user>\n" +
    "            <name>admin</name>\n" +
    "            <uid>9000</uid>\n" +
    "            <gid>20</gid>\n" +
    "            <password>$1$3iwdPGZ1$PBh0MaDjzSFf1jozKYJUI1</password>\n" +
    "            <ssh_keydir>/var/confd/homes/admin/.ssh</ssh_keydir>\n" +
    "            <homedir>/var/confd/homes/admin</homedir>\n" +
    "          </user>\n" +
    "          <user>\n" +
    "            <name>oper</name>\n" +
    "            <uid>9000</uid>\n" +
    "            <gid>20</gid>\n" +
    "            <password>$1$hJImrsyG$Ey294aLU/8wE.Y5vgjqzm/</password>\n" +
    "            <ssh_keydir>/var/confd/homes/oper/.ssh</ssh_keydir>\n" +
    "            <homedir>/var/confd/homes/oper</homedir>\n" +
    "          </user>\n" +
    "          <user>\n" +
    "            <name>private</name>\n" +
    "            <uid>9000</uid>\n" +
    "            <gid>20</gid>\n" +
    "            <password>$1$f85WqsO0$GdtqBqa0yAZXXm5sSCv/M/</password>\n" +
    "            <ssh_keydir>/var/confd/homes/private/.ssh</ssh_keydir>\n" +
    "            <homedir>/var/confd/homes/private</homedir>\n" +
    "          </user>\n" +
    "          <user>\n" +
    "            <name>public</name>\n" +
    "            <uid>9000</uid>\n" +
    "            <gid>20</gid>\n" +
    "            <password>$1$lYiRxyl.$0ofHPegBlwr7asbjz/a/Q.</password>\n" +
    "            <ssh_keydir>/var/confd/homes/public/.ssh</ssh_keydir>\n" +
    "            <homedir>/var/confd/homes/public</homedir>\n" +
    "          </user>\n" +
    "        </users>\n" +
    "      </authentication>\n" +
    "      <ios>\n" +
    "        <level>\n" +
    "          <nr>0</nr>\n" +
    "          <prompt>\\h>\n" +
    "          </prompt>\n" +
    "        </level>\n" +
    "        <level>\n" +
    "          <nr>15</nr>\n" +
    "          <prompt>\\h#\n" +
    "          </prompt>\n" +
    "        </level>\n" +
    "        <privilege>\n" +
    "          <mode>exec</mode>\n" +
    "          <level>\n" +
    "            <nr>0</nr>\n" +
    "            <command>\n" +
    "              <name>action</name>\n" +
    "            </command>\n" +
    "            <command>\n" +
    "              <name>autowizard</name>\n" +
    "            </command>\n" +
    "            <command>\n" +
    "              <name>enable</name>\n" +
    "            </command>\n" +
    "            <command>\n" +
    "              <name>exit</name>\n" +
    "            </command>\n" +
    "            <command>\n" +
    "              <name>help</name>\n" +
    "            </command>\n" +
    "            <command>\n" +
    "              <name>startup</name>\n" +
    "            </command>\n" +
    "          </level>\n" +
    "          <level>\n" +
    "            <nr>15</nr>\n" +
    "            <command>\n" +
    "              <name>configure</name>\n" +
    "            </command>\n" +
    "          </level>\n" +
    "        </privilege>\n" +
    "      </ios>\n" +
    "    </aaa>\n" +
    "    <nacm xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-acm\">\n" +
    "      <write-default>permit</write-default>\n" +
    "      <groups>\n" +
    "        <group>\n" +
    "          <name>admin</name>\n" +
    "          <user-name>admin</user-name>\n" +
    "          <user-name>private</user-name>\n" +
    "        </group>\n" +
    "        <group>\n" +
    "          <name>oper</name>\n" +
    "          <user-name>oper</user-name>\n" +
    "          <user-name>public</user-name>\n" +
    "        </group>\n" +
    "      </groups>\n" +
    "      <rule-list>\n" +
    "        <name>admin</name>\n" +
    "        <group>admin</group>\n" +
    "        <rule>\n" +
    "          <name>any-access</name>\n" +
    "          <action>permit</action>\n" +
    "        </rule>\n" +
    "      </rule-list>\n" +
    "      <rule-list>\n" +
    "        <name>any-group</name>\n" +
    "        <group>*</group>\n" +
    "        <rule>\n" +
    "          <name>tailf-aaa-authentication</name>\n" +
    "          <module-name>tailf-aaa</module-name>\n" +
    "          <path>/aaa/authentication/users/user[name='$USER']</path>\n" +
    "          <access-operations>read update</access-operations>\n" +
    "          <action>permit</action>\n" +
    "        </rule>\n" +
    "        <rule>\n" +
    "          <name>tailf-aaa-user</name>\n" +
    "          <module-name>tailf-aaa</module-name>\n" +
    "          <path>/user[name='$USER']</path>\n" +
    "          <access-operations>create read update delete</access-operations>\n" +
    "          <action>permit</action>\n" +
    "        </rule>\n" +
    "        <rule>\n" +
    "          <name>tailf-webui-user</name>\n" +
    "          <module-name>tailf-webui</module-name>\n" +
    "          <path>/webui/data-stores/user-profile[username='$USER']</path>\n" +
    "          <access-operations>create read update delete</access-operations>\n" +
    "          <action>permit</action>\n" +
    "        </rule>\n" +
    "      </rule-list>\n" +
    "    </nacm>\n" +
    "  </data>";

    static final String RESPONSE_DATA1 =
    "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"5\">\n" +
    "  " + RESPONSE_BODY1 + "\n" +
    "</rpc-reply>";

    static final String ERROR_DATA2 =
    "<rpc-reply message-id=\"101\"\n" +
    "       xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"\n" +
    "       xmlns:xc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
    "       <rpc-error>\n" +
    "         <error-type>application</error-type>\n" +
    "         <error-tag>invalid-value</error-tag>\n" +
    "         <error-severity>error</error-severity>\n" +
    "         <error-path xmlns:t=\"http://example.com/schema/1.2/config\">\n" +
    "           /t:top/t:interface[t:name=\"Ethernet0/0\"]/t:mtu\n" +
    "         </error-path>\n" +
    "         <error-message xml:lang=\"en\">\n" +
    "           MTU value 25000 is not within range 256..9192\n" +
    "         </error-message>\n" +
    "       </rpc-error>\n" +
    "       <rpc-error>\n" +
    "         <error-type>application</error-type>" +
    "         <error-tag>invalid-value</error-tag>\n" +
    "         <error-severity>error</error-severity>\n" +
    "         <error-path xmlns:t=\"http://example.com/schema/1.2/config\">\n" +
    "           /t:top/t:interface[t:name=\"Ethernet1/0\"]/t:address/t:name\n" +
    "         </error-path>\n" +
    "         <error-message xml:lang=\"en\">\n" +
    "           Invalid IP address for interface Ethernet1/0\n" +
    "         </error-message>\n" +
    "       </rpc-error>\n" +
    "     </rpc-reply>";


    @Test
    public void testOkParse() {
        NetconfRpcReply ok = NetconfRpcParserUtil.parseRpcReply(OK_DATA1);
        assertThat(ok.isOk(), is(true));
        assertThat(ok.messageId(), is("3"));
    }

    @Test
    public void testErrorWithAnyParse() {

        NetconfRpcReply reply = NetconfRpcParserUtil.parseRpcReply(ERROR_DATA1);

        assertThat(reply.messageId(), is("4"));

        List<NetconfRpcError> errs = reply.errors();
        assertThat(errs, hasSize(1));

        NetconfRpcError err = errs.get(0);
        assertThat(err.type(), is(ErrorType.APPLICATION));
        assertThat(err.tag(), is(ErrorTag.INVALID_VALUE));
        assertThat(err.severity(), is(ErrorSeverity.ERROR));
        assertThat(err.path().isPresent(), is(true));
        assertThat(err.path().get().trim(), is(ERROR_XPATH1));
        assertThat(err.message(), is(notNullValue()));
        assertThat(err.message(), is("\"fastEther\" is not a valid value."));
        assertThat(err.info().get(NetconfRpcError.BAD_ELEMENT), is(endsWith("type")));
        assertThat(err.info().size(), is(1));
        assertThat(err.infoAny(), hasSize(1));
        assertThat(err.infoAny().get(0), is(instanceOf(Node.class)));
        assertThat(NetconfRpcParserUtil.toString(err.infoAny().get(0)),
                   is(both(startsWith("<something")).and(endsWith("</something>"))));

        assertThat(err.appTag().isPresent(), is(false));
    }

    @Test
    public void testErrorParse() {

        NetconfRpcReply reply = NetconfRpcParserUtil.parseRpcReply(ERROR_DATA2);

        assertThat(reply.messageId(), is("101"));

        List<NetconfRpcError> errs = reply.errors();
        assertThat(errs, hasSize(2));

        NetconfRpcError err1 = errs.get(0);
        assertThat(err1.type(), is(ErrorType.APPLICATION));
        assertThat(err1.tag(), is(ErrorTag.INVALID_VALUE));
        assertThat(err1.severity(), is(ErrorSeverity.ERROR));
        assertThat(err1.path().isPresent(), is(true));
        assertThat(err1.path().get().trim(), is("/t:top/t:interface[t:name=\"Ethernet0/0\"]/t:mtu"));
        assertThat(err1.message(), is(notNullValue()));
        assertThat(err1.message().trim(), is("MTU value 25000 is not within range 256..9192"));
        assertThat(err1.info().size(), is(0));
        assertThat(err1.infoAny(), hasSize(0));
        assertThat(err1.appTag().isPresent(), is(false));

        NetconfRpcError err2 = errs.get(1);
        assertThat(err2.type(), is(ErrorType.APPLICATION));
        assertThat(err2.tag(), is(ErrorTag.INVALID_VALUE));
        assertThat(err2.severity(), is(ErrorSeverity.ERROR));
        assertThat(err2.path().isPresent(), is(true));
        assertThat(err2.path().get().trim(), is("/t:top/t:interface[t:name=\"Ethernet1/0\"]/t:address/t:name"));
        assertThat(err2.message(), is(notNullValue()));
        assertThat(err2.message().trim(), is("Invalid IP address for interface Ethernet1/0"));
        assertThat(err2.info().size(), is(0));
        assertThat(err2.infoAny(), hasSize(0));
        assertThat(err2.appTag().isPresent(), is(false));
    }

    @Test
    public void testGetResponseParse() {
        NetconfRpcReply rep = NetconfRpcParserUtil.parseRpcReply(RESPONSE_DATA1);
        assertThat(rep.isOk(), is(false));
        assertThat(rep.messageId(), is("5"));

        assertThat(rep.responses(), hasSize(1));
        assertThat(rep.responses().get(0),
                   is(both(startsWith("<data")).and(endsWith("</data>"))));
        // Can't do below: response contains xmlns attribute for netconf base
        //assertThat(rep.responses().get(0), is(RESPONSE_BODY1));
    }

}
