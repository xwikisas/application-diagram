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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A cell (shape or connector) of a diagram, as stored in the diagram XML.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramCell
{
    private final String id;

    private final String value;

    private final String link;

    private final Map<String, String> style;

    private final boolean vertex;

    private final boolean edge;

    private final double x;

    private final double y;

    private final double width;

    private final double height;

    /**
     * Creates a new cell.
     *
     * @param id the cell identifier
     * @param value the cell label
     * @param link the link set on the cell, if any
     * @param style the raw cell style (e.g. {@code rounded=0;whiteSpace=wrap;html=1;})
     * @param vertex whether the cell is a shape
     * @param edge whether the cell is a connector
     * @param geometry the geometry of the cell: x, y, width and height
     */
    public DiagramCell(String id, String value, String link, String style, boolean vertex, boolean edge,
        double[] geometry)
    {
        this.id = id;
        this.value = value == null ? "" : value;
        this.link = link;
        this.style = parseStyle(style);
        this.vertex = vertex;
        this.edge = edge;
        this.x = geometry[0];
        this.y = geometry[1];
        this.width = geometry[2];
        this.height = geometry[3];
    }

    private static Map<String, String> parseStyle(String style)
    {
        Map<String, String> result = new LinkedHashMap<>();
        if (style != null) {
            for (String entry : style.split(";")) {
                if (!entry.isEmpty()) {
                    int separator = entry.indexOf('=');
                    if (separator < 0) {
                        // The style name (e.g. "text" or "ellipse") is stored without value.
                        result.put(entry, "");
                    } else {
                        result.put(entry.substring(0, separator), entry.substring(separator + 1));
                    }
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * @return the cell identifier
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the cell label (may contain HTML)
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * @return the link set on the cell, or {@code null} if there's none
     */
    public String getLink()
    {
        return this.link;
    }

    /**
     * @return the style of the cell, as a map of style properties
     */
    public Map<String, String> getStyle()
    {
        return this.style;
    }

    /**
     * @param name the style property name
     * @return the value of the given style property, or {@code null} if it's not set
     */
    public String getStyle(String name)
    {
        return this.style.get(name);
    }

    /**
     * @return whether this cell is a shape
     */
    public boolean isVertex()
    {
        return this.vertex;
    }

    /**
     * @return whether this cell is a connector
     */
    public boolean isEdge()
    {
        return this.edge;
    }

    /**
     * @return whether the cell is locked
     */
    public boolean isLocked()
    {
        return "1".equals(getStyle("locked"));
    }

    /**
     * @return the rotation of the cell, in degrees
     */
    public double getRotation()
    {
        String rotation = getStyle("rotation");
        return rotation == null ? 0 : Double.parseDouble(rotation);
    }

    /**
     * @return the horizontal position of the cell
     */
    public double getX()
    {
        return this.x;
    }

    /**
     * @return the vertical position of the cell
     */
    public double getY()
    {
        return this.y;
    }

    /**
     * @return the width of the cell
     */
    public double getWidth()
    {
        return this.width;
    }

    /**
     * @return the height of the cell
     */
    public double getHeight()
    {
        return this.height;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]%s", this.id, this.value, this.style);
    }
}
