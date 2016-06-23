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
package inline_customize.server;

@javax.jws.WebService (serviceName="AddNumbersService", targetNamespace="http://duke.org", endpointInterface="inline_customize.server.MathUtil")
public class AddNumbersImpl implements MathUtil{
    
    /**
     * @param number1
     * @param number2
     * @return The sum
     * @throws MathUtilException
     *             if any of the numbers to be added is negative.
     */
    public int add (int number1, int number2) throws MathUtilException {
        if(number1 < 0 || number2 < 0){
            throw new MathUtilException ("Negative number cant be added!", "Numbers: "+number1+", "+number2);
        }
        return number1 + number2;
    }
    
}
