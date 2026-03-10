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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Triggers the data migration when the Diagram Application is upgraded.
 *
 * @version $Id$
 * @since 1.11
 */
@Component
@Named(DiagramApplicationListener.ROLE_HINT)
@Singleton
public class DiagramApplicationListener extends AbstractEventListener implements Initializable
{
    protected static final String ROLE_HINT = "DiagramApplicationListener";

    private static final String DIAGRAM_APPLICATION_ID = "com.xwiki.diagram:application-diagram";

    @Inject
    private Logger logger;

    @Inject
    private StoreSVGAsAttachmentMigration svgMigrator;

    @Inject
    private DrawIOImagePathMigration imagePathMigrator;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ComponentManager componentManager;

    /**
     * Default constructor.
     */
    public DiagramApplicationListener()
    {
        super(ROLE_HINT,
            Arrays.<Event>asList(new ExtensionUpgradedEvent(DIAGRAM_APPLICATION_ID), new ExtensionInstalledEvent()));
    }

    /**
     * The migration should be done at ExtensionUpgradedEvent, but for avoiding XCOMMONS-751: Getting wrong component
     * instance during JAR extension upgrade, it is done also at initialization step, since when an extension is
     * upgraded its listeners are initialized too. After the issue is fixed and diagram starts depending on a version of
     * XWiki >= the version where is fixed, then only the migration from inside the event should be executed.
     */
    @Override
    public void initialize()
    {
        // Don't trigger the migration process at xwiki startup time.
        if (this.contextProvider.get() != null) {
            boolean hasNamespace = componentManager instanceof NamespacedComponentManager;
            getTargetWikis(
                hasNamespace ? ((NamespacedComponentManager) componentManager).getNamespace() : null).forEach(
                this::migrate);
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ExtensionUpgradedEvent || isDiagramInstallEvent(event)) {
            ExtensionEvent extensionEvent = (ExtensionEvent) event;
            getTargetWikis(extensionEvent.hasNamespace() ? extensionEvent.getNamespace() : null).forEach(this::migrate);
        }
    }

    private static boolean isDiagramInstallEvent(Event event)
    {
        return event instanceof ExtensionInstalledEvent && DIAGRAM_APPLICATION_ID.equals(
            ((ExtensionEvent) event).getExtensionId().getId());
    }

    private Collection<String> getTargetWikis(String namespace)
    {
        // Checking also for null namespace since it could mean that the upgrade is done on farm level.
        if (namespace != null && namespace.startsWith("wiki:")) {
            return Collections.singleton(namespace.substring(5));
        } else {
            try {
                return this.wikiDescriptorManager.getAllIds();
            } catch (WikiManagerException e) {
                this.logger.error("Failed to get the list of wikis.", e);
                return Collections.emptySet();
            }
        }
    }

    private void migrate(String wiki)
    {
        this.svgMigrator.migrate(wiki);
        this.imagePathMigrator.migrate(wiki);
    }
}
