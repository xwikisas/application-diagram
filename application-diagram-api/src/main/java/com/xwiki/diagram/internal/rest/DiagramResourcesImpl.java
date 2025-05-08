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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.diagram.DiagramResources;

/**
 * Default implementation of {@link DiagramResources}.
 *
 * @version $Id$
 * @since 1.22.1
 */
@Component
@Named("com.xwiki.diagram.internal.rest.DiagramResourcesImpl")
@Singleton
@Unstable
public class DiagramResourcesImpl extends XWikiResource implements DiagramResources
{
    private static final String PREFIX = "diagram";

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Response deleteAttachments(String documentReference) throws Exception
    {

        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        String decodedReference = URLDecoder.decode(documentReference, StandardCharsets.UTF_8);
        XWikiDocument diagramDocument = xwiki.getDocument(resolver.resolve(decodedReference), context);

        // If the document doesn't exist yet just return 200.
        if (!documentAccessBridge.exists(diagramDocument.getDocumentReference())) {
            // We can reach this point only when the diagram is firstly created.
            return Response.status(Response.Status.OK).build();
        }

        if (!this.authorization.hasAccess(Right.EDIT, diagramDocument.getDocumentReference())) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        removeAttachments(diagramDocument, ".png");
        removeAttachments(diagramDocument, ".svg");
        xwiki.saveDocument(diagramDocument, "Clean old diagram attachments", false, context);
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
