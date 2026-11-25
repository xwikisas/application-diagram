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
package com.xwiki.diagram.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLElementSanitizer;

/**
 * Cleans the SVG of a diagram so that it can be safely included in the HTML of a wiki page. The SVG is
 * stored as an attachment on the page and the user can edit that attachment, so it can't be trusted: elements
 * and attributes that are not allowed by the HTML sanitizer of the platform (e.g. scripts, event handlers or links
 * with a dangerous URL) are removed, and the result is encoded so that it can't be mistaken for wiki syntax.
 *
 * @version $Id$
 * @since 2.1.4
 */
@Component(roles = DiagramSVGSanitizer.class)
@Singleton
public class DiagramSVGSanitizer
{
    /**
     * The element used by draw.io to display formatted labels. It isn't allowed by the platform sanitizer, but we need
     * it for the labels to be displayed. Its children are checked like any other element.
     */
    private static final String FOREIGN_OBJECT = "foreignobject";

    /**
     * There might be other elements, but we will have to find them later since I can't do an exhaustive search.
     */
    private static final Set<String> FOREIGN_OBJECT_ATTRIBUTES =
        Set.of("x", "y", "width", "height", "requiredfeatures", "requiredextensions", "pointer-events", "style");

    private static final String XMLNS = "xmlns";

    @Inject
    private HTMLElementSanitizer htmlElementSanitizer;

    @Inject
    private Logger logger;

    /**
     * @param svg the SVG image to clean
     * @return the cleaned SVG image, without the XML declaration and the document type declaration, or an empty string
     *         if the image is not valid
     */
    public String sanitize(String svg)
    {
        if (svg == null || svg.isBlank()) {
            return "";
        }
        // The XML declaration and the document type declaration are not allowed in HTML.
        String content = svg.replaceFirst("<\\?[^>]+>", "").replaceFirst("<!DOCTYPE[^>]+>", "").trim();
        try {
            Document document = SafeXMLParsers.newDocumentBuilder().parse(new InputSource(new StringReader(content)));
            Element root = document.getDocumentElement();
            if (!"svg".equalsIgnoreCase(root.getTagName())) {
                return "";
            }
            sanitizeChildren(root);
            sanitizeAttributes(root);
            // Prevent the SVG from being parsed as wiki syntax (e.g. to close the HTML macro that wraps it).
            return XMLUtils.serialize(root, false).replace("{", "&#123;");
        } catch (Exception e) {
            this.logger.warn("Failed to clean the diagram SVG image. Root cause: [{}]", e.getMessage());
            return "";
        }
    }

    private void sanitizeChildren(Node parent)
    {
        List<Node> children = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            children.add(child);
        }
        for (Node child : children) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                if (isElementAllowed(element)) {
                    sanitizeAttributes(element);
                    sanitizeChildren(element);
                } else {
                    parent.removeChild(element);
                }
            } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                // Use a text node so that the content is escaped when serialized.
                parent.replaceChild(parent.getOwnerDocument().createTextNode(child.getNodeValue()), child);
            } else if (child.getNodeType() != Node.TEXT_NODE) {
                // Comments, processing instructions, etc.
                parent.removeChild(child);
            }
        }
    }

    private boolean isElementAllowed(Element element)
    {
        String name = element.getTagName().toLowerCase(Locale.ROOT);
        return FOREIGN_OBJECT.equals(name) || this.htmlElementSanitizer.isElementAllowed(name);
    }

    private void sanitizeAttributes(Element element)
    {
        String elementName = element.getTagName().toLowerCase(Locale.ROOT);
        NamedNodeMap attributes = element.getAttributes();
        List<Attr> forbidden = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (!isAttributeAllowed(elementName, attribute)) {
                forbidden.add(attribute);
            }
        }
        forbidden.forEach(element::removeAttributeNode);
    }

    private boolean isAttributeAllowed(String elementName, Attr attribute)
    {
        String name = attribute.getName().toLowerCase(Locale.ROOT);
        if (XMLNS.equals(name) || name.startsWith(XMLNS + ':')) {
            // Namespace declarations are needed for the SVG to be displayed.
            return true;
        } else if (FOREIGN_OBJECT.equals(elementName)) {
            return FOREIGN_OBJECT_ATTRIBUTES.contains(name);
        } else {
            return this.htmlElementSanitizer.isAttributeAllowed(elementName, name, attribute.getValue());
        }
    }
}
