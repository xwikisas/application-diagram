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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;

import com.xpn.xwiki.XWikiContext;

/**
 * Common behavior for updating the name of the diagram when an attachment is moved.
 *
 * @version $Id$
 * @since 1.22.12
 */
public abstract class AbstractInlineDiagramMacroRefactoring implements MacroRefactoring
{
    protected static final String ATTACHMENT_SUFFIX = ".diagram.xml";

    protected static final String FORMAT_NAME = "%s%s";

    protected static final String DIAGRAM_NAME = "diagramName";

    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    protected Logger logger;

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        String diagramName = String.format(FORMAT_NAME, macroBlock.getParameter(DIAGRAM_NAME), ATTACHMENT_SUFFIX);

        // We should refactor the macro block only if the old name of the moved attachment matches the
        // diagramName parameter of the macro, and if the attachment is moved on the same page, because the macro is
        // not designed to handle macros from other pages. In that case, we should just use the default diagram macro.
        if (sourceReference.getName().equals(diagramName) && sourceReference.getParent()
            .equals(targetReference.getParent()))
        {
            String referenceName = targetReference.getName();
            String newName = referenceName.substring(0, referenceName.length() - ATTACHMENT_SUFFIX.length());
            macroBlock.setParameter(DIAGRAM_NAME, newName);
            return Optional.of(macroBlock);
        }
        return Optional.empty();
    }
}
