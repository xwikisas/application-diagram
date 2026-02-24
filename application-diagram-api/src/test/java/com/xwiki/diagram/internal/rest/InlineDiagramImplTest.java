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
import java.io.InputStream;
import java.util.Base64;

import javax.inject.Provider;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xwiki.diagram.InlineDiagramResources}
 */
@ComponentTest
class InlineDiagramImplTest
{

    private static final String SOURCE = "Space.Page";

    private static final String NAME = "diagram";

    private static final String SUFFIX = ".diagram.xml";

    @InjectMockComponents
    private InlineDiagramImpl inlineDiagram;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Logger logger;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiDocument clonedDocument;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private XWikiAttachment attachment;

    @BeforeComponent
    void beforeComponent()
    {
        // We need this before component because XWikiResource is calling the context in Initialize call.
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @BeforeEach
    void setup() throws Exception
    {
        ReflectionUtils.setFieldValue(inlineDiagram, "logger", this.logger);
        when(resolver.resolve(SOURCE)).thenReturn(documentReference);
        when(contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(documentReference, context)).thenReturn(document);
        when(document.clone()).thenReturn(clonedDocument);
        when(clonedDocument.getDocumentReference()).thenReturn(documentReference);
        when(context.getAuthorReference()).thenReturn(documentReference);
        when(clonedDocument.setAttachment(any(), any(), any())).thenReturn(attachment);
    }

    @Test
    void saveShouldReturnForbiddenWhenNoEditRight() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(false);

        Response response = inlineDiagram.save(SOURCE, NAME, new ByteArrayInputStream("content".getBytes()));
        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        verify(xwiki, never()).saveDocument(any(), any());
    }

    @Test
    void saveShouldCreateAttachmentWhenNotExisting() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(NAME + SUFFIX)).thenReturn(null);
        InputStream body = new ByteArrayInputStream("new-content".getBytes());

        Response response = inlineDiagram.save(SOURCE, NAME, body);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        verify(clonedDocument).setAttachment(eq(NAME + SUFFIX), any(InputStream.class), eq(context));
        verify(xwiki).saveDocument(clonedDocument, context);
    }

    @Test
    void saveShouldUpdateAttachmentWhenContentDifferent() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(NAME + SUFFIX)).thenReturn(attachment);
        when(attachment.getContentInputStream(context)).thenReturn(new ByteArrayInputStream("old-content".getBytes()));
        InputStream newBody = new ByteArrayInputStream("new-content".getBytes());

        Response response = inlineDiagram.save(SOURCE, NAME, newBody);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        verify(attachment).setContent(any(InputStream.class));
        verify(xwiki).saveDocument(clonedDocument, context);
    }

    @Test
    void saveShouldNotUpdateWhenContentIsSame() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(NAME + SUFFIX)).thenReturn(attachment);

        byte[] content = "same-content".getBytes();
        when(attachment.getContentInputStream(context)).thenReturn(new ByteArrayInputStream(content));
        InputStream sameBody = new ByteArrayInputStream(content);

        Response response = inlineDiagram.save(SOURCE, NAME, sameBody);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        verify(attachment, never()).setContent(ArgumentMatchers.<byte[]>any());
        verify(xwiki, never()).saveDocument(clonedDocument, context);
    }

    @Test
    void saveShouldReturnInternalServerErrorOnXWikiException() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(NAME + SUFFIX)).thenReturn(null);
        doThrow(new XWikiException()).when(xwiki).saveDocument(any(), any());

        Response response = inlineDiagram.save(SOURCE, NAME, new ByteArrayInputStream("content".getBytes()));

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        verify(logger).error(startsWith("Something went wrong"), any(XWikiException.class));
    }

    @Test
    void saveRenderShouldReturnForbiddenWhenNoEditRight() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(false);

        String body = Base64.getEncoder().encodeToString("image-content".getBytes());
        Response response = inlineDiagram.saveRender(SOURCE, NAME, body);

        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        verify(xwiki, never()).saveDocument(any(), any());
    }

    @Test
    void saveRenderShouldUpdateAttachmentWhenContentDifferent() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(NAME + ".png")).thenReturn(attachment);
        when(attachment.getContentInputStream(context)).thenReturn(new ByteArrayInputStream("old-image".getBytes()));

        String body = Base64.getEncoder().encodeToString("new-image".getBytes());
        Response response = inlineDiagram.saveRender(SOURCE, NAME, body);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        verify(attachment).setContent(any(InputStream.class));
        verify(xwiki).saveDocument(clonedDocument, context);
    }

    @Test
    void saveRenderShouldNotUpdateWhenContentIsSame() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(NAME + ".png")).thenReturn(attachment);

        byte[] content = "same-image".getBytes();
        when(attachment.getContentInputStream(context)).thenReturn(new ByteArrayInputStream(content));

        String body = Base64.getEncoder().encodeToString(content);
        Response response = inlineDiagram.saveRender(SOURCE, NAME, body);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        verify(attachment, never()).setContent(any(InputStream.class));
        verify(xwiki, never()).saveDocument(clonedDocument, context);
    }

    @Test
    void saveRenderShouldReturnInternalServerErrorOnException() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(NAME + ".png")).thenReturn(null);
        doThrow(new XWikiException()).when(xwiki).saveDocument(any(), any());

        String body = Base64.getEncoder().encodeToString("image-content".getBytes());
        Response response = inlineDiagram.saveRender(SOURCE, NAME, body);

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        verify(logger).error(startsWith("Something went wrong"), any(XWikiException.class));
    }

}
