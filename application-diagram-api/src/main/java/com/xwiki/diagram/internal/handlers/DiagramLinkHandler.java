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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.xml.html.HTMLCleaner;

/**
 * Handles links to wiki pages, contained by diagrams.
 * 
 * @version $Id$
 * @since 1.13
 */
@Component(roles = DiagramLinkHandler.class)
@Singleton
public class DiagramLinkHandler
{
    /**
     * UserObject node tag.
     */
    public static final String USEROBJECT = "UserObject";

    /**
     * mxCell node tag.
     */
    public static final String MXCELL = "mxCell";

    /**
     * Href attribute.
     */
    private static final String HREF = "href";

    /**
     * The prefix added to the links inserted in a diagram.
     */
    private static final String CUSTOM_LINK_PREFIX = "data:xwiki/reference,";

    @Inject
    private HTMLCleaner defaultHTMLCleaner;

    @Inject
    private Logger logger;

    /**
     * Get custom link for a resource reference.
     * 
     * @param resourceReference the resource reference
     * @return the custom link
     */
    private String getCustomLinkFromResourceReference(String resourceReference)
    {
        return CUSTOM_LINK_PREFIX + "doc:" + resourceReference;
    }

    /**
     * Remove the custom part of inserted links.
     * 
     * @param href the value of a link attribute
     * @return the reference to the page
     */
    private String getResourceReferenceFromCustomLink(String href)
    {
        String resourceReference = href.substring(CUSTOM_LINK_PREFIX.length());
        Integer typeSeparatorIndex = resourceReference.indexOf(":");
        return resourceReference.substring(typeSeparatorIndex + 1);
    }

    /**
     * Check if the link is diagram custom link.
     * 
     * @param href the value of a link attribute
     * @return true if the link is custom.
     */
    private Boolean isXWikiCustomLink(String href)
    {
        return href.substring(0, CUSTOM_LINK_PREFIX.length()).contentEquals(CUSTOM_LINK_PREFIX);
    }

    /**
     * Modify content of UserObject node of the diagram, to contain the new name of the link.
     * 
     * @param node UserObject node
     * @param newDocumentRef document's reference after rename
     * @param oldDocumentRef document's reference before rename
     */
    public void updateUserObjectNode(Node node, DocumentReference newDocumentRef, DocumentReference oldDocumentRef)
    {
        if (node.hasChildNodes()) {
            Node linkNode = node.getAttributes().getNamedItem("link");
            String oldSource = linkNode.getNodeValue();
            if (isXWikiCustomLink(oldSource)
                && oldDocumentRef.toString().equals(getResourceReferenceFromCustomLink(oldSource))) {
                String newSource = getCustomLinkFromResourceReference(newDocumentRef.toString());
                linkNode.setTextContent(newSource);
            }
        }
    }

    /**
     * Modify content of mxCell node of the diagram, to contain the new name of the link.
     * 
     * @param node mxCell node
     * @param newDocumentRef document's reference after rename
     * @param oldDocumentRef document's reference before rename
     * @throws ParserConfigurationException if document builder cannot be created
     * @throws SAXException if parsing the document fails
     * @throws IOException if parsing the document fails
     */
    public void updateMxCellNode(Node node, DocumentReference newDocumentRef, DocumentReference oldDocumentRef)
        throws ParserConfigurationException, SAXException, IOException
    {
        if (node.hasChildNodes()) {
            Node linkNode = node.getAttributes().getNamedItem("value");
            if (linkNode == null) {
                return;
            }

            String value = linkNode.getNodeValue();
            if (value.indexOf(HREF) == -1) {
                return;
            }

            // The value attribute contains the text element, which could contain one or more links.
            for (String oldSource : getLinksFromEmbeddedNode(value)) {
                if (oldSource != null && isXWikiCustomLink(oldSource)
                    && oldDocumentRef.toString().equals(getResourceReferenceFromCustomLink(oldSource))) {
                    String newSource = getCustomLinkFromResourceReference(newDocumentRef.toString());
                    linkNode.setNodeValue(value.replace(oldSource, newSource));
                }
            }

        }
    }

    /**
     * Get link from inside UserObject node of the diagram.
     * 
     * @param link value from the attribute
     * @return resource reference
     */
    public String getUserObjectNodeLink(String link)
    {
        if (link == null || !isXWikiCustomLink(link)) {
            return null;
        }
        return getResourceReferenceFromCustomLink(link);
    }

    /**
     * Get links from inside mxCell node of the diagram.
     * 
     * @param value text element content in string format
     * @return resource references inside text element
     */
    public List<String> getMxCellNodeLinks(String value)
    {
        List<String> links = new ArrayList<String>();
        if (value == null || value.indexOf(DiagramLinkHandler.HREF) == -1) {
            return links;
        }
        try {
            // The value attribute contains the text element, which could contain one or more links.
            links = getLinksFromEmbeddedNode(value);
            links = links.stream().filter(Objects::nonNull).filter(this::isXWikiCustomLink)
                .map(link -> getResourceReferenceFromCustomLink(link)).collect(Collectors.toList());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            logger.warn("Failed while parsing a mxCell node", e);
        }

        return links;
    }

    /**
     * Get links from the content of a text element.
     * 
     * @param value value of the text element.
     * @return links inside text element
     * @throws ParserConfigurationException if document builder cannot be created
     * @throws IOException if parsing the document fails
     * @throws SAXException if parsing the document fails
     */
    private List<String> getLinksFromEmbeddedNode(String value)
        throws SAXException, IOException, ParserConfigurationException
    {
        List<String> hrefValues = new ArrayList<String>();
        // Create a DOM using value for getting the href of 'a' elements inside it.
        Document doc = defaultHTMLCleaner.clean(new StringReader(value));
        NodeList nodes = doc.getElementsByTagName("a");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            hrefValues.add(node.getAttribute(HREF));
        }
        return hrefValues;
    }
}
