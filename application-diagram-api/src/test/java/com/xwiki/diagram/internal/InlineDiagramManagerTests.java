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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AccessDeniedException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InlineDiagramManager}
 *
 * @version $Id$
 * @since 1.22.1
 */
@ComponentTest
public class InlineDiagramManagerTests
{
    private static final String SOURCE = "Space.Page";

    private static final String NAME = "diagram";

    private static final String EXACT_FILENAME = NAME + InlineDiagramManager.DIAGRAM_SUFFIX;

    @InjectMockComponents
    private InlineDiagramManager inlineDiagramManager;

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
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @BeforeEach
    void setup() throws Exception
    {
        ReflectionUtils.setFieldValue(inlineDiagramManager, "logger", this.logger);
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
    void saveExecuteShouldThrowAccessDeniedException() throws AccessDeniedException, IOException, XWikiException
    {
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);
        doThrow(accessDeniedException).when(authorization).checkAccess(Right.EDIT, documentReference);
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class,
            () -> inlineDiagramManager.executeSave(SOURCE, NAME, new ByteArrayInputStream("content".getBytes()),
                "ignore"));
        assertEquals(thrown, accessDeniedException);
    }

    @Test
    void saveExecuteShouldCreateAttachmentWhenNotExisting() throws AccessDeniedException, IOException, XWikiException
    {
        when(clonedDocument.getExactAttachment(EXACT_FILENAME)).thenReturn(null);
        InputStream body = new ByteArrayInputStream("new-content".getBytes());

        boolean createdOrUpdated =
            inlineDiagramManager.executeSave(SOURCE, NAME, body, InlineDiagramManager.DIAGRAM_SUFFIX);
        assertTrue(createdOrUpdated);
        verify(clonedDocument).setAttachment(eq(EXACT_FILENAME), any(InputStream.class), eq(context));
        verify(xwiki).saveDocument(clonedDocument, context);
    }

    @Test
    void saveExecuteShouldUpdateAttachmentWhenContentDifferent() throws Exception
    {

        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(EXACT_FILENAME)).thenReturn(attachment);
        when(attachment.getContentInputStream(context)).thenReturn(new ByteArrayInputStream("old-content".getBytes()));
        InputStream newBody = new ByteArrayInputStream("new-content".getBytes());

        boolean createOrUpdate =
            inlineDiagramManager.executeSave(SOURCE, NAME, newBody, InlineDiagramManager.DIAGRAM_SUFFIX);

        assertFalse(createOrUpdate);
        verify(attachment).setContent(any(InputStream.class));
        verify(xwiki).saveDocument(clonedDocument, context);
    }

    @Test
    void saveShouldNotUpdateWhenContentIsSame() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(EXACT_FILENAME)).thenReturn(attachment);

        byte[] content = "same-content".getBytes();
        when(attachment.getContentInputStream(context)).thenReturn(new ByteArrayInputStream(content));
        InputStream sameBody = new ByteArrayInputStream(content);

        boolean createOrUpdate =
            inlineDiagramManager.executeSave(SOURCE, NAME, sameBody, InlineDiagramManager.DIAGRAM_SUFFIX);

        assertFalse(createOrUpdate);

        verify(attachment, never()).setContent(ArgumentMatchers.<byte[]>any());
        verify(xwiki, never()).saveDocument(clonedDocument, context);
    }

    @Test
    void saveShouldReturnInternalServerErrorOnXWikiException() throws Exception
    {
        when(authorization.hasAccess(eq(Right.EDIT), eq(documentReference))).thenReturn(true);
        when(clonedDocument.getExactAttachment(EXACT_FILENAME)).thenReturn(null);
        XWikiException exception = new XWikiException();
        doThrow(exception).when(xwiki).saveDocument(any(), any());

        XWikiException thrown = assertThrows(XWikiException.class, () -> {
            inlineDiagramManager.executeSave(SOURCE, NAME, new ByteArrayInputStream("content".getBytes()),
                InlineDiagramManager.DIAGRAM_SUFFIX);
        });

        assertEquals(exception, thrown);
    }
}
