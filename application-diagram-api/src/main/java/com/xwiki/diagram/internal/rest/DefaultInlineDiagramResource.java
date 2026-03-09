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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;
import com.xwiki.diagram.InlineDiagramResources;
import com.xwiki.diagram.internal.InlineDiagramManager;

import static com.xwiki.diagram.internal.InlineDiagramManager.DIAGRAM_SUFFIX;
import static com.xwiki.diagram.internal.InlineDiagramManager.PNG_SUFFIX;

/**
 * Default implementation of {@link InlineDiagramResources}.
 *
 * @version $Id$
 * @since 1.22.1
 */
@Component
@Named("com.xwiki.diagram.internal.rest.DefaultInlineDiagramResource")
@Singleton
@Unstable
public class DefaultInlineDiagramResource extends XWikiResource implements InlineDiagramResources
{
    @Inject
    private Logger logger;

    @Inject
    private InlineDiagramManager inlineDiagramManager;

    @Override
    public Response save(String sourceReference, String name, InputStream body)
    {
        try {
            boolean createdOrUpdated = inlineDiagramManager.executeSave(sourceReference, name, body, DIAGRAM_SUFFIX);
            return createdOrUpdated ? Response.status(Response.Status.CREATED).build() :
                Response.status(Response.Status.OK).build();
        } catch (AccessDeniedException e) {
            logger.error("Failed to save diagram [{}] on [{}] because of missing rights.", name, sourceReference, e);
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (XWikiException | IOException e) {
            logger.error("Something went wrong while trying to save the diagram content. Content: [{}]", body, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response saveRender(String sourceReference, String name, String body)
    {

        byte[] img = Base64.getDecoder().decode(body);
        ByteArrayInputStream stream = new ByteArrayInputStream(img);
        try {
            boolean createdOrUpdated = inlineDiagramManager.executeSave(sourceReference, name, stream, PNG_SUFFIX);
            return createdOrUpdated ? Response.status(Response.Status.CREATED).build() :
                Response.status(Response.Status.OK).build();
        } catch (AccessDeniedException e) {
            logger.error("Failed to save the preview of diagram [{}] on [{}] because of missing rights.", name,
                sourceReference, e);
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (XWikiException | IOException e) {
            logger.error("Something went wrong while trying to save the diagram content. Content: [{}]", body, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
