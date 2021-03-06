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
/*
 * $Id: SOAPFactory1_1Impl.java,v 1.1.1.1 2006/01/27 13:10:57 kumarjayanti Exp $
 */

/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
*
* @author SAAJ RI Development Team
*/
package com.sun.xml.messaging.saaj.soap.ver1_1;

import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;

import javax.xml.namespace.QName;

import com.sun.xml.messaging.saaj.soap.SOAPDocumentImpl;
import com.sun.xml.messaging.saaj.soap.SOAPFactoryImpl;

public class SOAPFactory1_1Impl extends SOAPFactoryImpl {
    protected SOAPDocumentImpl createDocument() {
        return (new SOAPPart1_1Impl()).getDocument();
    }

    public Detail createDetail() throws SOAPException {
        return new Detail1_1Impl(createDocument());
    }

    public SOAPFault createFault(String reasonText, QName faultCode) 
        throws SOAPException {
        Fault1_1Impl fault = new Fault1_1Impl(createDocument(), null);
        fault.setFaultString(reasonText);
        fault.setFaultCode(faultCode);
        return fault;
    }

    public SOAPFault createFault() throws SOAPException {
        Fault1_1Impl fault = new Fault1_1Impl(createDocument(), null);
        fault.setFaultCode(fault.getDefaultFaultCode());
        fault.setFaultString("Fault string, and possibly fault code, not set");
        return fault;
    }
}
