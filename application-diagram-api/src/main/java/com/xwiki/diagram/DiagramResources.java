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
package com.xwiki.diagram;

import javax.ws.rs.Encoded;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestComponent;

/**
 * Provides the APIs needed by the Diagram application in order to delete the attachments.
 *
 * @version $Id$
 */
@Path("/diagram")
public interface DiagramResources extends XWikiRestComponent
{
    /**
     * Deletes the attachments associated with a diagram.
     *
     * @param documentReference
     * @return OK if the deletion was successfully finished, 405 if the user doesn't have rights or 500 for other
     * errors.
     */
    @POST
    @Path("{documentReference}")
    Response deleteAttachments(
        @PathParam("documentReference") @Encoded String documentReference) throws Exception;
}
