package com.xwiki.diagram.internal.macroRefactoring;

import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmbeddedInlineDiagramMacroRefactoring}
 */
@ComponentTest
public class EmbeddedInlineDiagramMacroRefactoringTest
{
    private static final String SOURCE_DOCUMENT = "diagramSource";

    @InjectMockComponents
    private EmbeddedInlineDiagramMacroRefactoring refactoring;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @Mock
    private MacroBlock macroBlock;

    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> serializer;

    @Test
    void testReplaceReferenceUpdateWithSuccess() throws Exception
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(macroBlock.getParameter(SOURCE_DOCUMENT)).thenReturn("test");
        when(resolver.resolve("test")).thenReturn(documentReference);

        Optional<MacroBlock> optional =
            refactoring.replaceReference(macroBlock, documentReference, documentReference, documentReference, false);
        assertTrue(optional.isPresent());
        verify(serializer).serialize(documentReference);
    }

    @Test
    void testReplaceReferenceUpdateWithoutSuccess() throws Exception
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(macroBlock.getParameter(SOURCE_DOCUMENT)).thenReturn("test");
        when(resolver.resolve("test")).thenReturn(documentReference);

        Optional<MacroBlock> optional =
            refactoring.replaceReference(macroBlock, documentReference, mock(DocumentReference.class),
                documentReference, false);
        assertTrue(optional.isEmpty());
    }
}
