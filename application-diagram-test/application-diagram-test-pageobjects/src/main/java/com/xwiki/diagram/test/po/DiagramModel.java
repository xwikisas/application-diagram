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
package com.xwiki.diagram.test.po;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The content of a diagram page (the {@code mxGraphModel} of the first diagram page), parsed from the diagram XML.
 *
 * @version $Id$
 * @since 2.1.4
 */
public final class DiagramModel
{
    private static final String MX_CELL = "mxCell";

    private static final String MX_GEOMETRY = "mxGeometry";

    private final Element graphModel;

    private final List<DiagramCell> cells;

    private DiagramModel(Element graphModel, List<DiagramCell> cells)
    {
        this.graphModel = graphModel;
        this.cells = Collections.unmodifiableList(cells);
    }

    /**
     * Parses the given diagram XML. Both the {@code mxfile} format (used to store the diagram) and the
     * {@code mxGraphModel} format (used by the editor) are supported, as long as the content is not compressed.
     *
     * @param xml the diagram XML
     * @return the parsed diagram model
     */
    public static DiagramModel parse(String xml)
    {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList graphModels = document.getElementsByTagName("mxGraphModel");
            if (graphModels.getLength() == 0) {
                throw new IllegalArgumentException("The diagram XML doesn't contain any (uncompressed) graph model.");
            }
            Element graphModel = (Element) graphModels.item(0);
            List<DiagramCell> cells = new ArrayList<>();
            NodeList cellElements = graphModel.getElementsByTagName(MX_CELL);
            for (int i = 0; i < cellElements.getLength(); i++) {
                cells.add(toCell((Element) cellElements.item(i)));
            }
            return new DiagramModel(graphModel, cells);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse the diagram XML.", e);
        }
    }

    /**
     * @param xml the diagram XML, in the {@code mxfile} format
     * @return the names of the diagram pages
     */
    public static List<String> parsePageNames(String xml)
    {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList pages = document.getElementsByTagName("diagram");
            List<String> names = new ArrayList<>();
            for (int i = 0; i < pages.getLength(); i++) {
                names.add(((Element) pages.item(i)).getAttribute("name"));
            }
            return names;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse the diagram XML.", e);
        }
    }

    private static DiagramCell toCell(Element cell)
    {
        // Cells with custom properties (e.g. a link) are wrapped in an UserObject / object element that holds the cell
        // identifier and label.
        Node parent = cell.getParentNode();
        Element holder = cell;
        if (parent instanceof Element && !MX_CELL.equals(((Element) parent).getTagName())
            && !"root".equals(((Element) parent).getTagName()))
        {
            holder = (Element) parent;
        }
        String value = holder == cell ? cell.getAttribute("value") : holder.getAttribute("label");
        String link = holder.hasAttribute("link") ? holder.getAttribute("link") : null;
        return new DiagramCell(holder.getAttribute("id"), value, link, cell.getAttribute("style"),
            "1".equals(cell.getAttribute("vertex")), "1".equals(cell.getAttribute("edge")), getGeometry(cell));
    }

    private static double[] getGeometry(Element cell)
    {
        double[] geometry = new double[4];
        NodeList children = cell.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && MX_GEOMETRY.equals(((Element) child).getTagName())) {
                Element element = (Element) child;
                geometry[0] = getNumber(element, "x");
                geometry[1] = getNumber(element, "y");
                geometry[2] = getNumber(element, "width");
                geometry[3] = getNumber(element, "height");
            }
        }
        return geometry;
    }

    private static double getNumber(Element element, String attribute)
    {
        String value = element.getAttribute(attribute);
        return value.isEmpty() ? 0 : Double.parseDouble(value);
    }

    /**
     * @param name the attribute name (e.g. {@code pageWidth}, {@code gridSize}, {@code background})
     * @return the value of the given graph model attribute, or an empty string if it's not set
     */
    public String getAttribute(String name)
    {
        return this.graphModel.getAttribute(name);
    }

    /**
     * @return all the cells of the diagram, in z-order (the last cell is displayed on top)
     */
    public List<DiagramCell> getCells()
    {
        return this.cells;
    }

    /**
     * @return the shapes of the diagram, in z-order (the last shape is displayed on top)
     */
    public List<DiagramCell> getVertices()
    {
        return this.cells.stream().filter(DiagramCell::isVertex).collect(Collectors.toList());
    }

    /**
     * @param id the cell identifier
     * @return the cell with the given identifier
     */
    public Optional<DiagramCell> getCell(String id)
    {
        return this.cells.stream().filter(cell -> cell.getId().equals(id)).findFirst();
    }

    /**
     * @param id the cell identifier
     * @return the position of the given cell in the z-order of the shapes (0 is the back-most shape), or -1 if the
     *     cell is not a shape of this diagram
     */
    public int getZIndex(String id)
    {
        List<DiagramCell> vertices = getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
