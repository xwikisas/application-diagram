/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.diagram.internal.handlers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handler for diagram content operations.
 * 
 * @version $Id$
 * @since 1.13
 */
public class DiagramContentHandler
{
    /**
     * View action.
     */
    public static final String VIEW_ACTION = "view";

    /**
     * Pages referenced inside diagram content.
     */
    private List<String> includedPages = new ArrayList<String>();

    /**
     * Helper methods for custom links contained by a diagram.
     */
    private DiagramLinkHandler linkHandler;

    /**
     * Constructor.
     */
    public DiagramContentHandler()
    {
        this.linkHandler = new DiagramLinkHandler();
    }

    /**
     * Update the attachments content and save the new version.
     * 
     * @param backlinkDoc document that has a backlink to diagram
     * @param context the execution context
     * @param oldDocRef reference of the page before rename
     * @param newDocRef reference of the page after rename
     * @throws XWikiException when an error occurs while accessing documents.
     * @throws IOException if something goes wrong while setting attachment's content.
     */
    public void updateAttachment(XWikiDocument backlinkDoc, XWikiContext context, DocumentReference oldDocRef,
        DocumentReference newDocRef) throws XWikiException, IOException
    {
        XWikiAttachment attachment = backlinkDoc.getAttachment("diagram.svg");
        if (attachment != null) {
            backlinkDoc.addAttachment(getUpdatedAttachment(attachment, context, oldDocRef, newDocRef));
            context.getWiki().saveDocument(backlinkDoc, "Updated attachment after page rename", context);
        }
    }

    /**
     * Migrate links inside diagram's attachment to the new name.
     * 
     * @param attachment attachment to be processed
     * @param context the execution context
     * @param oldDocRef reference of the page before rename
     * @param newDocRef reference of the page after rename
     * @return the modified attachment
     * @throws XWikiException when an error occurs while accessing documents.
     * @throws IOException if something goes wrong while setting attachment's content.
     */
    public XWikiAttachment getUpdatedAttachment(XWikiAttachment attachment, XWikiContext context,
        DocumentReference oldDocRef, DocumentReference newDocRef) throws XWikiException, IOException
    {
        XWikiDocument oldDoc = context.getWiki().getDocument(oldDocRef, context);
        XWikiDocument newDoc = context.getWiki().getDocument(newDocRef, context);

        BufferedReader reader = new BufferedReader(new InputStreamReader(attachment.getContentInputStream(context)));
        StringBuffer sb = new StringBuffer();

        String str;
        while ((str = reader.readLine()) != null) {
            sb.append(str);
        }

        String regex = "\"%s\"";
        String oldAbsoluteURL = String.format(regex, oldDoc.getExternalURL(VIEW_ACTION, context));
        String newAbsoluteURL = String.format(regex, newDoc.getExternalURL(VIEW_ACTION, context));
        String oldURL = String.format(regex, oldDoc.getURL(VIEW_ACTION, context));
        String newURL = String.format(regex, newDoc.getURL(VIEW_ACTION, context));

        String newContent = sb.toString().replaceAll(oldAbsoluteURL, newAbsoluteURL).replaceAll(oldURL, newURL);

        attachment.setContent(new ByteArrayInputStream(newContent.getBytes()));

        return attachment;
    }

    /**
     * Update content of the diagram with new links.
     * 
     * @param backlinkDoc document that has a backlink to the diagram
     * @param originalDocRef reference of the document before rename
     * @param currentDocRef reference of the document after rename
     * @param context context of the execution
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created
     * @throws SAXException if parsing the document fails
     * @throws IOException if any IO errors occur
     * @throws XWikiException if saving the document fails
     */
    public void updateDiagramContent(XWikiDocument backlinkDoc, DocumentReference originalDocRef,
        DocumentReference currentDocRef, XWikiContext context)
        throws ParserConfigurationException, SAXException, IOException, XWikiException
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new ByteArrayInputStream(backlinkDoc.getContent().getBytes()));

        NodeList userObjectList = document.getElementsByTagName("UserObject");
        for (int i = 0; i < userObjectList.getLength(); i++) {
            linkHandler.updateUserObjectNode(userObjectList.item(i), currentDocRef, originalDocRef);
        }

        NodeList mxCellList = document.getElementsByTagName("mxCell");
        for (int i = 0; i < mxCellList.getLength(); i++) {
            linkHandler.updateMxCellNode(mxCellList.item(i), currentDocRef, originalDocRef);
        }

        backlinkDoc.setContent(getStringFromDocument(document));
        context.getWiki().saveDocument(backlinkDoc, "Updated diagram after page rename", context);
    }

    /**
     * Convert a document to String.
     * 
     * @param doc the Document to be processed
     * @return converted Document to String
     */
    public String getStringFromDocument(Document doc)
    {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(sw));

            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    /**
     * Search referenced pages inside the content of a diagram.
     * 
     * @param content the content of the diagram
     * @return includedPages list of pages inside this content
     */
    public List<String> getIncludedPages(String content)
    {
        try {
            GetDiagramLinksHandler getDiagramLinksHandler = new GetDiagramLinksHandler();
            SAXParserFactory.newInstance().newSAXParser().parse(new ByteArrayInputStream(content.getBytes()),
                getDiagramLinksHandler);

            return getDiagramLinksHandler.getIncludedPages();
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return includedPages;
    }
}
