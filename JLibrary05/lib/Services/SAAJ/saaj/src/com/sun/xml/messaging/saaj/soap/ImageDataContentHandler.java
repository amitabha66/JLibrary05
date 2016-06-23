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
 * $Id: ImageDataContentHandler.java,v 1.1.1.1 2006/01/27 13:10:55 kumarjayanti Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2006/01/27 13:10:55 $
 */

/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.messaging.saaj.soap;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.sun.xml.messaging.saaj.util.LogDomainConstants;

public class ImageDataContentHandler extends Component
    implements DataContentHandler {

    protected static Logger log =
        Logger.getLogger(LogDomainConstants.SOAP_DOMAIN,
                         "com.sun.xml.messaging.saaj.soap.LocalStrings");
    
    private DataFlavor[] flavor;

    public ImageDataContentHandler() {
        String[] mimeTypes = ImageIO.getReaderMIMETypes();
        flavor = new DataFlavor[mimeTypes.length];
        for(int i=0; i < mimeTypes.length; i++) {
            flavor[i] = new ActivationDataFlavor(
                java.awt.Image.class, mimeTypes[i], "Image");
        }
    }

    /**
     * Returns an array of DataFlavor objects indicating the flavors the
     * data can be provided in. The array should be ordered according to
     * preference for providing the data (from most richly descriptive to
     * least descriptive).
     *
     * @return The DataFlavors.
     */
    public DataFlavor[] getTransferDataFlavors() { 
        return flavor;
    }

    /**
     * Returns an object which represents the data to be transferred.
     * The class of the object returned is defined by the representation class
     * of the flavor.
     *
     * @param df The DataFlavor representing the requested type.
     * @param ds The DataSource representing the data to be converted.
     * @return The constructed Object.
     */
    public Object getTransferData(DataFlavor df, DataSource ds)
        throws IOException {
        for (int i=0; i < flavor.length; i++) {
            if (flavor[i].equals(df)) {
                return getContent(ds);
            }
        }
        return null;
    }

    /**
     * Return an object representing the data in its most preferred form.
     * Generally this will be the form described by the first DataFlavor
     * returned by the <code>getTransferDataFlavors</code> method.
     *
     * @param ds The DataSource representing the data to be converted.
     * @return The constructed Object.
     */
    public Object getContent(DataSource ds) throws IOException {
        return ImageIO.read(new BufferedInputStream(ds.getInputStream()));
    }

    /**
     * Convert the object to a byte stream of the specified MIME type
     * and write it to the output stream.
     *
     * @param obj   The object to be converted.
     * @param mimeType  The requested MIME type of the resulting byte stream.
     * @param os    The output stream into which to write the converted
     *          byte stream.
     */

    public void writeTo(Object obj, String type, OutputStream os)
        throws IOException {

        try {
            BufferedImage bufImage = null;
            if (obj instanceof BufferedImage) {
                bufImage = (BufferedImage)obj;
            } else if (obj instanceof Image) {
                bufImage = render((Image)obj);
            } else {
                log.log(Level.SEVERE,
                    "SAAJ0520.soap.invalid.obj.type", 
                    new String[] { obj.getClass().toString() });
                throw new IOException(
                    "ImageDataContentHandler requires Image object, "
                    + "was given object of type "
                    + obj.getClass().toString());
            }
            ImageWriter writer = null;
            Iterator i = ImageIO.getImageWritersByMIMEType(type);
            if (i.hasNext()) {
                writer = (ImageWriter)i.next();
            }
            if (writer != null) {
                ImageOutputStream stream = null;
                stream = ImageIO.createImageOutputStream(os);
                writer.setOutput(stream);
                writer.write(bufImage);
                stream.close();
            } else {
                log.log(Level.SEVERE, "SAAJ0526.soap.unsupported.mime.type",
                    new String[] { type });
                throw new IOException("Unsupported mime type:"+ type);
            }
        } catch (Exception e) {
            log.severe("SAAJ0525.soap.cannot.encode.img");
            throw new IOException("Unable to encode the image to a stream "
                + e.getMessage());
        }
    }


    private BufferedImage render(Image img) throws InterruptedException {

        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(img, 0);
        tracker.waitForAll();
        BufferedImage bufImage = new BufferedImage(img.getWidth(null),
            img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics g = bufImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return bufImage;
    }

}
