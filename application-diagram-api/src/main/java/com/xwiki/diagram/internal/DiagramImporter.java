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
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxGraphMlCodec;
import com.mxgraph.io.gliffy.importer.GliffyDiagramConverter;
import com.mxgraph.online.OpenServlet;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphHeadless;

/**
 * Utility class used to convert a diagram from a third-party format to the draw.io format, so that it can be edited by
 * the Diagram application.
 * 
 * @version $Id$
 * @since 1.14
 */
@Component(roles = DiagramImporter.class)
@Singleton
public class DiagramImporter
{
    /**
     * Used to detect Gliffy diagrams.
     */
    private static String gliffyRegex = "(?s).*\"contentType\":\\s*\"application/gliffy\\+json\".*";

    /**
     * Used to detect GraphML diagrams.
     */
    private static String graphMlRegex = "(?s).*<graphml xmlns=\".*";

    /**
     * Attempts to convert the given diagram from a third party format to the draw.io format. See draw.io's
     * {@code OpenServlet} for details.
     * 
     * @param diagram the diagram content, using a third-party format
     * @param fileName the diagram file name, used to detect the diagram type
     * @return the diagram XML in draw.io format, or {@code null} if the diagram format is not recognized or unsupported
     * @since 1.14
     */
    public String importDiagram(String diagram, String fileName)
    {
        String xml = null;

        if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
            try {
                xml = extractXMLFromPNG(diagram.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // Shouldn't happen.
            }
        } else if (diagram.matches(graphMlRegex)) {
            // Creates a graph that contains a model but does not validate since that is not needed for the model and
            // not allowed on GAE.
            mxGraph graph = new mxGraphHeadless();
            mxGraphMlCodec.decode(mxXmlUtils.parseXml(diagram), graph);
            xml = mxXmlUtils.getXml(new mxCodec().encode(graph.getModel()));
        } else if (diagram.matches(gliffyRegex)) {
            GliffyDiagramConverter converter = new GliffyDiagramConverter(diagram);
            xml = converter.getGraphXml();
        }

        return xml;
    }

    // NOTE: Key length must not be longer than 79 bytes (not checked)
    private String extractXMLFromPNG(byte[] data)
    {
        Map<String, String> textChunks = OpenServlet.decodeCompressedText(new ByteArrayInputStream(data));
        return (textChunks != null) ? textChunks.get("mxGraphModel") : null;
    }
}
