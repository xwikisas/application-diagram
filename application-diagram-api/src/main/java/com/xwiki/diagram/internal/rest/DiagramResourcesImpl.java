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
package com.xwiki.diagram.internal.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DiagramResources}.
 *
 * @version $Id$
 */
@Component
@Named("com.xwiki.diagram.internal.rest.DiagramResourcesImpl")
@Singleton
public class DiagramResourcesImpl extends XWikiResource implements DiagramResources
{
    private static final String PREFIX = "diagram";

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Override
    public Response deleteAttachments(String documentReference) throws XWikiRestException, XWikiException
    {

        XWikiDocument diagramDocument =
            Utils.getXWiki(componentManager).getDocument(resolver.resolve(documentReference),
                Utils.getXWikiContext(componentManager));

        if (!this.authorization.hasAccess(Right.EDIT, diagramDocument.getDocumentReference())) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        removeAttachments(diagramDocument, ".png");
        removeAttachments(diagramDocument, ".svg");
        Utils.getXWiki(componentManager)
            .saveDocument(diagramDocument, "Deleted diagram attachments", Utils.getXWikiContext(componentManager));
        return Response.status(Response.Status.OK).build();
    }

    private void removeAttachments(XWikiDocument document, String extension)
    {
        int count = 0;
        while (true) {
            XWikiAttachment attachment = document.getAttachment(getFileName(count, extension));
            if (attachment == null) {
                break;
            }
            document.removeAttachment(attachment);
            count++;
        }
    }

    private String getFileName(int index, String suffix)
    {
        if (index == 0) {
            return String.format("%s%s", PREFIX, suffix);
        }
        return String.format("%s%d%s", PREFIX, index + 1, suffix);
    }
}
