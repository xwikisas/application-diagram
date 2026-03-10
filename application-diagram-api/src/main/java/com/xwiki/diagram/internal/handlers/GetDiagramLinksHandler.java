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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

/**
 * Parses the XML of a diagram and gets the links to wiki pages that are linked.
 * 
 * @version $Id$
 * @since 1.13
 */
@Component(roles = GetDiagramLinksHandler.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class GetDiagramLinksHandler extends DefaultHandler
{
    private static final String LINK = "link";

    private static final String LABEL = "label";

    @Inject
    private DiagramLinkHandler linkHandler;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> resolver;

    /**
     * Lists of pages referenced inside nodes.
     */
    private List<String> linkedPages = new ArrayList<String>();

    @Override
    public void startElement(String uri, String key, String qName, Attributes attributes) throws SAXException
    {
        if (qName.equalsIgnoreCase(DiagramLinkHandler.USEROBJECT)) {
            // User Object nodes can store the link in 2 places in the attribute "link" and in the label as
            // a <a href = "customLink">actualLabel</a>
            if (attributes.getValue(LINK) != null) {
                linkedPages.add(linkHandler.getUserObjectNodeLink(attributes.getValue(LINK)));
            }
            // I didn't manage to create the situation where there are links in both the LINK and LABEL attributes,
            // but to keep it safe we check both cases instead of having an else if.
            if (attributes.getValue(LABEL) != null && attributes.getValue(LABEL).contains("href")) {
                linkedPages.addAll(linkHandler.getMxCellNodeLinks(attributes.getValue(LABEL)));
            }
        } else if (qName.equalsIgnoreCase(DiagramLinkHandler.MXCELL)) {
            linkedPages.addAll(linkHandler.getMxCellNodeLinks(attributes.getValue("value")));
        }
    }

    /**
     * Get list of unique linked pages.
     * 
     * @param diagramReference the reference of current diagram
     * @return list of referenced pages.
     */
    public List<EntityReference> getLinkedPages(DocumentReference diagramReference)
    {
        return linkedPages.stream().distinct().map(link -> resolver.resolve(link, diagramReference))
            .collect(Collectors.toList());
    }
}
