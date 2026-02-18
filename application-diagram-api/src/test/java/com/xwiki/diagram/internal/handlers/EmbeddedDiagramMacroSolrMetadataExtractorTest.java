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
package com.xwiki.diagram.internal.handlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmbeddedDiagramMacroSolrMetadataExtractor}
 */
@ComponentTest
class EmbeddedDiagramMacroSolrMetadataExtractorTest
{
    @InjectMockComponents
    private EmbeddedDiagramMacroSolrMetadataExtractor extractor;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("explicit")
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private LinkRegistry linkRegistry;

    @Mock
    private XWikiDocument document;

    @Mock
    private XDOM xdom;

    @Mock
    private Block macroBlock;

    @Mock
    private Block macroBlock2;

    @Mock
    private SolrInputDocument solrDocument;

    @Mock
    private DocumentReference currentDocRef;

    @Mock
    private DocumentReference resolvedDocRef;

    @BeforeEach
    void setUp()
    {
        when(document.getDocumentReference()).thenReturn(currentDocRef);
        when(document.getXDOM()).thenReturn(xdom);
    }

    @Test
    void extractReturnsFalseWhenNoMacros()
    {
        when(xdom.getBlocks(any(MacroBlockMatcher.class), any())).thenReturn(null);

        boolean result = extractor.extract(document, solrDocument);

        assertFalse(result);
        verifyNoInteractions(linkRegistry);
    }

    @Test
    void extractReturnsFalseWhenMacroListEmpty()
    {
        when(xdom.getBlocks(any(MacroBlockMatcher.class), any())).thenReturn(Collections.emptyList());

        boolean result = extractor.extract(document, solrDocument);

        assertFalse(result);
        verifyNoInteractions(linkRegistry);
    }

    @Test
    void extractSingleMacroRegistersBacklinks()
    {
        when(xdom.getBlocks(any(MacroBlockMatcher.class), any())).thenReturn(Collections.singletonList(macroBlock));

        when(macroBlock.getParameter("diagramSource")).thenReturn("Space.Page");
        when(macroBlock.getParameter("diagramName")).thenReturn("myDiagram");

        when(resolver.resolve("Space.Page", currentDocRef)).thenReturn(resolvedDocRef);

        when(linkRegistry.registerBacklinks(any(), any())).thenReturn(true);
        ArgumentCaptor<List<EntityReference>> captor = ArgumentCaptor.forClass(List.class);

        boolean result = extractor.extract(document, solrDocument);

        assertTrue(result);
        verify(linkRegistry).registerBacklinks(eq(solrDocument), captor.capture());

        List<EntityReference> references = captor.getValue();

        assertEquals(2, references.size());
        assertInstanceOf(DocumentReference.class, references.get(0));
        assertInstanceOf(AttachmentReference.class, references.get(1));

        assertEquals("myDiagram.diagram.xml", references.get(1).getName());
        assertEquals(resolvedDocRef, references.get(0));
    }

    @Test
    void extractMultipleMacrosAggregatesAllReferences()
    {

        when(xdom.getBlocks(any(MacroBlockMatcher.class), any())).thenReturn(Arrays.asList(macroBlock, macroBlock2));

        when(macroBlock.getParameter("diagramSource")).thenReturn("Space.Page1");
        when(macroBlock.getParameter("diagramName")).thenReturn("diagram1");

        when(macroBlock2.getParameter("diagramSource")).thenReturn("Space.Page2");
        when(macroBlock2.getParameter("diagramName")).thenReturn("diagram2");

        DocumentReference resolved1 = mock(DocumentReference.class);
        DocumentReference resolved2 = mock(DocumentReference.class);

        when(resolver.resolve("Space.Page1", currentDocRef)).thenReturn(resolved1);
        when(resolver.resolve("Space.Page2", currentDocRef)).thenReturn(resolved2);

        when(linkRegistry.registerBacklinks(any(), any())).thenReturn(true);
        ArgumentCaptor<List<EntityReference>> captor = ArgumentCaptor.forClass(List.class);

        boolean result = extractor.extract(document, solrDocument);

        assertTrue(result);
        verify(linkRegistry).registerBacklinks(eq(solrDocument), captor.capture());
        List<EntityReference> references = captor.getValue();
        assertEquals(4, references.size());
    }
}
