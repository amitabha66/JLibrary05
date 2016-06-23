/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package inline_customize.client;

import java.rmi.RemoteException;

public class AddNumbersClient {
    private MathUtil port;
    
    public AddNumbersClient () {
        port = new MathUtilService().getMathUtil ();
    }
    
    public static void main (String[] args) {
        try {
            AddNumbersClient client = new AddNumbersClient ();
            //invoke synchronous method
            client.invoke ();
        } catch (RemoteException e) {
            e.printStackTrace ();
        } catch(MathUtilException e){
            System.out.println ("\tException detail: "+ e.getMessage ()+", "+e.getFaultInfo ());
        }
    }
    
    private void invoke () throws RemoteException, MathUtilException{
        int number1 = 10;
        int number2 = 20;
        
        System.out.printf ("Invoking addNumbers(%d, %d)\n", number1, number2);
        int result = port.add (number1, number2);
        System.out.printf ("The result of adding %d and %d is %d.\n\n", number1, number2, result);
        
        //lets make endpoint throw exception
        number1 = -10;
        System.out.printf ("Invoking addNumbers(%d, %d) and expect exception.\n", number1, number2);
        result = port.add (number1, number2);
    }
}
