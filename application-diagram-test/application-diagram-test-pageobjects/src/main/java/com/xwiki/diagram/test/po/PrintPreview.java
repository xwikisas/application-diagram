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

/**
 * The print preview of a diagram, opened from File > Print / Export as PDF.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class PrintPreview
{
    private final String text;

    private final int pageCount;

    /**
     * @param text the text displayed by the preview
     * @param pageCount the number of printed pages (rendered as SVG)
     */
    public PrintPreview(String text, int pageCount)
    {
        this.text = text;
        this.pageCount = pageCount;
    }

    /**
     * @return the text displayed by the preview
     */
    public String getText()
    {
        return this.text;
    }

    /**
     * @return the number of printed pages
     */
    public int getPageCount()
    {
        return this.pageCount;
    }
}
