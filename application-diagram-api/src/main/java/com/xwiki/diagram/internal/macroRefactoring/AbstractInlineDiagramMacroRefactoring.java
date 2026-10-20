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

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.rendering.macro.MacroRefactoring;

import com.xpn.xwiki.XWikiContext;

/**
 * Common behavior for updating the name of the diagram when an attachment is moved.
 *
 * @version $Id$
 * @since 1.22.12
 */
public abstract class AbstractInlineDiagramMacroRefactoring implements MacroRefactoring
{
    protected static final String XML_ATTACHMENT_SUFFIX = ".diagram.xml";

    protected static final String DRAWIO_ATTACHMENT_SUFFIX = ".drawio";

    protected static final String FORMAT_NAME = "%s%s";

    protected static final String DIAGRAM_NAME = "diagramName";

    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    protected Logger logger;
}
