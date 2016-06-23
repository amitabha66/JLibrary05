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
 * $Id: BodyImpl.java,v 1.1.1.1 2006/01/27 13:10:56 kumarjayanti Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2006/01/27 13:10:56 $
 */

/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.messaging.saaj.soap.impl;

import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

import com.sun.xml.messaging.saaj.SOAPExceptionImpl;
import com.sun.xml.messaging.saaj.soap.SOAPDocument;
import com.sun.xml.messaging.saaj.soap.SOAPDocumentImpl;
import com.sun.xml.messaging.saaj.soap.name.NameImpl;

/**
 * The implementation of SOAP-ENV:BODY or the SOAPBody abstraction.
 *
 * @author Anil Vijendran (anil@sun.com)
 */
public abstract class BodyImpl extends ElementImpl implements SOAPBody {
    private SOAPFault fault;

    protected BodyImpl(SOAPDocumentImpl ownerDoc, NameImpl bodyName) {
        super(ownerDoc, bodyName);
    }

    protected abstract NameImpl getFaultName(String name);
    protected abstract boolean isFault(SOAPElement child);
    protected abstract SOAPBodyElement createBodyElement(Name name);
    protected abstract SOAPBodyElement createBodyElement(QName name);
    protected abstract SOAPFault createFaultElement();
    protected abstract QName getDefaultFaultCode();

    public SOAPFault addFault() throws SOAPException {
        if (hasFault()) {
            log.severe("SAAJ0110.impl.fault.already.exists");
            throw new SOAPExceptionImpl("Error: Fault already exists");
        }

        fault = createFaultElement();

        addNode(fault);

        fault.setFaultCode(getDefaultFaultCode());
        fault.setFaultString("Fault string, and possibly fault code, not set");

        return fault;
    }

    public SOAPFault addFault(
        Name faultCode,
        String faultString,
        Locale locale)
        throws SOAPException {
        
        SOAPFault fault = addFault();
        fault.setFaultCode(faultCode);
        fault.setFaultString(faultString, locale);
        return fault;
    }

   public SOAPFault addFault(
        QName faultCode,
        String faultString,
        Locale locale)
        throws SOAPException {

        SOAPFault fault = addFault();
        fault.setFaultCode(faultCode);
        fault.setFaultString(faultString, locale);
        return fault;
    }

    public SOAPFault addFault(Name faultCode, String faultString)
        throws SOAPException {

        SOAPFault fault = addFault();
        fault.setFaultCode(faultCode);
        fault.setFaultString(faultString);
        return fault;
    }

    public SOAPFault addFault(QName faultCode, String faultString)
        throws SOAPException {

        SOAPFault fault = addFault();
        fault.setFaultCode(faultCode);
        fault.setFaultString(faultString);
        return fault;
    }

    void initializeFault() {
        FaultImpl flt = (FaultImpl) findFault();
        fault = flt;
    }

    protected SOAPElement findFault() {
        Iterator eachChild = getChildElementNodes();
        while (eachChild.hasNext()) {
            SOAPElement child = (SOAPElement) eachChild.next();
            if (isFault(child)) {
                return child;
            }
        }

        return null;
    }

    public boolean hasFault() {
        initializeFault();
        return fault != null;
    }

    public SOAPFault getFault() {
        if (hasFault())
            return fault;
        return null;
    }

    public SOAPBodyElement addBodyElement(Name name) throws SOAPException {
        SOAPBodyElement newBodyElement =
            (SOAPBodyElement) ElementFactory.createNamedElement(
                ((SOAPDocument) getOwnerDocument()).getDocument(),
                name.getLocalName(),
                name.getPrefix(),
                name.getURI());
        if (newBodyElement == null) {
            newBodyElement = createBodyElement(name);
        }
        addNode(newBodyElement);
        return newBodyElement;
    }

    public SOAPBodyElement addBodyElement(QName qname) throws SOAPException {
        SOAPBodyElement newBodyElement =
            (SOAPBodyElement) ElementFactory.createNamedElement(
                ((SOAPDocument) getOwnerDocument()).getDocument(),
                qname.getLocalPart(),
                qname.getPrefix(),
                qname.getNamespaceURI());
        if (newBodyElement == null) {
            newBodyElement = createBodyElement(qname);
        }
        addNode(newBodyElement);
        return newBodyElement;
    }

    public void setParentElement(SOAPElement element) throws SOAPException {

        if (!(element instanceof SOAPEnvelope)) {
            log.severe("SAAJ0111.impl.body.parent.must.be.envelope");
            throw new SOAPException("Parent of SOAPBody has to be a SOAPEnvelope");
        }
        super.setParentElement(element);
    }

    protected SOAPElement addElement(Name name) throws SOAPException {
        return addBodyElement(name);
    }

    protected SOAPElement addElement(QName name) throws SOAPException {
        return addBodyElement(name);
    }

    //    public Node insertBefore(Node newElement, Node ref) throws DOMException {
    //        if (!(newElement instanceof SOAPBodyElement) && (newElement instanceof SOAPElement)) {
    //            newElement = new ElementWrapper((ElementImpl) newElement);
    //        }
    //        return super.insertBefore(newElement, ref);
    //    }
    //
    //    public Node replaceChild(Node newElement, Node ref) throws DOMException {
    //        if (!(newElement instanceof SOAPBodyElement) && (newElement instanceof SOAPElement)) {
    //            newElement = new ElementWrapper((ElementImpl) newElement);
    //        }
    //        return super.replaceChild(newElement, ref);
    //    }

    public SOAPBodyElement addDocument(Document document)
        throws SOAPException {
        /*
                
                Element rootNode =
                    document.getDocumentElement();
                // Causes all deferred nodes to be inflated
                rootNode.normalize();
                adoptElement(rootNode);
                SOAPBodyElement bodyElement = (SOAPBodyElement) convertToSoapElement(rootNode);
                addNode(bodyElement);
                return bodyElement;
        */
        ///*
        SOAPBodyElement newBodyElement = null;
        DocumentFragment docFrag = document.createDocumentFragment();
        Element rootElement = document.getDocumentElement();
        if(rootElement != null) {
            docFrag.appendChild(rootElement);

            Document ownerDoc = getOwnerDocument();
            // This copies the whole tree which could be very big so it's slow.
            // However, it does have the advantage of actually working.
            org.w3c.dom.Node replacingNode = ownerDoc.importNode(docFrag, true);
            // Adding replacingNode at the last of the children list of body
            addNode(replacingNode);
            Iterator i =
                getChildElements(NameImpl.copyElementName(rootElement));
            // Return the child element with the required name which is at the
            // end of the list
            while(i.hasNext())
                newBodyElement = (SOAPBodyElement) i.next();
        }
        return newBodyElement;
        //*/        
    }

    protected SOAPElement convertToSoapElement(Element element) {
        if ((element instanceof SOAPBodyElement) &&
            //this check is required because ElementImpl currently 
            // implements SOAPBodyElement
            !(element.getClass().equals(ElementImpl.class))) {
            return (SOAPElement) element;
        } else {
            return replaceElementWithSOAPElement(
                element,
                (ElementImpl) createBodyElement(NameImpl
                    .copyElementName(element)));
        }
    }

    public SOAPElement setElementQName(QName newName) throws SOAPException {
        log.log(Level.SEVERE,
                "SAAJ0146.impl.invalid.name.change.requested",
                new Object[] {elementQName.getLocalPart(),
                              newName.getLocalPart()});
        throw new SOAPException("Cannot change name for "
                                + elementQName.getLocalPart() + " to "
                                + newName.getLocalPart());
    }

    public Document extractContentAsDocument() throws SOAPException {

        Iterator eachChild = getChildElements();
        javax.xml.soap.Node firstBodyElement = null;

        while (eachChild.hasNext() &&
               !(firstBodyElement instanceof SOAPElement))
            firstBodyElement = (javax.xml.soap.Node) eachChild.next();

        boolean exactlyOneChildElement = true;
        if (firstBodyElement == null)
            exactlyOneChildElement = false;
        else {
            for (org.w3c.dom.Node node = firstBodyElement.getNextSibling();
                 node != null;
                 node = node.getNextSibling()) {

                if (node instanceof Element) {
                    exactlyOneChildElement = false;
                    break;
                }
            }
        }

        if(!exactlyOneChildElement) {
            log.log(Level.SEVERE,
                    "SAAJ0250.impl.body.should.have.exactly.one.child");
            throw new SOAPException("Cannot extract Document from body");
        }

        Document document = null;
        try {
            DocumentBuilderFactory factory = 
                new com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();

            Element rootElement = (Element) document.importNode(
                                                firstBodyElement,
                                                true);

            document.appendChild(rootElement);

        } catch(Exception e) {
            log.log(Level.SEVERE,
                    "SAAJ0251.impl.cannot.extract.document.from.body");
            throw new SOAPExceptionImpl(
                "Unable to extract Document from body", e);
        }
            
        firstBodyElement.detachNode();

        return document;
    }

}
