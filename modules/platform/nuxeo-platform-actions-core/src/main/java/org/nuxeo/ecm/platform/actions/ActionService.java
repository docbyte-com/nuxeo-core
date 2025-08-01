/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.actions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.services.config.ConfigurationService;

import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ActionService extends DefaultComponent implements ActionManager {

    /**
     * @since 2025.0
     */
    public static final String XP_ACTIONS = "actions";

    /**
     * @since 2025.0
     */
    public static final String XP_FILTERS = "filters";

    public static final ComponentName ID = new ComponentName("org.nuxeo.ecm.platform.actions.ActionService");

    private static final long serialVersionUID = -5256555810901945824L;

    private static final Logger log = LogManager.getLogger(ActionService.class);

    private static final String LOG_MIN_DURATION_KEY = "nuxeo.actions.debug.log_min_duration_ms";

    protected final MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    private long logMinDurationNanos = Duration.ofMillis(-1).toNanos();

    private Timer actionsTimer;

    private Timer actionTimer;

    private Timer filtersTimer;

    private Timer filterTimer;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        actionsTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "actions"));
        actionTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "action"));
        filtersTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "filters"));
        filterTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "filter"));
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        actionsTimer = null;
        actionTimer = null;
        filtersTimer = null;
        filterTimer = null;
    }

    @Override
    public void start(ComponentContext context) {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        long logMinDurationMillis = configurationService.getLong(LOG_MIN_DURATION_KEY, -1);
        logMinDurationNanos = Duration.ofMillis(logMinDurationMillis).toNanos();
    }

    private void applyFilters(ActionContext context, List<Action> actions) {
        Iterator<Action> it = actions.iterator();
        while (it.hasNext()) {
            Action action = it.next();
            action.setFiltered(true);
            if (!checkFilters(context, action)) {
                it.remove();
            }
        }
    }

    @Override
    public boolean checkFilters(Action action, ActionContext context) {
        return checkFilters(context, action);
    }

    private boolean checkFilters(ActionContext context, Action action) {
        if (action == null) {
            return false;
        }
        log.trace("Checking access for action '{}'...", action::getId);

        boolean granted = checkFilters(action, action.getFilterIds(), context);
        if (granted) {
            log.trace("Granting access for action '{}'", action::getId);
        } else {
            log.trace("Denying access for action '{}'", action::getId);
        }
        return granted;
    }

    @Override
    public List<Action> getActions(String category, ActionContext context) {
        return getActions(category, context, true);
    }

    @Override
    public List<Action> getAllActions(String category) {
        return this.<Action> getDescriptors(XP_ACTIONS)
                   .stream()
                   .filter(Action::isEnabled)
                   .filter(a -> List.of(a.categories).contains(category))
                   .map(Action::new)
                   .collect(Collectors.toList());
    }

    @SuppressWarnings("resource") // timerContext closed by stop() in finally
    @Override
    public List<Action> getActions(String category, ActionContext context, boolean hideUnavailableActions) {
        final Timer.Context timerContext = actionsTimer.time();
        try {
            List<Action> actions = getAllActions(category);
            if (hideUnavailableActions) {
                applyFilters(context, actions);
                return actions;
            } else {
                List<Action> allActions = new ArrayList<>(actions);
                applyFilters(context, actions);

                for (Action a : allActions) {
                    a.setAvailable(actions.contains(a));
                }
                return allActions;
            }
        } finally {
            long duration = timerContext.stop();
            if (isTimeTracerLogEnabled() && duration > logMinDurationNanos) {
                log.debug("Resolving actions for category '{}' took: {} ms", () -> category,
                        () -> String.format("%.2f", duration / 1000000.0));
            }
        }
    }

    protected boolean isTimeTracerLogEnabled() {
        return log.isDebugEnabled() && logMinDurationNanos >= 0;
    }

    @SuppressWarnings("resource") // timerContext closed by stop() in finally
    @Override
    public Action getAction(String actionId, ActionContext context, boolean hideUnavailableAction) {
        final Timer.Context timerContext = actionTimer.time();
        try {
            Action action = getAction(actionId);
            if (action != null) {
                if (hideUnavailableAction) {
                    if (!checkFilters(context, action)) {
                        return null;
                    }
                } else {
                    if (!checkFilters(context, action)) {
                        action.setAvailable(false);
                    }
                }
                action.setFiltered(true);
            }
            return action;
        } finally {
            long duration = timerContext.stop();
            if (isTimeTracerLogEnabled() && duration > logMinDurationNanos) {
                log.debug("Resolving action with id '{}' took: {} ms", () -> actionId,
                        () -> String.format("%.2f", duration / 1000000.0));
            }
        }
    }

    @Override
    public Action getAction(String actionId) {
        var action = this.<Action> getDescriptor(XP_ACTIONS, actionId);
        return action == null ? null : new Action(action);
    }

    @Override
    public boolean isRegistered(String actionId) {
        return getAction(actionId) != null;
    }

    @Override
    public boolean isEnabled(String actionId, ActionContext context) {
        Action action = getAction(actionId);
        if (action != null) {
            return isEnabled(action, context);
        }
        return false;
    }

    public boolean isEnabled(Action action, ActionContext context) {
        for (String filterId : action.getFilterIds()) {
            ActionFilter filter = getFilter(filterId);
            if (filter != null && !filter.accept(action, context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ActionFilter[] getFilters(String actionId) {
        Action action = getAction(actionId);
        if (action == null) {
            return null;
        }
        List<String> filterIds = action.getFilterIds();
        if (filterIds != null && !filterIds.isEmpty()) {
            ActionFilter[] filters = new ActionFilter[filterIds.size()];
            for (int i = 0; i < filters.length; i++) {
                String filterId = filterIds.get(i);
                filters[i] = getFilter(filterId);
            }
            return filters;
        }
        return null;
    }

    @Override
    public ActionFilter getFilter(String filterId) {
        return this.<DefaultActionFilter> getDescriptor(XP_FILTERS, filterId);
    }

    @SuppressWarnings("resource") // timerContext closed by stop() in finally
    @Override
    public boolean checkFilter(String filterId, ActionContext context) {
        final Timer.Context timerContext = filterTimer.time();
        try {
            ActionFilter filter = getFilter(filterId);
            return filter != null && filter.accept(null, context);
        } finally {
            long duration = timerContext.stop();
            if (isTimeTracerLogEnabled() && duration > logMinDurationNanos) {
                log.debug("Resolving filter with id '{}' took: {} ms", () -> filterId,
                        () -> String.format("%.2f", duration / 1000000.0));
            }
        }
    }

    @Override
    public boolean checkFilters(List<String> filterIds, ActionContext context) {
        return checkFilters(null, filterIds, context);
    }

    @SuppressWarnings("resource") // timerContext closed by stop() in finally
    protected boolean checkFilters(Action action, List<String> filterIds, ActionContext context) {
        if (filterIds == null || filterIds.isEmpty()) {
            return true;
        }
        final Timer.Context timerContext = filtersTimer.time();
        try {
            for (String filterId : filterIds) {
                ActionFilter filter = getFilter(filterId);
                if (filter == null) {
                    continue;
                }
                if (!filter.accept(action, context)) {
                    // denying filter found => ignore following filters
                    log.trace("Filter '{}' denied access", filterId);
                    return false;
                }
                log.trace("Filter '{}' granted access", filterId);
            }
            return true;
        } finally {
            long duration = timerContext.stop();
            if (isTimeTracerLogEnabled() && duration > logMinDurationNanos) {
                log.debug("Resolving filters '{}' took: {} ms", () -> filterIds,
                        () -> String.format("%.2f", duration / 1000000.0));
            }
        }
    }

    @Override
    public void addAction(Action action) {
        register(XP_ACTIONS, action);
    }

    @Override
    protected boolean register(String xp, Descriptor descriptor) {
        if (XP_ACTIONS.equals(xp)) {
            for (ActionFilter f : ((Action) descriptor).getFilters()) {
                register(XP_FILTERS, f);
            }
        }
        return super.register(xp, descriptor);
    }

    @Override
    public Action removeAction(String actionId) {
        var removing = this.<Action> getDescriptor(XP_ACTIONS, actionId);
        if (removing.isEnabled()) {
            removing.setEnabled(false);
            register(XP_ACTIONS, removing);
        }
        return removing;
    }

    @Override
    public void remove() {
        // do nothing
    }
}
