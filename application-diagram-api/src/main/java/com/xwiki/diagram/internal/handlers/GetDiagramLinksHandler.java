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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses the XML of a diagram and gets the links to wiki pages that are included.
 * 
 * @version $Id$
 * @since 1.13
 */
public class GetDiagramLinksHandler extends DefaultHandler
{
    private DiagramLinkHandler linkHandler;

    /**
     * Lists of pages referenced inside nodes.
     */
    private List<String> includedPages = new ArrayList<String>();

    @Override
    public void startElement(String uri, String key, String qName, Attributes attributes) throws SAXException
    {
        String link = null;
        linkHandler = new DiagramLinkHandler();

        if (qName.equalsIgnoreCase(DiagramLinkHandler.USEROBJECT)) {
            link = linkHandler.getUserObjectNodeLink(attributes.getValue("link"));
        } else if (qName.equalsIgnoreCase(DiagramLinkHandler.MXCELL)) {
            link = linkHandler.getMxCellNodeLink(attributes.getValue("value"));
        }

        if (link != null) {
            includedPages.add(link);
        }
    }

    /**
     * Get list of unique included pages.
     * 
     * @return list of referenced pages.
     */
    public List<String> getIncludedPages()
    {
        includedPages = includedPages.stream().distinct().collect(Collectors.toList());
        return this.includedPages;
    }
}
