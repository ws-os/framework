/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.data.selection;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.event.selection.SingleSelectionChange;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.shared.Registration;
import com.vaadin.shared.data.selection.SelectionModel.Single;
import com.vaadin.shared.data.selection.SelectionServerRpc;
import com.vaadin.ui.AbstractListing;
import com.vaadin.util.ReflectTools;

/**
 * A {@code SelectionModel} for selecting a single value. Implements
 * {@code Extension} to provide the communication logic for single selection for
 * the listing it extends.
 *
 * @author Vaadin Ltd.
 *
 * @param <T>
 *            the type of the items to select
 *
 * @since 8.0
 */
public class SingleSelection<T> extends AbstractSelectionModel<T>
        implements Single<T> {

    @Deprecated
    private static final Method SELECTION_CHANGE_METHOD = ReflectTools
            .findMethod(SingleSelectionListener.class, "accept",
                    SingleSelectionChange.class);

    /**
     * Creates a new {@code SingleSelection} extending the given parent listing.
     *
     * @param parent
     *            the parent listing
     */
    public SingleSelection(
            AbstractListing<T, ? super SingleSelection<T>> parent) {
        registerRpc(new SelectionServerRpc() {

            @Override
            public void select(String key) {
                if (!Objects.equals(selectedItem, getData(key))) {
                    doSelect(getData(key), true);
                }
            }

            @Override
            public void deselect(String key) {
                if (Objects.equals(selectedItem, getData(key))) {
                    doSelect(null, true);
                }
            }
        });
        extend(parent);
    }

    private T selectedItem = null;

    @Override
    public Optional<T> getSelectedItem() {
        return Optional.ofNullable(selectedItem);
    }

    @Override
    public void select(T item) {
        doSelect(item, false);
    }

    @Override
    public void deselect(T value) {
        if (Objects.equals(selectedItem, value)) {
            doSelect(null, false);
        }
    }

    @Override
    public void remove() {
        if (selectedItem != null) {
            refresh(selectedItem);
        }
        super.remove();
    }

    /**
     * Adds a selection listener. The listener is called when the value of this
     * {@code SingleSelection} is changed either by the user or
     * programmatically.
     *
     * @param listener
     *            the value change listener, not null
     * @return a registration for the listener
     */
    public Registration addSelectionListener(
            SingleSelectionListener<T> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        addListener(SingleSelectionChange.class, listener,
                SELECTION_CHANGE_METHOD);
        return () -> removeListener(SingleSelectionChange.class, listener);
    }

    /**
     * Selects the given item or deselects the current one if given
     * {@code null}.
     *
     * @param item
     *            the item to select or {@code null} to deselect
     * @param userOriginated
     *            {@code true} if this event originates from the client,
     *            {@code false} otherwise.
     */
    protected void doSelect(T item, boolean userOriginated) {
        if (!Objects.equals(item, selectedItem)) {
            if (selectedItem != null) {
                refresh(selectedItem);
            }
            selectedItem = item;
            if (selectedItem != null) {
                refresh(selectedItem);
            }
            fireEvent(new SingleSelectionChange<>(getParent(), selectedItem,
                    userOriginated));
        }
    }
}
