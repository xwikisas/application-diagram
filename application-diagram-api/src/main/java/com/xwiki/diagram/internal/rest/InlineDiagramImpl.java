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

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.diagram.InlineDiagram;

/**
 * Default implementation of {@link InlineDiagram}.
 *
 * @version $Id$
 * @since 1.22.1
 */
@Component
@Named("com.xwiki.diagram.internal.rest.InlineDiagramImpl")
@Singleton
@Unstable
public class InlineDiagramImpl extends XWikiResource implements InlineDiagram
{
    private static final String SUFFIX = ".diagram.xml";

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public Response save(String sourceReference, String name, InputStream body)
    {
        try {
            DocumentReference reference = resolver.resolve(sourceReference);
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument sourceDocument = xwiki.getDocument(reference, context).clone();
            // Check that the user that made the request has edit rights over the document.
            if (!authorization.hasAccess(Right.EDIT, sourceDocument.getDocumentReference())) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String fileExactName = name + SUFFIX;
            XWikiAttachment attachment = sourceDocument.getExactAttachment(fileExactName);
            if (attachment == null) {
                return createAttachment(xwiki, context, sourceDocument, fileExactName, body);
            }
            return updateAttachment(xwiki, context, sourceDocument, attachment, body);
        } catch (XWikiException e) {
            logger.error("Something went wrong while trying to update the XWiki document", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            logger.error("Something went wrong while trying to update the XWiki attachment", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response createAttachment(XWiki xwiki, XWikiContext context, XWikiDocument document, String name,
        InputStream body) throws IOException, XWikiException
    {
        document.setAttachment(name, body, context);
        xwiki.saveDocument(document, context);
        return Response.status(Response.Status.CREATED).build();
    }

    private Response updateAttachment(XWiki xwiki, XWikiContext context, XWikiDocument document,
        XWikiAttachment attachment, InputStream body) throws IOException, XWikiException
    {
        attachment.setContent(body);
        xwiki.saveDocument(document, context);
        return Response.status(Response.Status.OK).build();
    }
}
