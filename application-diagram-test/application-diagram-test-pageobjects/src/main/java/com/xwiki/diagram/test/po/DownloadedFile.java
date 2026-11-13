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

import java.nio.charset.StandardCharsets;

/**
 * A file downloaded from the diagram editor (e.g. an exported image).
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DownloadedFile
{
    private final String name;

    private final byte[] content;

    /**
     * @param name the file name
     * @param content the file content
     */
    public DownloadedFile(String name, byte[] content)
    {
        this.name = name;
        this.content = content;
    }

    /**
     * @return the file name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the file content
     */
    public byte[] getContent()
    {
        return this.content;
    }

    /**
     * @return the file content, as UTF-8 text
     */
    public String getText()
    {
        return new String(this.content, StandardCharsets.UTF_8);
    }
}
