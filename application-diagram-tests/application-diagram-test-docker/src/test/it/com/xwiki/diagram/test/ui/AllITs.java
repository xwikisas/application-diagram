package com.xwiki.diagram.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Poll application.
 *
 * @version $Id$
 * @since 2.2
 */
@UITest
class AllITs
{
    @Nested
    @DisplayName("Overall Poll UI")
    class NestedPollIT extends DiagramIT
    {
    }
}