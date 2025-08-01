/*
 * (C) Copyright 2007-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ec.notification.service;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.nuxeo.mail.MailServiceImpl.DEFAULT_SENDER;
import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.service.AuditComponent;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerHook;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.ecm.platform.ec.notification.SubscriptionAdapter;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.mail.MailConstants;
import org.nuxeo.mail.MailException;
import org.nuxeo.mail.MailService;
import org.nuxeo.mail.MailServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
public class NotificationService extends DefaultComponent implements NotificationManager {

    private static final Logger log = LogManager.getLogger(NotificationService.class);

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.ec.notification.service.NotificationService");

    protected static final String NOTIFICATIONS_EP = "notifications";

    protected static final String TEMPLATES_EP = "templates";

    protected static final String GENERAL_SETTINGS_EP = "generalSettings";

    protected static final String NOTIFICATION_HOOK_EP = "notificationListenerHook";

    protected static final String NOTIFICATION_VETO_EP = "notificationListenerVeto";

    // FIXME: performance issue when putting URLs in a Map.
    protected Map<String, URL> templateUrls;

    protected EmailHelper emailHelper = new EmailHelper();

    protected GeneralSettingsDescriptor generalSettings;

    protected NotificationRegistry notificationRegistry;

    protected Map<String, NotificationListenerHook> hookListeners;

    protected List<NotificationListenerVeto> notificationVetos;

    protected String senderName;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(NotificationManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        notificationRegistry = new NotificationRegistryImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        notificationRegistry.clear();
        notificationRegistry = null;
    }

    @Override
    @SuppressWarnings("deprecation") // for getMailSessionJndiName & registerJndiSMTPSender
    public void start(ComponentContext context) {
        templateUrls = this.<TemplateDescriptor> getDescriptors(TEMPLATES_EP)
                           .stream()
                           .filter(descriptor -> descriptor.getSrcAsURL() != null)
                           .collect(Collectors.toMap(TemplateDescriptor::getName, TemplateDescriptor::getSrcAsURL));
        generalSettings = ObjectUtils.getIfNull(getDescriptor(GENERAL_SETTINGS_EP, UNIQUE_DESCRIPTOR_ID), () -> {
            // init default settings
            var generalSettings = new GeneralSettingsDescriptor();
            generalSettings.serverPrefix = "http://localhost:8080/nuxeo/";
            generalSettings.eMailSubjectPrefix = "[Nuxeo]";
            generalSettings.mailSessionJndiName = "java:/Mail";
            return generalSettings;
        });
        hookListeners = this.<NotificationListenerHookDescriptor> getDescriptors(NOTIFICATION_HOOK_EP)
                            .stream()
                            .<NotificationListenerHookWithName> mapMulti((descriptor, consumer) -> {
                                try {
                                    consumer.accept(new NotificationListenerHookWithName(descriptor.getName(),
                                            descriptor.getHookListener().getDeclaredConstructor().newInstance()));
                                } catch (ReflectiveOperationException e) {
                                    log.error(e);
                                }
                            })
                            .collect(Collectors.toMap(NotificationListenerHookWithName::name,
                                    NotificationListenerHookWithName::hook));
        notificationVetos = this.<NotificationListenerVetoDescriptor> getDescriptors(NOTIFICATION_VETO_EP)
                                .stream()
                                .<NotificationListenerVeto> mapMulti((descriptor, consumer) -> {
                                    try {
                                        consumer.accept(descriptor.getNotificationVeto()
                                                                  .getDeclaredConstructor()
                                                                  .newInstance());
                                    } catch (ReflectiveOperationException e) {
                                        log.error("Unable to instantiate NotificationListenerVeto with id: {}",
                                                descriptor.getId(), e);
                                    }
                                })
                                .toList();
        senderName = Optional.ofNullable(generalSettings.getMailSenderName()).orElseGet(() -> {
            var jndiSessionName = generalSettings.getMailSessionJndiName();
            if (!jndiSessionName.equals(MailConstants.DEFAULT_MAIL_JNDI_NAME)) {
                log.warn("Your GeneralSettingsDescriptor has been contributed with a custom mailSessionJndiName. "
                        + "This field is now deprecated. Please use mailSenderName and contribute a MailSenderDescriptor");
                return ((MailServiceImpl) Framework.getService(MailService.class)).registerJndiSMTPSender(
                        jndiSessionName);
            } // else keep default
            return DEFAULT_SENDER;
        });
    }

    @Override
    public void stop(ComponentContext context) {
        templateUrls = null;
        generalSettings = null;
        hookListeners = null;
        notificationVetos = null;
        senderName = null;
    }

    @Override
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        log.info("Registering notification extension");
        if (NOTIFICATIONS_EP.equals(xp)) {
            NotificationDescriptor notifDesc = (NotificationDescriptor) contribution;
            notificationRegistry.registerNotification(notifDesc, getNames(notifDesc.getEvents()));
        } else {
            super.registerContribution(contribution, xp, component);
        }
    }

    private static List<String> getNames(List<NotificationEventDescriptor> events) {
        List<String> eventNames = new ArrayList<>();
        for (NotificationEventDescriptor descriptor : events) {
            eventNames.add(descriptor.name);
        }
        return eventNames;
    }

    @Override
    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        if (NOTIFICATIONS_EP.equals(xp)) {
            NotificationDescriptor notifDesc = (NotificationDescriptor) contribution;
            notificationRegistry.unregisterNotification(notifDesc);
        } else {
            super.unregisterContribution(contribution, xp, component);
        }
    }

    @Override
    public URL getTemplateUrl(String name) {
        return templateUrls.get(name);
    }

    @Override
    public List<String> getSubscribers(String notification, DocumentModel doc) {
        return doc.getAdapter(SubscriptionAdapter.class).getNotificationSubscribers(notification);
    }

    @Override
    public List<String> getSubscriptionsForUserOnDocument(String username, DocumentModel doc) {
        return doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
    }

    protected void disableEvents(DocumentModel doc) {
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, TRUE);
        doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, TRUE);
        doc.putContextData(AuditComponent.DISABLE_AUDIT_LOGGER, TRUE);
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, TRUE);
        doc.putContextData(VersioningService.DISABLE_AUTOMATIC_VERSIONING, TRUE);
    }

    protected void restoreEvents(DocumentModel doc) {
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, null);
        doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, null);
        doc.putContextData(AuditComponent.DISABLE_AUDIT_LOGGER, null);
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, null);
        doc.putContextData(VersioningService.DISABLE_AUTOMATIC_VERSIONING, null);
    }

    @Override
    public void addSubscription(String username, String notification, DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName) {

        CoreInstance.doPrivileged(doc.getRepositoryName(), (CoreSession session) -> {
            doc.getAdapter(SubscriptionAdapter.class).addSubscription(username, notification);
            disableEvents(doc);
            session.saveDocument(doc);
            restoreEvents(doc);
        });

        // send event for email if necessary
        if (sendConfirmationEmail) {
            raiseConfirmationEvent(principal, doc, username, notificationName);
        }
    }

    @Override
    public void addSubscriptions(String username, DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal) {
        CoreInstance.doPrivileged(doc.getRepositoryName(), (CoreSession session) -> {
            doc.getAdapter(SubscriptionAdapter.class).addSubscriptionsToAll(username);
            disableEvents(doc);
            session.saveDocument(doc);
            restoreEvents(doc);
        });

        // send event for email if necessary
        if (sendConfirmationEmail) {
            raiseConfirmationEvent(principal, doc, username, "All Notifications");
        }
    }

    @Override
    public void removeSubscriptions(String username, List<String> notifications, DocumentModel doc) {
        CoreInstance.doPrivileged(doc.getRepositoryName(), (CoreSession session) -> {
            SubscriptionAdapter sa = doc.getAdapter(SubscriptionAdapter.class);
            for (String notification : notifications) {
                sa.removeUserNotificationSubscription(username, notification);
            }
            disableEvents(doc);
            session.saveDocument(doc);
            restoreEvents(doc);
        });
    }

    protected EventProducer producer;

    protected void doFireEvent(Event event) {
        if (producer == null) {
            producer = Framework.getService(EventProducer.class);
        }
        producer.fireEvent(event);
    }

    private void raiseConfirmationEvent(NuxeoPrincipal principal, DocumentModel doc, String username,
            String notification) {

        Map<String, Serializable> options = new HashMap<>();

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME, doc.getRepositoryName());

        // options for confirmation email
        options.put("recipients", username);
        options.put("notifName", notification);

        CoreSession session = doc.getCoreSession();
        DocumentEventContext ctx = new DocumentEventContext(session, principal, doc);
        ctx.setCategory(DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY);
        ctx.setProperties(options);
        Event event = ctx.newEvent(DocumentEventTypes.SUBSCRIPTION_ASSIGNED);
        doFireEvent(event);
    }

    @Override
    public void removeSubscription(String username, String notification, DocumentModel doc) {
        removeSubscriptions(username, singletonList(notification), doc);
    }

    /**
     * @deprecated since 2025.0, use {@link NotificationManager#getTemplateUrl(String)} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static URL getTemplateURL(String name) {
        return Framework.getService(NotificationManager.class).getTemplateUrl(name);
    }

    public String getServerUrlPrefix() {
        return generalSettings.getServerPrefix();
    }

    public String getEMailSubjectPrefix() {
        return generalSettings.getEMailSubjectPrefix();
    }

    /**
     * @deprecated since 2023.4 use {@link #getMailSenderName()} instead.
     */
    @Deprecated(since = "2023.4")
    public String getMailSessionJndiName() {
        return generalSettings.getMailSessionJndiName();
    }

    @Override
    public String getMailSenderName() {
        return senderName;
    }

    @Override
    public Notification getNotificationByName(String selectedNotification) {
        List<Notification> listNotif = notificationRegistry.getNotifications();
        for (Notification notification : listNotif) {
            if (notification.getName().equals(selectedNotification)) {
                return notification;
            }
        }
        return null;
    }

    @Override
    public void sendNotification(String notificationName, Map<String, Object> infoMap, String userPrincipal) {

        Notification notif = getNotificationByName(notificationName);

        NuxeoPrincipal recipient = NotificationServiceHelper.getUsersService().getPrincipal(userPrincipal);
        String email = recipient.getEmail();
        String mailTemplate = notif.getTemplate();

        infoMap.put("mail.to", email);

        String authorUsername = (String) infoMap.get("author");

        if (authorUsername != null) {
            NuxeoPrincipal author = NotificationServiceHelper.getUsersService().getPrincipal(authorUsername);
            infoMap.put("principalAuthor", author);
        }

        // mail.put("doc", docMessage); - should be already there

        String subject = notif.getSubject() == null ? "Alert" : notif.getSubject();
        if (notif.getSubjectTemplate() != null) {
            subject = notif.getSubjectTemplate();
        }

        subject = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix() + " " + subject;

        infoMap.put("subject", subject);
        infoMap.put("template", mailTemplate);

        try {
            emailHelper.sendMailMessage(infoMap);
        } catch (MailException e) {
            throw new NuxeoException("Failed to send notification email ", e);
        }
    }

    @Override
    public void sendDocumentByMail(DocumentModel doc, String freemarkerTemplateName, String subject, String comment,
            NuxeoPrincipal sender, List<String> sendTo) {
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("document", doc);
        infoMap.put("subject", subject);
        infoMap.put("comment", comment);
        infoMap.put("sender", sender);

        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        DocumentView docView = new DocumentViewImpl(docLoc);
        docView.setViewId("view_documents");
        infoMap.put("docUrl",
                Framework.getService(DocumentViewCodecManager.class)
                         .getUrlFromDocumentView(docView, true,
                                 NotificationServiceHelper.getNotificationService().getServerUrlPrefix()));

        if (freemarkerTemplateName == null) {
            freemarkerTemplateName = "defaultNotifTemplate";
        }
        infoMap.put("template", freemarkerTemplateName);

        for (String to : sendTo) {
            infoMap.put("mail.to", to);
            try {
                emailHelper.sendMailMessage(infoMap);
            } catch (MailException e) {
                log.debug("Failed to send notification email: {}", e::toString);
            }
        }
    }

    @Override
    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        return notificationRegistry.getNotificationsForSubscriptions(parentType);
    }

    @Override
    public List<Notification> getNotificationsForEvents(String eventId) {
        return notificationRegistry.getNotificationsForEvent(eventId);
    }

    public EmailHelper getEmailHelper() {
        return emailHelper;
    }

    public void setEmailHelper(EmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }

    @Override
    public Set<String> getNotificationEventNames() {
        return notificationRegistry.getNotificationEventNames();
    }

    public Collection<NotificationListenerHook> getListenerHooks() {
        return hookListeners.values();
    }

    public Collection<NotificationListenerVeto> getNotificationVetos() {
        return notificationVetos;
    }

    @Override
    public List<String> getUsersSubscribedToNotificationOnDocument(String notification, DocumentModel doc) {
        return getSubscribers(notification, doc);
    }

    @Override
    public List<DocumentModel> getSubscribedDocuments(String prefixedPrincipalName, String repositoryName) {
        String nxql = "SELECT * FROM Document WHERE ecm:mixinType = '" + SubscriptionAdapter.NOTIFIABLE_FACET + "' "
                + "AND ecm:isVersion = 0 " + "AND notif:notifications/*/subscribers/* = "
                + NXQL.escapeString(prefixedPrincipalName);

        return CoreInstance.doPrivileged(repositoryName,
                (CoreSession s) -> s.query(nxql)
                                    .stream()
                                    .map(NotificationService::detachDocumentModel)
                                    .collect(toList()));
    }

    protected static DocumentModel detachDocumentModel(DocumentModel doc) {
        doc.detach(true);
        return doc;
    }

    record NotificationListenerHookWithName(String name, NotificationListenerHook hook) {
    }
}
