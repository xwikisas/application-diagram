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

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DiagramMacroSolrMetadataExtractor}
 */
@ComponentTest
public class DiagramMacroSolrMetadataExtractorTest
{

    private static final String REFERENCE = "reference";

    private static final String STRATEGY = "Strategy";

    private static final String DIAGRAM = "Diagram";

    @InjectMockComponents
    private DiagramMacroSolrMetadataExtractor extractor;

    @MockComponent
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private EntityNameValidationManager entityNameValidationManager;

    @MockComponent
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @Mock
    private XWikiContext xwikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument xwikiDocument;

    @Mock
    private XDOM xdom;

    @Mock
    private EntityNameValidation entityNameValidation;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private SolrInputDocument solrDocument;

    @BeforeEach
    void setup()
    {
        when(this.xwikiDocument.getXDOM()).thenReturn(xdom);
        when(this.contextProvider.get()).thenReturn(this.xwikiContext);
        when(this.xwikiContext.getWiki()).thenReturn(this.wiki);
        when(this.xwikiDocument.getDocumentReference()).thenReturn(documentReference);
        when(entityNameValidationConfiguration.useValidation()).thenReturn(true);
        when(entityNameValidationConfiguration.useTransformation()).thenReturn(true);
        when(entityNameValidationManager.getEntityReferenceNameStrategy()).thenReturn(entityNameValidation);
    }

    /**
     * The diagram was created in another version, but the reference doesn't respect the name strategy, but we want to
     * keep the reference so we don't break the macros that already exist.
     *
     */
    @Test
    void updateReferenceCase1() throws XWikiException
    {
        MacroBlock block = this.createMacroBlock(STRATEGY);
        when(this.xwikiDocument.getXDOM().getBlocks(any(), any())).thenReturn(List.of(block));
        when(explicitDocumentReferenceResolver.resolve(STRATEGY, xwikiDocument.getDocumentReference())).thenReturn(
            documentReference);
        when(xwikiContext.getWiki().exists(documentReference, xwikiContext)).thenReturn(true);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().isValid(STRATEGY)).thenReturn(false);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().transform(STRATEGY)).thenReturn(
            "Strategy2");
        extractor.extract(this.xwikiDocument, this.solrDocument);
        assertEquals(STRATEGY, block.getParameter(REFERENCE));
    }

    /**
     * The diagram was created, it respects the name strategy, but the reference doesn't respect the name strategy.
     */
    @Test
    void updateReferenceCase2() throws XWikiException
    {
        MacroBlock block = this.createMacroBlock("AAAA");
        when(this.xwikiDocument.getXDOM().getBlocks(any(), any())).thenReturn(List.of(block));
        when(explicitDocumentReferenceResolver.resolve("AAAA", xwikiDocument.getDocumentReference())).thenReturn(
            documentReference);
        when(xwikiContext.getWiki().exists(documentReference, xwikiContext)).thenReturn(false);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().isValid("AAAA")).thenReturn(false);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().transform("AAAA")).thenReturn("BBBB");
        extractor.extract(this.xwikiDocument, this.solrDocument);
        assertEquals("BBBB", block.getParameter(REFERENCE));
    }

    /**
     * The macro has the default settings where the reference is missing and there is a naming strategy that updates the
     * name.
     *
     */
    @Test
    void updateReferenceCase3() throws XWikiException
    {
        MacroBlock block = this.createMacroBlock(null);
        when(this.xwikiDocument.getXDOM().getBlocks(any(), any())).thenReturn(List.of(block));
        when(explicitDocumentReferenceResolver.resolve(STRATEGY, xwikiDocument.getDocumentReference())).thenReturn(
            documentReference);
        when(xwikiContext.getWiki().exists(documentReference, xwikiContext)).thenReturn(false);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().isValid(DIAGRAM)).thenReturn(false);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().transform(DIAGRAM)).thenReturn("Anagram");
        extractor.extract(this.xwikiDocument, this.solrDocument);
        assertEquals("Anagram", block.getParameter(REFERENCE));
    }

    /**
     * Tests that the diagram macros are updated only when both Transform names automatically &  Validate names before
     * saving are set to true.
     *
     */
    @Test
    void updateReferenceCase4() throws XWikiException
    {
        assertTrue(this.testNameStrategyConfiguration(false, false));
        assertTrue(this.testNameStrategyConfiguration(true, false));
        assertTrue(this.testNameStrategyConfiguration(false, true));
    }

    private boolean testNameStrategyConfiguration(boolean transformsNameAutomatically,
        boolean validateNamesBeforeSaving) throws XWikiException
    {

        MacroBlock block = this.createMacroBlock(DIAGRAM);
        when(this.xwikiDocument.getXDOM().getBlocks(any(), any())).thenReturn(List.of(block));
        when(explicitDocumentReferenceResolver.resolve(DIAGRAM, xwikiDocument.getDocumentReference())).thenReturn(
            documentReference);
        when(xwikiContext.getWiki().exists(documentReference, xwikiContext)).thenReturn(false);
        when(entityNameValidationConfiguration.useValidation()).thenReturn(validateNamesBeforeSaving);
        when(entityNameValidationConfiguration.useTransformation()).thenReturn(transformsNameAutomatically);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().isValid(DIAGRAM)).thenReturn(false);
        when(entityNameValidationManager.getEntityReferenceNameStrategy().transform(DIAGRAM)).thenReturn("Anagram");
        extractor.extract(this.xwikiDocument, this.solrDocument);
        return DIAGRAM.equals(block.getParameter(REFERENCE));
    }

    private MacroBlock createMacroBlock(String name)
    {
        return new MacroBlock("diagram", Collections.singletonMap(REFERENCE, name), false);
    }
}
