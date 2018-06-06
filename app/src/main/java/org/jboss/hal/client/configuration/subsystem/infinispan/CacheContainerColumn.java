/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.hal.client.configuration.subsystem.infinispan;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import org.jboss.hal.core.CrudOperations;
import org.jboss.hal.core.finder.ColumnAction;
import org.jboss.hal.core.finder.ColumnActionFactory;
import org.jboss.hal.core.finder.Finder;
import org.jboss.hal.core.finder.FinderColumn;
import org.jboss.hal.core.finder.ItemAction;
import org.jboss.hal.core.finder.ItemActionFactory;
import org.jboss.hal.core.finder.ItemDisplay;
import org.jboss.hal.core.mvp.Places;
import org.jboss.hal.dmr.NamedNode;
import org.jboss.hal.dmr.Property;
import org.jboss.hal.meta.security.Constraint;
import org.jboss.hal.meta.token.NameTokens;
import org.jboss.hal.resources.Icons;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;
import org.jboss.hal.spi.AsyncColumn;
import org.jboss.hal.spi.Requires;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static org.jboss.hal.client.configuration.subsystem.infinispan.AddressTemplates.CACHE_CONTAINER_ADDRESS;
import static org.jboss.hal.client.configuration.subsystem.infinispan.AddressTemplates.CACHE_CONTAINER_TEMPLATE;
import static org.jboss.hal.client.configuration.subsystem.infinispan.AddressTemplates.INFINISPAN_SUBSYSTEM_TEMPLATE;
import static org.jboss.hal.client.configuration.subsystem.infinispan.AddressTemplates.REMOTE_CACHE_CONTAINER_TEMPLATE;
import static org.jboss.hal.dmr.ModelDescriptionConstants.*;
import static org.jboss.hal.resources.CSS.fontAwesome;
import static org.jboss.hal.resources.CSS.pfIcon;

@AsyncColumn(Ids.CACHE_CONTAINER)
@Requires(value = {CACHE_CONTAINER_ADDRESS}, recursive = false)
public class CacheContainerColumn extends FinderColumn<CacheContainer> {

    private final CrudOperations crud;

    @Inject
    public CacheContainerColumn(Finder finder,
            ColumnActionFactory columnActionFactory,
            ItemActionFactory itemActionFactory,
            CrudOperations crud,
            Places places,
            Resources resources) {

        super(new Builder<CacheContainer>(finder, Ids.CACHE_CONTAINER, Names.CACHE_CONTAINER)

                .itemsProvider((context, callback) -> crud.readChildren(INFINISPAN_SUBSYSTEM_TEMPLATE,
                        asList(CACHE_CONTAINER, REMOTE_CACHE_CONTAINER),
                        result -> {
                            List<CacheContainer> cc = new ArrayList<>();
                            for (Property property : result.step(0).get(RESULT).asPropertyList()) {
                                cc.add(new CacheContainer(property.getName(), false, property.getValue()));
                            }
                            for (Property property : result.step(1).get(RESULT).asPropertyList()) {
                                cc.add(new CacheContainer(property.getName(), true, property.getValue()));
                            }
                            cc.sort(comparing(NamedNode::getName));
                            callback.onSuccess(cc);
                        }))

                .onPreview(CacheContainerPreview::new)
                .useFirstActionAsBreadcrumbHandler()
                .withFilter()
                .showCount()
        );
        this.crud = crud;

        List<ColumnAction<CacheContainer>> addActions = new ArrayList<>();
        addActions.add(new ColumnAction.Builder<CacheContainer>(Ids.CACHE_CONTAINER_ADD)
                .title(resources.messages().addResourceTitle(Names.CACHE_CONTAINER))
                .handler(column -> addCacheContainer())
                .constraint(Constraint.executable(CACHE_CONTAINER_TEMPLATE, ADD))
                .build());
        addActions.add(new ColumnAction.Builder<CacheContainer>(Ids.REMOTE_CACHE_CONTAINER_ADD)
                .title(resources.messages().addResourceTitle(Names.REMOTE_CACHE_CONTAINER))
                .handler(column -> addRemoteCacheContainer())
                .constraint(Constraint.executable(REMOTE_CACHE_CONTAINER_TEMPLATE, ADD))
                .build());
        addColumnActions(Ids.CACHE_CONTAINER_ADD_ACTIONS, pfIcon("add-circle-o"), resources.constants().add(),
                addActions);
        addColumnAction(columnActionFactory.refresh(Ids.CACHE_CONTAINER_REFRESH));

        setItemRenderer(item -> new ItemDisplay<CacheContainer>() {
            @Override
            public String getId() {
                return item.isRemote() ? Ids.remoteCacheContainer(item.getName()) : Ids.cacheContainer(item.getName());
            }

            @Override
            public String getTitle() {
                return item.getName();
            }

            @Override
            public HTMLElement getIcon() {
                return item.isRemote() ? Icons.custom(fontAwesome("cloud")) : Icons.custom(pfIcon("memory"));
            }

            @Override
            public String getTooltip() {
                return item.isRemote() ? Names.REMOTE_CACHE_CONTAINER : Names.CACHE_CONTAINER;
            }

            @Override
            public String getFilterData() {
                String name = item.getName();
                return item.isRemote() ? name + " remote" : name;
            }

            @Override
            public HTMLElement asElement() {
                if (item.isRemote()) {
                    return item.hasDefined(DEFAULT_REMOTE_CLUSTER) ? ItemDisplay.withSubtitle(item.getName(),
                            item.get(DEFAULT_REMOTE_CLUSTER).asString()) : null;
                } else {
                    return item.hasDefined(DEFAULT_CACHE) ? ItemDisplay.withSubtitle(item.getName(),
                            item.get(DEFAULT_CACHE).asString()) : null;
                }
            }

            @Override
            public List<ItemAction<CacheContainer>> actions() {
                if (item.isRemote()) {
                    return asList(itemActionFactory.viewAndMonitor(Ids.remoteCacheContainer(item.getName()),
                            places.selectedProfile(NameTokens.REMOTE_CACHE_CONTAINER)
                                    .with(NAME, item.getName())
                                    .build()),
                            itemActionFactory.remove(Names.REMOTE_CACHE_CONTAINER, item.getName(),
                                    REMOTE_CACHE_CONTAINER_TEMPLATE, CacheContainerColumn.this));
                } else {
                    return asList(
                            itemActionFactory.viewAndMonitor(Ids.cacheContainer(item.getName()),
                                    places.selectedProfile(NameTokens.CACHE_CONTAINER)
                                            .with(NAME, item.getName())
                                            .build()),
                            itemActionFactory.remove(Names.CACHE_CONTAINER, item.getName(),
                                    CACHE_CONTAINER_TEMPLATE, CacheContainerColumn.this)
                    );
                }
            }
        });
    }

    private void addCacheContainer() {
        crud.add(Ids.CACHE_CONTAINER_ADD, Names.CACHE_CONTAINER, CACHE_CONTAINER_TEMPLATE,
                (name, address) -> refresh(Ids.cacheContainer(name)));
    }

    private void addRemoteCacheContainer() {
        DomGlobal.alert(Names.NYI);
    }
}
