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

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xwiki.diagram.internal.InlineDiagramManager;

import static com.xwiki.diagram.internal.InlineDiagramManager.DIAGRAM_SUFFIX;
import static com.xwiki.diagram.internal.InlineDiagramManager.PNG_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultInlineDiagramResource}.
 *
 * @version $Id$
 * @since 1.22.1
 */
@ComponentTest
public class DefaultInlineDiagramResourceTest
{
    private final String sourceReference = "wiki:Space.Page";

    private final String name = "myDiagram";

    private final InputStream stream = new ByteArrayInputStream("TEST".getBytes());

    private final String validBase64Body = Base64.getEncoder().encodeToString("TEST_IMAGE".getBytes());

    @InjectMockComponents
    private DefaultInlineDiagramResource resource;

    @MockComponent
    private InlineDiagramManager inlineDiagramManager;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp()
    {
        ReflectionUtils.setFieldValue(resource, "logger", this.logger);
    }

    @Test
    void saveCreatesNewDiagram() throws Exception
    {
        when(inlineDiagramManager.executeSave(sourceReference, name, stream, DIAGRAM_SUFFIX)).thenReturn(true);

        Response response = resource.save(sourceReference, name, stream);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    void saveUpdatesExistingDiagram() throws Exception
    {
        when(inlineDiagramManager.executeSave(sourceReference, name, stream, DIAGRAM_SUFFIX)).thenReturn(false);
        Response response = resource.save(sourceReference, name, stream);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void saveHandlesAccessDeniedException() throws Exception
    {
        DocumentReference reference = mock(DocumentReference.class);
        EntityReference entityReference = mock(EntityReference.class);
        AccessDeniedException exception = new AccessDeniedException(reference, entityReference);
        when(inlineDiagramManager.executeSave(sourceReference, name, stream, DIAGRAM_SUFFIX)).thenThrow(exception);
        Response response = resource.save(sourceReference, name, stream);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        verify(logger).error("Failed to save diagram [{}] on [{}] because of missing rights.",
            name, sourceReference, exception);
    }

    @Test
    void saveHandlesXWikiException() throws Exception
    {
        XWikiException exception = new XWikiException();
        when(inlineDiagramManager.executeSave(sourceReference, name, stream, DIAGRAM_SUFFIX)).thenThrow(exception);

        Response response = resource.save(sourceReference, name, stream);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        verify(logger).error(
            "Something went wrong while trying to save the diagram content. Content: [{}]",
            stream, exception);
    }

    @Test
    void saveHandlesIOException() throws Exception
    {
        IOException exception = new IOException("IO Error");
        when(inlineDiagramManager.executeSave(sourceReference, name, stream, DIAGRAM_SUFFIX)).thenThrow(exception);

        Response response = resource.save(sourceReference, name, stream);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        verify(logger).error(
            "Something went wrong while trying to save the diagram content. Content: [{}]",
            stream, exception);
    }

    @Test
    void saveRenderHandlesAccessDeniedException() throws Exception
    {
        DocumentReference reference = mock(DocumentReference.class);
        EntityReference entityReference = mock(EntityReference.class);
        AccessDeniedException exception = new AccessDeniedException(reference, entityReference);
        when(inlineDiagramManager.executeSave(eq(sourceReference), eq(name), any(ByteArrayInputStream.class),
            eq(PNG_SUFFIX))).thenThrow(exception);

        Response response = resource.saveRender(sourceReference, name, validBase64Body);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        verify(logger).error(
            "Failed to save the preview of diagram [{}] on [{}] because of missing rights.",
            name, sourceReference, exception);
    }

    @Test
    void saveRenderHandlesXWikiException() throws Exception
    {
        XWikiException exception = new XWikiException();
        when(inlineDiagramManager.executeSave(eq(sourceReference), eq(name), any(ByteArrayInputStream.class),
            eq(PNG_SUFFIX))).thenThrow(exception);

        Response response = resource.saveRender(sourceReference, name, validBase64Body);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        verify(logger).error("Something went wrong while trying to save the diagram content. Content: [{}]", validBase64Body,
            exception);
    }

    @Test
    void saveRenderHandlesIOException() throws Exception
    {
        IOException exception = new IOException("IO Error");
        when(inlineDiagramManager.executeSave(eq(sourceReference), eq(name), any(ByteArrayInputStream.class),
            eq(PNG_SUFFIX))).thenThrow(exception);

        Response response = resource.saveRender(sourceReference, name, validBase64Body);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        verify(logger).error("Something went wrong while trying to save the diagram content. Content: [{}]",validBase64Body,
            exception);
    }
}