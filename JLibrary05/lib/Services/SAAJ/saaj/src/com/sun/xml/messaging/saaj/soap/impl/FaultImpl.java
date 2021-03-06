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
 * $Id: FaultImpl.java,v 1.1.1.1 2006/01/27 13:10:56 kumarjayanti Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2006/01/27 13:10:56 $
 */

/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.messaging.saaj.soap.impl;

import java.util.Locale;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.soap.*;

import org.w3c.dom.Element;

import com.sun.xml.messaging.saaj.SOAPExceptionImpl;
import com.sun.xml.messaging.saaj.soap.SOAPDocumentImpl;
import com.sun.xml.messaging.saaj.soap.name.NameImpl;

public abstract class FaultImpl extends ElementImpl implements SOAPFault {

    /* This can also represent a fault reason element */
    protected SOAPFaultElement faultStringElement;

    /* This can also represent a fault role element */
    protected SOAPFaultElement faultActorElement;

    protected SOAPFaultElement faultCodeElement;

    protected Detail detail;

    protected FaultImpl(SOAPDocumentImpl ownerDoc, NameImpl name) {
        super(ownerDoc, name);
    }


    protected abstract NameImpl getDetailName();
    protected abstract NameImpl getFaultCodeName();
    protected abstract NameImpl getFaultStringName();
    protected abstract NameImpl getFaultActorName();
    protected abstract DetailImpl createDetail();
    protected abstract FaultElementImpl createSOAPFaultElement(String localName);
    protected abstract void checkIfStandardFaultCode(String faultCode, String uri) throws SOAPException;
    protected abstract void finallySetFaultCode(String faultcode) throws SOAPException;
    protected abstract boolean isStandardFaultElement(String localName);
    protected abstract QName getDefaultFaultCode();


    protected void findFaultCodeElement() {
        this.faultCodeElement =
            (SOAPFaultElement) findChild(getFaultCodeName());
    }

    protected void findFaultActorElement() {
        this.faultActorElement =
            (SOAPFaultElement) findChild(getFaultActorName());
    }

    protected void findFaultStringElement() {
        this.faultStringElement =
            (SOAPFaultElement) findChild(getFaultStringName());
    }

    public void setFaultCode(String faultCode) throws SOAPException {
        setFaultCode(
            NameImpl.getLocalNameFromTagName(faultCode),
            NameImpl.getPrefixFromTagName(faultCode),
            null);
    }

    public void setFaultCode(String faultCode, String prefix, String uri)
        throws SOAPException {

        if (prefix == null || prefix.equals("")) {
            if (uri == null || uri.equals("")) {
                log.severe("SAAJ0140.impl.no.ns.URI");
                throw new SOAPExceptionImpl("No NamespaceURI, SOAP requires faultcode content to be a QName");
            }
            prefix = getNamespacePrefix(uri);
            if (prefix == null || prefix.equals("")) {
                prefix = "ns0";
            }
        }

        if (this.faultCodeElement == null)
            findFaultCodeElement();

        if (this.faultCodeElement == null)
            this.faultCodeElement = addFaultCodeElement();
        else
            this.faultCodeElement.removeContents();
 
        if (uri == null || uri.equals("")) {
            uri = this.faultCodeElement.getNamespaceURI(prefix);
        }
        if (uri == null || uri.equals("")) {
            log.severe("SAAJ0140.impl.no.ns.URI");
            throw new SOAPExceptionImpl("No NamespaceURI, SOAP requires faultcode content to be a QName");
        } else {
            checkIfStandardFaultCode(faultCode, uri);
            ((FaultElementImpl) this.faultCodeElement).ensureNamespaceIsDeclared(prefix, uri);
        }
        finallySetFaultCode(prefix + ":" + faultCode);
    }

    public void setFaultCode(Name faultCodeQName) throws SOAPException {
        setFaultCode(
            faultCodeQName.getLocalName(),
            faultCodeQName.getPrefix(),
            faultCodeQName.getURI());
    }

    public void setFaultCode(QName faultCodeQName) throws SOAPException {
        setFaultCode(
            faultCodeQName.getLocalPart(),
            faultCodeQName.getPrefix(),
            faultCodeQName.getNamespaceURI());
    }

    protected static QName convertCodeToQName(
        String code,
        SOAPElement codeContainingElement) {

        int prefixIndex = code.indexOf(':');
        if (prefixIndex == -1) {
            return new QName(code);
        }

        String prefix = code.substring(0, prefixIndex);
        String nsName =
            ((ElementImpl) codeContainingElement).getNamespaceURI(prefix);
        return new QName(nsName, getLocalPart(code), prefix);
    }

    protected void initializeDetail() {
        NameImpl detailName = getDetailName();
        detail = (Detail) findChild(detailName);
    }

    public Detail getDetail() {
        if (detail == null)
            initializeDetail();
        if ((detail != null) && (detail.getParentNode() == null)) {
        // a detach node was called on it
            detail = null;
        }
        return detail;
    }

    public Detail addDetail() throws SOAPException {
        if (detail == null)
            initializeDetail();
        if (detail == null) {
            detail = createDetail();
            addNode(detail);
            return detail;
        } else {
            // Log
            throw new SOAPExceptionImpl("Error: Detail already exists");
        }
    }

    public boolean hasDetail() {
        return (getDetail() != null);
    }

    public void setFaultActor(String faultActor) throws SOAPException {
        if (this.faultActorElement == null)
            findFaultActorElement();
        if (this.faultActorElement != null)
            this.faultActorElement.detachNode();
        if (faultActor == null)
            return;
        this.faultActorElement =
            addSOAPFaultElement(getFaultActorName().getLocalName());
        this.faultActorElement.addTextNode(faultActor);
    }

    public String getFaultActor() {
        if (this.faultActorElement == null)
            findFaultActorElement();
        if (this.faultActorElement != null) {
                return this.faultActorElement.getValue();
        }
        return null;
    }

    public SOAPElement setElementQName(QName newName) throws SOAPException {
        
        log.log(
            Level.SEVERE,
            "SAAJ0146.impl.invalid.name.change.requested",
            new Object[] {elementQName.getLocalPart(), newName.getLocalPart()});
        throw new SOAPException(
            "Cannot change name for " + elementQName.getLocalPart() + " to " + newName.getLocalPart());
    }

    protected SOAPElement convertToSoapElement(Element element) {
        if (element instanceof SOAPFaultElement) { 
            return (SOAPElement) element;
        } else if (element instanceof SOAPElement) {
            SOAPElement soapElement = (SOAPElement) element;
            if (getDetailName().equals(soapElement.getElementName())) {
                return replaceElementWithSOAPElement(element, createDetail());
            } else {
                String localName =
                    soapElement.getElementName().getLocalName();
                if (isStandardFaultElement(localName))
                    return replaceElementWithSOAPElement(
                               element, 
                               createSOAPFaultElement(localName));
                return soapElement;
            }
        } else {
            Name elementName = NameImpl.copyElementName(element);
            ElementImpl newElement;
            if (getDetailName().equals(elementName)) {
                newElement = (ElementImpl) createDetail();
            } else {
                String localName = elementName.getLocalName();
                if (isStandardFaultElement(localName))
                    newElement =
                        (ElementImpl) createSOAPFaultElement(localName);
                else
                    newElement = (ElementImpl) createElement(elementName);
            }
            return replaceElementWithSOAPElement(element, newElement);
        }
    }

    private SOAPFaultElement addFaultCodeElement() throws SOAPException {
        if (this.faultCodeElement == null)
            findFaultCodeElement();
        if (this.faultCodeElement == null) {
            this.faultCodeElement =
                addSOAPFaultElement(getFaultCodeName().getLocalName());
            return this.faultCodeElement;
        } else {
            throw new SOAPExceptionImpl("Error: Faultcode already exists");
        }
    }

    private SOAPFaultElement addFaultStringElement() throws SOAPException {
        if (this.faultStringElement == null)
            findFaultStringElement();
        if (this.faultStringElement == null) {
            this.faultStringElement =
                addSOAPFaultElement(getFaultStringName().getLocalName());
            return this.faultStringElement;
        } else {
            // Log
            throw new SOAPExceptionImpl("Error: Faultstring already exists");
        }
    }

    private SOAPFaultElement addFaultActorElement() throws SOAPException {
        if (this.faultActorElement == null)
            findFaultActorElement();
        if (this.faultActorElement == null) {
            this.faultActorElement =
                addSOAPFaultElement(getFaultActorName().getLocalName());
            return this.faultActorElement;
        } else {
            // Log
            throw new SOAPExceptionImpl("Error: Faultactor already exists");
        }
    }

    protected SOAPElement addElement(Name name) throws SOAPException {
        if (getDetailName().equals(name)) {
            return addDetail();
        } else if(getFaultCodeName().equals(name)) {
            return addFaultCodeElement();
        } else if(getFaultStringName().equals(name)) {
            return addFaultStringElement();
        } else if(getFaultActorName().equals(name)) {
            return addFaultActorElement();
        }
        return super.addElement(name);
    }

    protected SOAPElement addElement(QName name) throws SOAPException {
        return addElement(NameImpl.convertToName(name));
    }

    protected FaultElementImpl addSOAPFaultElement(String localName) 
        throws SOAPException {

        FaultElementImpl faultElem = createSOAPFaultElement(localName);
        addNode(faultElem);
        return faultElem;
    }

    /**
     * Convert an xml:lang attribute value into a Locale object
     */
    protected static Locale xmlLangToLocale(String xmlLang) {
        if (xmlLang == null) {
            return null;
        }

        // Spec uses hyphen as separator
        int index = xmlLang.indexOf("-");

        // Accept underscore as separator as well
        if (index == -1) {
            index = xmlLang.indexOf("_");
        }

        if (index == -1) {
            // No separator so assume only a language component
            return new Locale(xmlLang, "");
        }

        String language = xmlLang.substring(0, index);
        String country = xmlLang.substring(index + 1);
        return new Locale(language, country);
    }

    protected static String localeToXmlLang(Locale locale) {
        String xmlLang = locale.getLanguage();
        String country = locale.getCountry();
        if (!"".equals(country)) {
            xmlLang += "-" + country;
        }
        return xmlLang;
    }
}
