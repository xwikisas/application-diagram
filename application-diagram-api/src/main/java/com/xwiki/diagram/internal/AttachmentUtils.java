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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Generic utils for working with XWikiAttachments.
 *
 * @version $Id$
 * @since 1.22.12
 */
public final class AttachmentUtils
{
    private AttachmentUtils()
    {
    }

    // Copied from
    // https://github.com/xwiki/xwiki-platform/blob/master/xwiki-platform-core/xwiki-platform-oldcore/src/main/java/
    // com/xpn/xwiki/api/Attachment.java

    /**
     * @param attachment attachment for which we want the content
     * @param context current context
     * @return the content of an attachment as a string.
     */
    public static String getContentAsString(XWikiAttachment attachment, XWikiContext context)
    {
        try {
            // the input stream can be null if the attachment has been deleted for example.
            InputStream contentInputStream = attachment.getContentInputStream(context);
            if (contentInputStream != null) {
                return new String(IOUtils.toByteArray(contentInputStream));
            }
        } catch (IOException | XWikiException ex) {

        }
        return "";
    }
}
