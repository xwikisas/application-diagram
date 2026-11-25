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

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.xml.html.HTMLElementSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DiagramSVGSanitizer}.
 *
 * @version $Id$
 */
@ComponentTest
class DiagramSVGSanitizerTest
{
    private static final Set<String> ALLOWED_ELEMENTS = Set.of("svg", "g", "rect", "a", "text", "switch", "div");

    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of("width", "height", "x", "y", "xlink:href", "style");

    @InjectMockComponents
    private DiagramSVGSanitizer sanitizer;

    @MockComponent
    private HTMLElementSanitizer htmlElementSanitizer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        when(this.htmlElementSanitizer.isElementAllowed(anyString()))
            .then(invocation -> ALLOWED_ELEMENTS.contains(invocation.getArgument(0, String.class)));
        when(this.htmlElementSanitizer.isAttributeAllowed(anyString(), anyString(), any()))
            .then(invocation -> ALLOWED_ATTRIBUTES.contains(invocation.getArgument(1, String.class))
                && !invocation.getArgument(2, String.class).startsWith("javascript:"));
    }

    @Test
    void sanitizeKeepsRegularDiagram()
    {
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"10\">"
            + "<g><a xlink:href=\"/xwiki/bin/view/Main/\"><rect x=\"1\" y=\"2\"/></a>"
            + "<switch><foreignObject requiredFeatures=\"f\" width=\"5\"><div style=\"color: red\">Label</div>"
            + "</foreignObject><text>Label</text></switch></g></svg>";

        assertEquals("<svg width=\"10\" xmlns=\"http://www.w3.org/2000/svg\" "
            + "xmlns:xlink=\"http://www.w3.org/1999/xlink\"><g><a xlink:href=\"/xwiki/bin/view/Main/\"><rect x=\"1\" y=\"2\"/></a>"
            + "<switch><foreignObject requiredFeatures=\"f\" width=\"5\"><div style=\"color: red\">Label</div>"
            + "</foreignObject><text>Label</text></switch></g></svg>", this.sanitizer.sanitize(svg));
    }

    @Test
    void sanitizeRemovesScriptsAndEventHandlers()
    {
        String svg = "<svg><script>alert(1)</script><rect onclick=\"alert(2)\" x=\"1\"/>"
            + "<a xlink:href=\"javascript:alert(3)\"><text>t</text></a>"
            + "<foreignObject onload=\"alert(4)\"><div>d</div></foreignObject><!-- c --></svg>";

        String result = this.sanitizer.sanitize(svg);

        assertEquals("<svg><rect x=\"1\"/><a><text>t</text></a><foreignObject><div>d</div></foreignObject></svg>",
            result);
    }

    @Test
    void sanitizeEncodesWikiSyntax()
    {
        String svg = "<svg><text>{{/html}}{{groovy}}println(1){{/groovy}}</text>"
            + "<rect style=\"a{{/html}}\"/><text><![CDATA[{{velocity}}]]></text></svg>";

        String result = this.sanitizer.sanitize(svg);

        assertFalse(result.contains("{"));
        assertTrue(result.contains("&#123;&#123;/html}}&#123;&#123;groovy}}"));
        assertTrue(result.contains("&#123;&#123;velocity}}"));
    }

    @Test
    void sanitizeRejectsInvalidContent()
    {
        assertEquals("", this.sanitizer.sanitize(null));
        assertEquals("", this.sanitizer.sanitize("not xml {{/html}}"));
        assertEquals("", this.sanitizer.sanitize("<html><script>alert(1)</script></html>"));
        assertEquals("", this.sanitizer.sanitize("<!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
            + "<svg>&x;</svg>"));
        assertEquals(2, this.logCapture.size());
        assertEquals("Failed to clean the diagram SVG image. Root cause: [Content is not allowed in prolog.]",
            this.logCapture.getMessage(0));
        assertTrue(this.logCapture.getMessage(1).startsWith("Failed to clean the diagram SVG image."));
    }
}
