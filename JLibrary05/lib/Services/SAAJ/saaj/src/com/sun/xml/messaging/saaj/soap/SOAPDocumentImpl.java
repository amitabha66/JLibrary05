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
 * $Id: SOAPDocumentImpl.java,v 1.1.1.1 2006/01/27 13:10:56 kumarjayanti Exp $
 */

/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
*
* @author SAAJ RI Development Team
*/
package com.sun.xml.messaging.saaj.soap;

import java.util.logging.Logger;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
import org.w3c.dom.*;

import com.sun.xml.messaging.saaj.soap.impl.*;
import com.sun.xml.messaging.saaj.soap.name.NameImpl;
import com.sun.xml.messaging.saaj.util.LogDomainConstants;

public class SOAPDocumentImpl extends DocumentImpl implements SOAPDocument {

    protected static Logger log =
        Logger.getLogger(LogDomainConstants.SOAP_DOMAIN,
                         "com.sun.xml.messaging.saaj.soap.LocalStrings");
    
    SOAPPartImpl enclosingSOAPPart;

    public SOAPDocumentImpl(SOAPPartImpl enclosingDocument) {
        this.enclosingSOAPPart = enclosingDocument;
    }

    //    public SOAPDocumentImpl(boolean grammarAccess) {
    //        super(grammarAccess);
    //    }
    //
    //    public SOAPDocumentImpl(DocumentType doctype) {
    //        super(doctype);
    //    }
    //
    //    public SOAPDocumentImpl(DocumentType doctype, boolean grammarAccess) {
    //        super(doctype, grammarAccess);
    //    }

    public SOAPPartImpl getSOAPPart() {
        if (enclosingSOAPPart == null) {
            log.severe("SAAJ0541.soap.fragment.not.bound.to.part");
            throw new RuntimeException("Could not complete operation. Fragment not bound to SOAP part.");
        }
        return enclosingSOAPPart;
    }

    public SOAPDocumentImpl getDocument() {
        return this;
    }

    public DocumentType getDoctype() {
        // SOAP means no DTD, No DTD means no doctype (SOAP 1.2 only?)
        return null;
    }

    public DOMImplementation getImplementation() {
        return super.getImplementation();
    }

    public Element getDocumentElement() {
        // This had better be an Envelope!
        getSOAPPart().doGetDocumentElement();
        return doGetDocumentElement();
    }

    protected Element doGetDocumentElement() {
        return super.getDocumentElement();
    }

    public Element createElement(String tagName) throws DOMException {
        return ElementFactory.createElement(
            this,
            NameImpl.getLocalNameFromTagName(tagName),
            NameImpl.getPrefixFromTagName(tagName),
            null);
    }

    public DocumentFragment createDocumentFragment() {
        return new SOAPDocumentFragment(this);
    }

    public org.w3c.dom.Text createTextNode(String data) {
        return new TextImpl(this, data);
    }

    public Comment createComment(String data) {
        return new CommentImpl(this, data);
    }

    public CDATASection createCDATASection(String data) throws DOMException {
        return new CDATAImpl(this, data);
    }

    public ProcessingInstruction createProcessingInstruction(
        String target,
        String data)
        throws DOMException {
        log.severe("SAAJ0542.soap.proc.instructions.not.allowed.in.docs");
        throw new UnsupportedOperationException("Processing Instructions are not allowed in SOAP documents");
    }

    public Attr createAttribute(String name) throws DOMException {
        return super.createAttribute(name);
    }

    public EntityReference createEntityReference(String name)
        throws DOMException {        
            log.severe("SAAJ0543.soap.entity.refs.not.allowed.in.docs");
            throw new UnsupportedOperationException("Entity References are not allowed in SOAP documents");
    }

    public NodeList getElementsByTagName(String tagname) {
        return super.getElementsByTagName(tagname);
    }

    public org.w3c.dom.Node importNode(Node importedNode, boolean deep)
        throws DOMException {
        return super.importNode(importedNode, deep);
    }

    public Element createElementNS(String namespaceURI, String qualifiedName)
        throws DOMException {
        return ElementFactory.createElement(
            this,
            NameImpl.getLocalNameFromTagName(qualifiedName),
            NameImpl.getPrefixFromTagName(qualifiedName),
            namespaceURI);
    }

    public Attr createAttributeNS(String namespaceURI, String qualifiedName)
        throws DOMException {
        return super.createAttributeNS(namespaceURI, qualifiedName);
    }

    public NodeList getElementsByTagNameNS(
        String namespaceURI,
        String localName) {
        return super.getElementsByTagNameNS(namespaceURI, localName);
    }

    public Element getElementById(String elementId) {
        return super.getElementById(elementId);
    }

    public Node cloneNode(boolean deep) {
        SOAPPartImpl newSoapPart = getSOAPPart().doCloneNode();
        super.cloneNode(newSoapPart.getDocument(), deep);
        return newSoapPart;
    }

    public void cloneNode(SOAPDocumentImpl newdoc, boolean deep) {
        super.cloneNode(newdoc, deep);
    }
}
