package com.xwiki.diagram.internal.macroRefactoring;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.diagram.internal.handlers.DiagramContentHandler;
import com.xwiki.diagram.internal.handlers.DiagramLinkHandler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InlineDiagramMacroRefactoring}
 */
@ComponentTest
class InlineDiagramMacroRefactoringTest
{
    private static final String DIAGRAM_NAME = "diagramName";

    private final DocumentReference currentDocRef = new DocumentReference("wiki", "Space", "Page");

    private final DocumentReference sourceRef = new DocumentReference("wiki", "Space", "OldPage");

    private final DocumentReference targetRef = new DocumentReference("wiki", "Space", "NewPage");

    @MockComponent
    protected Provider<XWikiContext> contextProvider;

    @MockComponent
    protected Logger logger;

    @InjectMockComponents
    private InlineDiagramMacroRefactoring refactoring;

    @MockComponent
    private DiagramLinkHandler linkHandler;

    @MockComponent
    private DiagramContentHandler contentHandler;

    @Mock
    private MacroBlock macroBlock;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiAttachment attachment;

    @BeforeEach
    void setUp() throws Exception
    {
        when(macroBlock.getParameter(DIAGRAM_NAME)).thenReturn("MyDiagram");
        when(contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(currentDocRef, context)).thenReturn(document);
        when(document.clone()).thenReturn(document);
    }

    @Test
    void testReplaceReferenceAttachmentNotFound() throws Exception
    {
        when(document.getExactAttachment("MyDiagram.diagram.xml")).thenReturn(null);

        Optional<MacroBlock> result =
            refactoring.replaceReference(macroBlock, currentDocRef, sourceRef, targetRef, true);

        assertTrue(result.isEmpty());
        verify(xwiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    @Test
    void testReplaceReferenceAttachmentUpdated() throws Exception
    {
        String xmlContent = "<root><UserObject></UserObject></root>";
        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));

        when(document.getExactAttachment("MyDiagram.diagram.xml")).thenReturn(attachment);
        when(attachment.getContentInputStream(context)).thenReturn(inputStream);
        when(linkHandler.updateUserObjectNode(any(), eq(targetRef), eq(sourceRef))).thenReturn(true);

        Optional<MacroBlock> result =
            refactoring.replaceReference(macroBlock, currentDocRef, sourceRef, targetRef, true);

        assertTrue(result.isEmpty());
        verify(attachment).setContent(any(ByteArrayInputStream.class));
        verify(xwiki).saveDocument(document, "Refactor diagram attachment", true, context);
    }

    @Test
    void testReplaceAttachmentReferenceWithoutSuccess() throws Exception
    {

        when(macroBlock.getParameter(DIAGRAM_NAME)).thenReturn("mydiagram");
        String diagramName = "mydyagram.diagram.xml";
        AttachmentReference source = new AttachmentReference("somename", mock(DocumentReference.class));
        AttachmentReference target = mock(AttachmentReference.class, RETURNS_DEEP_STUBS);

        Optional<MacroBlock> block = refactoring.replaceReference(macroBlock, currentDocRef, source, target, false);
        assertTrue(block.isEmpty());
    }

    @Test
    void testReplaceAttachmentReferenceWithSuccess() throws Exception
    {
        when(macroBlock.getParameter(DIAGRAM_NAME)).thenReturn("mydiagram");
        DocumentReference documentReference = mock(DocumentReference.class);
        AttachmentReference source = new AttachmentReference("mydiagram.diagram.xml", documentReference);
        AttachmentReference target = new AttachmentReference("new.diagram.xml", documentReference);

        Optional<MacroBlock> block = refactoring.replaceReference(macroBlock, currentDocRef, source, target, false);
        assertTrue(block.isPresent());
        verify(macroBlock).setParameter(DIAGRAM_NAME, "new");
    }
}
