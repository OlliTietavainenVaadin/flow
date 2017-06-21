/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.demo.views;

import com.vaadin.annotations.Tag;
import com.vaadin.flow.router.View;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;

/**
 * Base class for all the Views that demo some component.
 */
@Tag("div")
public abstract class DemoView extends Component
        implements View, HasComponents {

    // Default card. All views need one.
    private Card container;

    protected DemoView() {

        getElement().getStyle().set("textAlign", "center");
        container = new Card();

        getElement().appendChild(container.getElement());

        initView();
    }

    /**
     * Method run where the actual view builds its contents
     */
    abstract void initView();

    @Override
    public void add(Component... components) {
        container.add(components);
    }

    /**
     * Create and add a new component card to the view
     * 
     * @param components
     *            components to add on creation
     * @return created component container card
     */
    public Card addCard(Component... components) {
        Card card = new Card();
        card.add(components);

        getElement().appendChild(card.getElement());

        return card;
    }
}