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
package com.xwiki.diagram.internal.macroRefactoring;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.stability.Unstable;

/**
 * Handles the update of the embedded diagram reference when the source page is moved.
 *
 * @version $Id$
 * @since 1.22.11
 */
@Component
@Named("embedInlineDiagram")
@Singleton
@Unstable
public class EmbeddedInlineDiagramMacroRefactoring extends AbstractInlineDiagramMacroRefactoring
{
    private static final String SOURCE_DOCUMENT = "diagramSource";

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        String param = macroBlock.getParameter(SOURCE_DOCUMENT);
        if (param == null) {
            return Optional.empty();
        }
        DocumentReference documentReference = resolver.resolve(param);
        if (documentReference.equals(sourceReference)) {
            macroBlock.setParameter(SOURCE_DOCUMENT, serializer.serialize(targetReference));
            return Optional.of(macroBlock);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        String reference = macroBlock.getParameter(SOURCE_DOCUMENT);
        if (reference == null) {
            return Optional.empty();
        }
        String serializedSourceReference = serializer.serialize(sourceReference);
        if (reference.equals(serializedSourceReference)) {
            macroBlock.setParameter(SOURCE_DOCUMENT, serializer.serialize(targetReference));
            return Optional.of(macroBlock);
        }
        return Optional.empty();
    }
}
