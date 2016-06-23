/**
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the "License").  You may not use this file except
 in compliance with the License.
 
 You can obtain a copy of the license at
 https://jwsdp.dev.java.net/CDDLv1.0.html
 See the License for the specific language governing
 permissions and limitations under the License.
 
 When distributing Covered Code, include this CDDL
 HEADER in each file and include the License file at
 https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 add the following below this CDDL HEADER, with the
 fields enclosed by brackets "[]" replaced with your
 own identifying information: Portions Copyright [yyyy]
 [name of copyright owner]
**/
package wsimport_catalog;

public class WsimportCatalogTester{
    public static void main (String[] args) {
        testCatalog();
    }

    /**
     * Just check if the class is loaded, meaning both -p worked and catalog resolver worked too
     */
    public static void testCatalog(){
        try {
            Class cls = Class.forName("wsimport_catalog.Hello");
            System.out.println("wsimport_catalog sample: Succeessfuly resolved wsdl and schema and generated artifacts!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("wsimport_catalog sample failed!");
        }
    }
}
