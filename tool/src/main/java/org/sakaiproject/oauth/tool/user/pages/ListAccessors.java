package org.sakaiproject.oauth.tool.user.pages;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.oauth.domain.Accessor;
import org.sakaiproject.oauth.domain.Consumer;
import org.sakaiproject.oauth.exception.InvalidConsumerException;
import org.sakaiproject.oauth.service.OAuthService;
import org.sakaiproject.oauth.tool.pages.SakaiPage;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Colin Hebert
 */
public class ListAccessors extends SakaiPage {
    @SpringBean
    private SessionManager sessionManager;
    @SpringBean
    private OAuthService oAuthService;

    public ListAccessors() {
        String userId = sessionManager.getCurrentSessionUserId();
        Collection<Accessor> accessors = oAuthService.getAccessAccessorForUser(userId);
        ListView<Accessor> accessorList = new ListView<Accessor>("accessorlist", new ArrayList<Accessor>(accessors)) {
            @Override
            protected void populateItem(ListItem<Accessor> components) {
                try {
                    final Consumer consumer = oAuthService.getConsumer(components.getModelObject().getConsumerId());
                    ExternalLink consumerHomepage = new ExternalLink("consumerUrl", consumer.getUrl(),
                            consumer.getName());
                    consumerHomepage.add(new SimpleAttributeModifier("target", "_blank"));
                    consumerHomepage.setEnabled(consumer.getUrl() != null && !consumer.getUrl().isEmpty());
                    components.add(consumerHomepage);
                    components.add(new Label("consumerDescription", consumer.getDescription()));
                    components.add(new Label("creationDate", new StringResourceModel("creation.date", null,
                            new Object[]{components.getModelObject().getCreationDate()})));
                    components.add(new Label("expirationDate", new StringResourceModel("expiration.date", null,
                            new Object[]{components.getModelObject().getExpirationDate()})));

                    components.add(new Link<Accessor>("delete", components.getModel()) {
                        @Override
                        public void onClick() {
                            try {
                                oAuthService.revokeAccessor(getModelObject().getToken());
                                setResponsePage(getPage().getClass());
                                getSession().info(consumer.getName() + "' token has been removed.");
                            } catch (Exception e) {
                                warn("Couldn't remove " + consumer.getName() + "'s token': " + e.getLocalizedMessage());
                            }
                        }
                    });
                } catch (InvalidConsumerException invalidConsumerException) {
                    // Invalid consumer, it is probably deleted
                    // For security reasons, this token should be revoked
                    oAuthService.revokeAccessor(components.getModelObject().getToken());
                    components.setVisible(false);
                }
            }

            @Override
            public boolean isVisible() {
                return !getModelObject().isEmpty() && super.isVisible();
            }
        };
        add(accessorList);

        Label noAccessorLabel = new Label("noAccessor", new ResourceModel("no.accessor"));
        noAccessorLabel.setVisible(!accessorList.isVisible());
        add(noAccessorLabel);
    }
}