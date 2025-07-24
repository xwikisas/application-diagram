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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.XWikiRestException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DiagramResourcesImplTest}
 *
 * @version $Id$
 */
@ComponentTest
public class DiagramResourcesImplTest
{
    @InjectMockComponents
    private DiagramResourcesImpl diagramResources;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private XWiki wiki;

    @Mock
    private DocumentReference documentReference;

    private XWikiContext xWikiContext;

    private XWikiDocument xwikiDocument;

    private XWikiDocument clonedDocument;

    @BeforeComponent
    void beforeComponent()
    {
        // We need this before component because XWikiResource is calling the context in Initialize call.
        this.xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
    }

    @BeforeEach
    void setup() throws Exception
    {
        when(contextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(resolver.resolve("test")).thenReturn(documentReference);
        // We want a new document in every test.
        this.clonedDocument = new XWikiDocument(documentReference);
        this.xwikiDocument = mock(XWikiDocument.class);
        when(xwikiDocument.clone()).thenReturn(clonedDocument);
        when(wiki.getDocument(documentReference, xWikiContext)).thenReturn(xwikiDocument);
        when(documentAccessBridge.exists((DocumentReference) any())).thenReturn(true);
    }

    @Test
    void deleteOnlyDiagramAttachmentsWhenUserHasEditRights() throws Exception
    {
        List<XWikiAttachment> attachments =
            createAttachments("diagram.png", "diagram.svg", "diagram2.png", "diagram2.svg", "anotherAttachment");
        when(this.authorizationManager.hasAccess(Right.EDIT, xwikiDocument.getDocumentReference())).thenReturn(true);

        clonedDocument.setAttachmentList(attachments);

        diagramResources.deleteAttachments("test");

        // Assert: Only the non-diagram attachment should remain
        assertEquals(1, clonedDocument.getAttachmentList().size());
        assertNotNull(clonedDocument.getAttachment("anotherAttachment"));
    }

    @Test
    void deleteNoAttachmentsWhenUserLacksEditRights() throws XWikiRestException, XWikiException
    {
        List<XWikiAttachment> attachments = createAttachments("diagram.png", "diagram.svg", "anotherFile");
        when(this.authorizationManager.hasAccess(Right.EDIT, xwikiDocument.getDocumentReference())).thenReturn(false);
        clonedDocument.setAttachmentList(attachments);

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            diagramResources.deleteAttachments("test");
        });
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
        assertEquals(3, clonedDocument.getAttachmentList().size());
    }

    /**
     * Helper method to create a list of attachments with given names.
     */
    private List<XWikiAttachment> createAttachments(String... filenames)
    {
        List<XWikiAttachment> attachments = new ArrayList<>();
        for (String filename : filenames) {
            attachments.add(new XWikiAttachment(xwikiDocument, filename));
        }
        return attachments;
    }
}
