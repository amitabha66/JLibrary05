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
 * $Id: Fault1_1Impl.java,v 1.1.1.1 2006/01/27 13:10:57 kumarjayanti Exp $
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

import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFaultElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.Name;
import javax.xml.soap.Name;

import com.sun.xml.messaging.saaj.soap.SOAPDocument;
import com.sun.xml.messaging.saaj.soap.SOAPDocumentImpl;
import com.sun.xml.messaging.saaj.soap.impl.*;
import com.sun.xml.messaging.saaj.soap.name.NameImpl;
import com.sun.xml.messaging.saaj.util.LogDomainConstants;
import com.sun.xml.messaging.saaj.SOAPExceptionImpl;


public class Fault1_1Impl extends FaultImpl {

    protected static Logger log =
        Logger.getLogger(
            LogDomainConstants.SOAP_VER1_1_DOMAIN,
            "com.sun.xml.messaging.saaj.soap.ver1_1.LocalStrings");

    public Fault1_1Impl(SOAPDocumentImpl ownerDocument, String prefix) {
       super(ownerDocument, NameImpl.createFault1_1Name(prefix));
    }

    protected NameImpl getDetailName() {
        return NameImpl.createDetail1_1Name();
    }

    protected NameImpl getFaultCodeName() {
        return NameImpl.createFromUnqualifiedName("faultcode");
    }

    protected NameImpl getFaultStringName() {
        return NameImpl.createFromUnqualifiedName("faultstring");
    }

    protected NameImpl getFaultActorName() {
        return NameImpl.createFromUnqualifiedName("faultactor");
    }

    protected DetailImpl createDetail() {
        return new Detail1_1Impl(
                       ((SOAPDocument) getOwnerDocument()).getDocument());
    }

    protected FaultElementImpl createSOAPFaultElement(String localName) {
        return new FaultElement1_1Impl(
                       ((SOAPDocument) getOwnerDocument()).getDocument(),
                       localName);
    }

    protected void checkIfStandardFaultCode(String faultCode, String uri)
        throws SOAPException {
        // SOAP 1.1 doesn't seem to mandate using faultcode from a particular
        // set of values.
        // Also need to be backward compatible.
    }

    protected void finallySetFaultCode(String faultcode) throws SOAPException {
        this.faultCodeElement.addTextNode(faultcode);
    }

    public String getFaultCode() {
        if (this.faultCodeElement == null)
            findFaultCodeElement();
        return this.faultCodeElement.getValue();
    }

    public Name getFaultCodeAsName() {

        String faultcodeString = getFaultCode();
        if (faultcodeString == null) {
            return null;
        }
        int prefixIndex = faultcodeString.indexOf(':');
        if (prefixIndex == -1) {
            // Not a valid SOAP message, but we return the unqualified name
            // anyway since some apps do not strictly conform to SOAP
            // specs.  A message that does not contain a <faultcode>
            // element itself is also not valid in which case we return
            // null instead of throwing an exception so this is consistent.
            return NameImpl.createFromUnqualifiedName(faultcodeString);
        }

        // Get the prefix and map it to a namespace name (AKA namespace URI)
        String prefix = faultcodeString.substring(0, prefixIndex);
        if (this.faultCodeElement == null)
            findFaultCodeElement();
        String nsName = this.faultCodeElement.getNamespaceURI(prefix);
        return NameImpl.createFromQualifiedName(faultcodeString, nsName);
    }

    public QName getFaultCodeAsQName() {
        String faultcodeString = getFaultCode();
        if (faultcodeString == null) {
            return null;
        }
        if (this.faultCodeElement == null)
            findFaultCodeElement();
        return convertCodeToQName(faultcodeString, this.faultCodeElement);
    }

    public void setFaultString(String faultString) throws SOAPException {

        if (this.faultStringElement == null)
            findFaultStringElement();

        if (this.faultStringElement == null)
            this.faultStringElement = addSOAPFaultElement("faultstring");
        else {
            this.faultStringElement.removeContents();
            //this.faultStringElement.removeAttributeNS("http://www.w3.org/XML/1998/namespace", "lang");
            this.faultStringElement.removeAttribute("xml:lang");
        }

        this.faultStringElement.addTextNode(faultString);
    }

    public String getFaultString() {
        if (this.faultStringElement == null)
            findFaultStringElement();
        return this.faultStringElement.getValue();

    }

    public Locale getFaultStringLocale() {
        if (this.faultStringElement == null)
            findFaultStringElement();
        if (this.faultStringElement != null) {
            String xmlLangAttr =
                this.faultStringElement.getAttributeValue(
                    NameImpl.createFromUnqualifiedName("xml:lang"));
            if (xmlLangAttr != null)
                return xmlLangToLocale(xmlLangAttr);
        }
        return null;
    }

    public void setFaultString(String faultString, Locale locale)
        throws SOAPException {
        setFaultString(faultString);
        this.faultStringElement.addAttribute(
            NameImpl.createFromTagName("xml:lang"),
            localeToXmlLang(locale));
    }

    protected boolean isStandardFaultElement(String localName) {
        if (localName.equalsIgnoreCase("detail") ||
            localName.equalsIgnoreCase("faultcode") ||
            localName.equalsIgnoreCase("faultstring") ||
            localName.equalsIgnoreCase("faultactor")) {
            return true;
        }
        return false;
    }

    public void appendFaultSubcode(QName subcode) {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "appendFaultSubcode");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public void removeAllFaultSubcodes() {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "removeAllFaultSubcodes");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public Iterator getFaultSubcodes() {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "getFaultSubcodes");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public String getFaultReasonText(Locale locale) {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "getFaultReasonText");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public Iterator getFaultReasonTexts() {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "getFaultReasonTexts");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public Iterator getFaultReasonLocales() {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "getFaultReasonLocales");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public void addFaultReasonText(String text, java.util.Locale locale)
        throws SOAPException {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "addFaultReasonText");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public String getFaultRole() {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "getFaultRole");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public void setFaultRole(String uri) {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "setFaultRole");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public String getFaultNode() {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "getFaultNode");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    public void setFaultNode(String uri) {
        log.log(
            Level.SEVERE,
            "SAAJ0303.ver1_1.msg.op.unsupported.in.SOAP1.1",
            "setFaultNode");
        throw new UnsupportedOperationException("Not supported in SOAP 1.1");
    }

    protected QName getDefaultFaultCode() {
        return new QName(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "Server");
    }

    public SOAPElement addChildElement(SOAPElement element)
        throws SOAPException {
        String localName = element.getLocalName();
        if ("Detail".equalsIgnoreCase(localName)) {
            if (hasDetail()) {
                log.severe("SAAJ0305.ver1_2.detail.exists.error");
                throw new SOAPExceptionImpl("Cannot add Detail, Detail already exists");
            }
        }
        return super.addChildElement(element);
    }
}
