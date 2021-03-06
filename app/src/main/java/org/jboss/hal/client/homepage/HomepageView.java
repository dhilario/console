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
package org.jboss.hal.client.homepage;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import elemental2.dom.HTMLElement;
import org.jboss.hal.config.Build;
import org.jboss.hal.config.Environment;
import org.jboss.hal.core.accesscontrol.AccessControl;
import org.jboss.hal.core.mvp.HalViewImpl;
import org.jboss.hal.core.mvp.Places;
import org.jboss.hal.meta.token.NameTokens;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;

import static org.jboss.gwt.elemento.core.Elements.*;
import static org.jboss.gwt.elemento.core.EventType.click;
import static org.jboss.hal.resources.CSS.clickable;
import static org.jboss.hal.resources.CSS.eapHomeRow;
import static org.jboss.hal.resources.CSS.eapHomeTitle;

public class HomepageView extends HalViewImpl implements HomepagePresenter.MyView {

    private HomepagePresenter presenter;

    @Inject
    public HomepageView(Environment environment, AccessControl ac, Resources resources, Places places) {

        boolean standalone = environment.isStandalone();
        boolean ssoEnabled = environment.isSingleSignOn();
        boolean community = environment.getHalBuild() == Build.COMMUNITY;
        boolean su = ac.isSuperUserOrAdministrator();
        String name = environment.getInstanceInfo().productName();

        Iterable<HomepageSection> sections;
        HTMLElement header;
        HTMLElement deployments;
        HTMLElement configuration;
        HTMLElement runtime;
        HTMLElement accessControl = div().asElement(); // to get rid of warning "might not be initialized"
        HTMLElement patching = div().asElement();
        HTMLElement help;

        if (community) {
            header = div().css(eapHomeTitle)
                    .add(h(1).textContent(resources.theme().getFullName()))
                    .asElement();
        } else {
            header = div().css(eapHomeTitle)
                    .add(p()
                            .add(span().textContent(resources.constants().homepageNewToEap() + " "))
                            .add(a().css(clickable)
                                    .on(click, event -> presenter.launchGuidedTour())
                                    .textContent(resources.constants().homepageTakeATour())))
                    .add(h(1).textContent(resources.theme().getFullName()))
                    .asElement();
        }

        if (standalone) {
            sections = Collections.singleton(HomepageSection.create(places, resources,
                    Ids.HOMEPAGE_DEPLOYMENTS_SECTION, org.jboss.hal.meta.token.NameTokens.DEPLOYMENTS,
                    resources.constants().homepageDeploymentsSection(),
                    resources.constants().homepageDeploymentsStandaloneStepIntro(),
                    Arrays.asList(resources.constants().homepageDeploymentsStandaloneStep1(),
                            resources.constants().homepageDeploymentsStepEnable()), true));
            deployments = HomepageModule.create(places,
                    org.jboss.hal.meta.token.NameTokens.DEPLOYMENTS,
                    Names.DEPLOYMENTS,
                    resources.constants().homepageDeploymentsSubHeader(),
                    resources.images().deployments(),
                    sections).asElement();

            sections = Collections.singleton(HomepageSection.create(places, resources,
                    Ids.HOMEPAGE_CONFIGURATION_SECTION, org.jboss.hal.meta.token.NameTokens.CONFIGURATION,
                    resources.constants().homepageConfigurationSection(),
                    resources.constants().homepageConfigurationStepIntro(),
                    Arrays.asList(
                            resources.constants().homepageConfigurationStandaloneStep1(),
                            resources.constants().homepageConfigurationStep2(),
                            resources.constants().homepageConfigurationStep3()), true));
            configuration = HomepageModule.create(places,
                    org.jboss.hal.meta.token.NameTokens.CONFIGURATION,
                    Names.CONFIGURATION,
                    resources.constants().homepageConfigurationStandaloneSubHeader(),
                    resources.images().configuration(),
                    sections).asElement();

            sections = Collections.singleton(HomepageSection.create(places, resources,
                    Ids.HOMEPAGE_RUNTIME_SECTION, org.jboss.hal.meta.token.NameTokens.RUNTIME,
                    resources.constants().homepageRuntimeStandaloneSection(),
                    resources.constants().homepageRuntimeStepIntro(),
                    Arrays.asList(
                            resources.constants().homepageRuntimeStandaloneStep1(),
                            resources.constants().homepageRuntimeStandaloneStep2()), true));
            runtime = HomepageModule.create(places,
                    org.jboss.hal.meta.token.NameTokens.RUNTIME,
                    Names.RUNTIME,
                    resources.constants().homepageRuntimeStandaloneSubHeader(),
                    resources.images().runtime(),
                    sections).asElement();

        } else {
            sections = Collections.singleton(HomepageSection.create(places, resources,
                    Ids.HOMEPAGE_DEPLOYMENTS_SECTION, org.jboss.hal.meta.token.NameTokens.DEPLOYMENTS,
                    resources.constants().homepageDeploymentsSection(),
                    resources.constants().homepageDeploymentsDomainStepIntro(),
                    Arrays.asList(
                            resources.constants().homepageDeploymentsDomainStep1(),
                            resources.constants().homepageDeploymentsDomainStep2(),
                            resources.constants().homepageDeploymentsStepEnable()), true));
            deployments = HomepageModule.create(places,
                    org.jboss.hal.meta.token.NameTokens.DEPLOYMENTS,
                    Names.DEPLOYMENTS,
                    resources.constants().homepageDeploymentsSubHeader(),
                    resources.images().deployments(),
                    sections).asElement();

            sections = Collections.singleton(HomepageSection.create(places, resources,
                    Ids.HOMEPAGE_CONFIGURATION_SECTION, org.jboss.hal.meta.token.NameTokens.CONFIGURATION,
                    resources.constants().homepageConfigurationSection(),
                    resources.constants().homepageConfigurationStepIntro(),
                    Arrays.asList(
                            resources.constants().homepageConfigurationDomainStep1(),
                            resources.constants().homepageConfigurationStep2(),
                            resources.constants().homepageConfigurationStep3()), true));
            configuration = HomepageModule.create(places,
                    org.jboss.hal.meta.token.NameTokens.CONFIGURATION,
                    Names.CONFIGURATION,
                    resources.constants().homepageConfigurationDomainSubHeader(),
                    resources.images().configuration(),
                    sections).asElement();

            sections = Arrays.asList(
                    HomepageSection.create(places, resources,
                            Ids.HOMEPAGE_RUNTIME_SERVER_GROUP_SECTION, org.jboss.hal.meta.token.NameTokens.RUNTIME,
                            resources.constants().homepageRuntimeDomainServerGroupSection(),
                            resources.constants().homepageRuntimeDomainServerGroupStepIntro(),
                            Arrays.asList(
                                    resources.constants().homepageRuntimeDomainServerGroupStep1(),
                                    resources.constants().homepageRuntimeDomainServerGroupStep2()), true),
                    HomepageSection.create(places, resources,
                            Ids.HOMEPAGE_RUNTIME_SERVER_SECTION, org.jboss.hal.meta.token.NameTokens.RUNTIME,
                            resources.constants().homepageRuntimeDomainCreateServerSection(),
                            resources.constants().homepageRuntimeDomainCreateServerStepIntro(),
                            Arrays.asList(
                                    resources.constants().homepageRuntimeDomainCreateServerStep1(),
                                    resources.constants().homepageRuntimeDomainCreateServerStep2()), true),
                    HomepageSection.create(places, resources,
                            Ids.HOMEPAGE_RUNTIME_MONITOR_SECTION, org.jboss.hal.meta.token.NameTokens.RUNTIME,
                            resources.constants().homepageRuntimeDomainMonitorServerSection(),
                            resources.constants().homepageRuntimeStepIntro(),
                            Arrays.asList(
                                    resources.constants().homepageRuntimeDomainMonitorServerStep1(),
                                    resources.constants().homepageRuntimeDomainMonitorServerStep2()), true));
            runtime = HomepageModule.create(places,
                    org.jboss.hal.meta.token.NameTokens.RUNTIME,
                    Names.RUNTIME,
                    resources.constants().homepageRuntimeDomainSubHeader(),
                    resources.images().runtime(),
                    sections).asElement();
        }

        if (su) {
            if (ssoEnabled) {
                accessControl = HomepageModule.create(places, NameTokens.ACCESS_CONTROL_SSO,
                        Names.ACCESS_CONTROL, //NON-NLS
                        resources.constants().homepageAccessControlSsoSubHeader(),
                        resources.images().accessControl(), Collections.emptyList()).asElement();
            } else {
                sections = Collections.singleton(HomepageSection.create(places, resources,
                        Ids.HOMEPAGE_ACCESS_CONTROL_SECTION, NameTokens.ACCESS_CONTROL,
                        resources.constants().homepageAccessControlSection(),
                        resources.constants().homepageAccessControlStepIntro(),
                        Arrays.asList(
                                resources.constants().homepageAccessControlStep1(),
                                resources.constants().homepageAccessControlStep2()), true));
                accessControl = HomepageModule.create(places, NameTokens.ACCESS_CONTROL,
                        Names.ACCESS_CONTROL, //NON-NLS
                        resources.constants().homepageAccessControlSubHeader(),
                        resources.images().accessControl(),
                        sections).asElement();
            }


            if (standalone) {
                sections = Collections.singleton(HomepageSection.create(places, resources,
                        Ids.HOMEPAGE_PATCHING_SECTION, org.jboss.hal.meta.token.NameTokens.PATCHING,
                        resources.constants().homepagePatchingSection(),
                        resources.messages().homepagePatchingStandaloneStepIntro(name),
                        Arrays.asList(
                                resources.constants().homepagePatchingStep1(),
                                resources.constants().homepagePatchingStepApply()), true));
            } else {
                sections = Collections.singleton(HomepageSection.create(places, resources,
                        Ids.HOMEPAGE_PATCHING_SECTION, org.jboss.hal.meta.token.NameTokens.PATCHING,
                        resources.constants().homepagePatchingSection(),
                        resources.messages().homepagePatchingDomainStepIntro(name),
                        Arrays.asList(
                                resources.constants().homepagePatchingStep1(),
                                resources.constants().homepagePatchingDomainStep2(),
                                resources.constants().homepagePatchingStepApply()), true));
            }
            patching = HomepageModule.create(places,
                    org.jboss.hal.meta.token.NameTokens.PATCHING,
                    "Patching", //NON-NLS
                    resources.messages().homepagePatchingSubHeader(name),
                    resources.images().patching(),
                    sections).asElement();
        }

        help = HomepageHelp.create(environment, resources).asElement();

        HTMLElement root = div()
                .add(div().css(eapHomeRow)
                        .add(header)
                        .add(deployments)
                        .add(configuration))
                .asElement();
        if (su) {
            root.appendChild(div().css(eapHomeRow)
                    .add(runtime)
                    .add(accessControl)
                    .asElement());
            root.appendChild(div().css(eapHomeRow)
                    .add(patching)
                    .add(help)
                    .asElement());
        } else {
            root.appendChild(div().css(eapHomeRow)
                    .add(runtime)
                    .add(help)
                    .asElement());
        }
        initElement(root);
    }

    @Override
    public void setPresenter(final HomepagePresenter presenter) {
        this.presenter = presenter;
    }
}
