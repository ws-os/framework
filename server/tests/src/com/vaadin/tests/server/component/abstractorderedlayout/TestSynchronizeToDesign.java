/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.tests.server.component.abstractorderedlayout;

import junit.framework.TestCase;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.DesignContext;

/**
 * Test case for writing abstract ordered layout to design
 * 
 * @since
 * @author Vaadin Ltd
 */
public class TestSynchronizeToDesign extends TestCase {

    public void testSynchronizeEmptyLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setCaption("changed-caption");
        Node design = createDesign();
        layout.synchronizeToDesign(design, createDesignContext());
        assertEquals(0, design.childNodes().size());
        assertEquals("changed-caption", design.attr("caption"));
    }

    public void testSynchronizeLayoutWithChildren() {
        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(new Label("test-label"));
        layout.getComponent(0).setCaption("test-caption");
        layout.addComponent(new Label("test-label-2"));
        Node design = createDesign();
        layout.synchronizeToDesign(design, createDesignContext());
        assertEquals(2, design.childNodes().size());
        assertEquals("v-label", ((Element) design.childNode(0)).tagName());
        assertEquals("test-caption", design.childNode(0).attr("caption"));
    }

    public void testSynchronizeUnitExpandRatio() {
        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(new Label("test-label"));
        layout.setExpandRatio(layout.getComponent(0), 1.0f);
        Node design = createDesign();
        layout.synchronizeToDesign(design, createDesignContext());
        assertTrue(design.childNode(0).hasAttr(":expand"));
        assertEquals("", design.childNode(0).attr(":expand"));
    }

    public void testSynchronizeArbitraryExpandRatio() {
        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(new Label("test-label"));
        layout.setExpandRatio(layout.getComponent(0), 2.40f);
        Node design = createDesign();
        layout.synchronizeToDesign(design, createDesignContext());
        assertTrue(design.childNode(0).hasAttr(":expand"));
        assertEquals("2.4", design.childNode(0).attr(":expand"));
    }

    public void testSynchronizeDefaultAlignment() {
        Node design = createDesign();
        VerticalLayout layout = createLayoutWithAlignment(design, null);
        layout.synchronizeToDesign(design, createDesignContext());
        assertFalse(design.childNode(0).hasAttr(":top"));
        assertFalse(design.childNode(0).hasAttr(":left"));
    }

    public void testSynchronizeMiddleCenter() {
        Node design = createDesign();
        VerticalLayout layout = createLayoutWithAlignment(design,
                Alignment.MIDDLE_CENTER);
        layout.synchronizeToDesign(design, createDesignContext());
        assertTrue(design.childNode(0).hasAttr(":middle"));
        assertTrue(design.childNode(0).hasAttr(":center"));
    }

    public void testSynchronizeBottomRight() {
        Node design = createDesign();
        VerticalLayout layout = createLayoutWithAlignment(design,
                Alignment.BOTTOM_RIGHT);
        layout.synchronizeToDesign(design, createDesignContext());
        assertTrue(design.childNode(0).hasAttr(":bottom"));
        assertTrue(design.childNode(0).hasAttr(":right"));
    }

    private VerticalLayout createLayoutWithAlignment(Node design,
            Alignment alignment) {
        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(new Label("test-label"));
        if (alignment != null) {
            layout.setComponentAlignment(layout.getComponent(0), alignment);
        }
        layout.synchronizeToDesign(design, createDesignContext());
        return layout;
    }

    private Node createDesign() {
        // make sure that the design node has old content that should be removed
        Attributes rootAttributes = new Attributes();
        rootAttributes.put("caption", "test-layout");
        Element node = new Element(Tag.valueOf("v-vertical-layout"), "",
                rootAttributes);
        Attributes firstChildAttributes = new Attributes();
        firstChildAttributes.put("caption", "test-label");
        Element firstChild = new Element(Tag.valueOf("v-label"), "",
                firstChildAttributes);
        node.appendChild(firstChild);

        Attributes secondChildAttributes = new Attributes();
        secondChildAttributes.put("caption", "test-button");
        Element secondChild = new Element(Tag.valueOf("v-button"), "",
                secondChildAttributes);
        node.appendChild(secondChild);
        return node;
    }

    private DesignContext createDesignContext() {
        return new DesignContext();
    }
}
