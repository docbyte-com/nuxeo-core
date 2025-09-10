/*
 * (C) Copyright 2010-2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupItemDescriptor;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The Class SuggestionServiceImpl.
 */
public class SuggestionServiceImpl extends DefaultComponent implements SuggestionService {

    private static final Logger log = LogManager.getLogger(SuggestionServiceImpl.class);

    protected static final String XP_SUGGESTERS = "suggesters";

    protected static final String XP_SUGGESTER_GROUPS = "suggesterGroups";

    protected Map<String, Suggester> suggesters;

    protected Map<String, SuggesterGroupDescriptor> suggesterGroups;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException {
        List<Suggestion> suggestions = new ArrayList<>();
        SuggesterGroupDescriptor suggesterGroup = suggesterGroups.get(context.suggesterGroup);
        if (suggesterGroup == null) {
            log.warn("No registered SuggesterGroup with id: {}", context.suggesterGroup);
            return suggestions;
        }

        for (SuggesterGroupItemDescriptor suggesterGroupItem : suggesterGroup.getSuggesters()) {
            String suggesterId = suggesterGroupItem.getName();
            Suggester suggester = suggesters.get(suggesterId);
            if (suggester == null) {
                log.warn("No suggester registered with id: {}", suggesterId);
                continue;
            }
            suggestions.addAll(suggester.suggest(userInput, context));
        }
        return suggestions;
    }

    @Override
    public List<Suggestion> suggest(String input, SuggestionContext context, String suggesterName)
            throws SuggestionException {
        Suggester suggester = suggesters.get(suggesterName);
        if (suggester == null) {
            throw new SuggestionException(String.format("No suggester registered under the name '%s'.", suggesterName));
        }
        return suggester.suggest(input, context);
    }

    // Nuxeo Runtime Component API

    @Override
    public void start(ComponentContext context) {
        suggesters = this.<SuggesterDescriptor> getDescriptors(XP_SUGGESTERS)
                         .stream()
                         .filter(SuggesterDescriptor::isEnabled)
                         .<SuggesterWithName> mapMulti((descriptor, consumer) -> {
                             try {
                                 var suggester = descriptor.instantiateSuggester();
                                 consumer.accept(new SuggesterWithName(descriptor.getName(), suggester));
                             } catch (Exception e) {
                                 ComponentName compName = descriptor.getContributingComponent().getName();
                                 String msg = "Failed to register extension to: "
                                         + "org.nuxeo.ecm.platform.suggestbox.service.SuggestionService, xpoint: suggesters "
                                         + "in component: " + compName;
                                 log.error(msg, e);
                                 Framework.getRuntime()
                                          .getMessageHandler()
                                          .addMessage(new RuntimeMessage(Level.ERROR, msg + " (" + e + ')',
                                                  Source.EXTENSION, compName.getName()));
                             }
                         })
                         .collect(Collectors.toMap(SuggesterWithName::name, SuggesterWithName::suggester));
        suggesterGroups = this.<SuggesterGroupDescriptor> getDescriptors(XP_SUGGESTER_GROUPS)
                              .stream()
                              .collect(Collectors.toMap(SuggesterGroupDescriptor::getName, Function.identity()));
    }

    @Override
    public void stop(ComponentContext context) {
        suggesters = null;
        suggesterGroups = null;
    }

    /**
     * Gets the suggester groups registry. Only for test purpose.
     *
     * @return the suggester groups
     */
    public Map<String, SuggesterGroupDescriptor> getSuggesterGroups() {
        return suggesterGroups;
    }

    record SuggesterWithName(String name, Suggester suggester) {
    }
}
