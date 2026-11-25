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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link SafeXMLParsers}.
 *
 * @version $Id$
 */
class SafeXMLParsersTest
{
    private static final String EXTERNAL_ENTITY = "<?xml version=\"1.0\"?><!DOCTYPE m ["
        + "<!ENTITY % d SYSTEM \"http://127.0.0.1:1/dtd\"> %d;]><mxGraphModel><root/></mxGraphModel>";

    private static final String NESTED_ENTITIES = "<?xml version=\"1.0\"?><!DOCTYPE g [<!ENTITY a \"aaaaaaaaaa\">"
        + "<!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">]><g>&b;</g>";

    private static final String DIAGRAM = "<mxGraphModel><root><mxCell id=\"0\"/></root></mxGraphModel>";

    private static ByteArrayInputStream stream(String xml)
    {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void documentBuilderParsesRegularDiagram() throws Exception
    {
        Document document = SafeXMLParsers.newDocumentBuilder().parse(stream(DIAGRAM));
        assertEquals("mxGraphModel", document.getDocumentElement().getNodeName());
    }

    @Test
    void documentBuilderRejectsDoctype()
    {
        assertThrows(SAXParseException.class,
            () -> SafeXMLParsers.newDocumentBuilder().parse(stream(EXTERNAL_ENTITY)));
        assertThrows(SAXParseException.class,
            () -> SafeXMLParsers.newDocumentBuilder().parse(stream(NESTED_ENTITIES)));
    }

    @Test
    void saxParserParsesRegularDiagram() throws Exception
    {
        SafeXMLParsers.newSAXParser().parse(stream(DIAGRAM), new DefaultHandler());
    }

    @Test
    void saxParserRejectsDoctype()
    {
        assertThrows(SAXParseException.class,
            () -> SafeXMLParsers.newSAXParser().parse(stream(EXTERNAL_ENTITY), new DefaultHandler()));
        assertThrows(SAXParseException.class,
            () -> SafeXMLParsers.newSAXParser().parse(stream(NESTED_ENTITIES), new DefaultHandler()));
    }
}
