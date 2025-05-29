package com.github.pszabo891.keycloak.broker.oidc.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.utils.StringUtil;

public class PrefixedUserAttributeMapperExtension extends AbstractClaimMapper {

    public static final String[] COMPATIBLE_PROVIDERS = { KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String USER_ATTRIBUTE = "user.attribute";
    public static final String ATTRIBUTE_PREFFIX = "attribute.prefix";
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(
            Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        ProviderConfigProperty property2;

        property1 = new ProviderConfigProperty();
        property1.setName(CLAIM);
        property1.setLabel("Claim");
        property1.setHelpText(
                "Name of claim to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);

        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText(
                "User attribute name to store claim.  Use email, lastName, and firstName to map to those predefined user properties.");
        property.setType(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE);
        configProperties.add(property);

        property2 = new ProviderConfigProperty();
        property2.setName(ATTRIBUTE_PREFFIX);
        property2.setLabel("Attribute Value Prefix");
        property2.setHelpText(
                "Prefix to be concatenated in front of every imported attribute value.");
        property2.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property2);
    }

    public static final String PROVIDER_ID = "oidc-prefixed-user-attribute-idp-mapper";

    private static final Logger LOG = Logger.getLogger(PrefixedUserAttributeMapperExtension.class);

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Prefixed Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Prefixed Attribute Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        LOG.debug("executing preprocessFederatedIdentity()");
        
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        String prefix = Objects.toString(mapperModel.getConfig().get(ATTRIBUTE_PREFFIX), "");
        
        LOG.debug("Retrieved prefix: "+prefix);

        if(StringUtil.isNullOrEmpty(attribute)){
            return;
        }
        Object value = getClaimValue(mapperModel, context);
        List<String> values = toPrefixedList(value, prefix);

        if (EMAIL.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(context::setEmail, values);
        } else if (FIRST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(context::setFirstName, values);
        } else if (LAST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(context::setLastName, values);
        } else {
            List<String> valuesToString = values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());

            context.setUserAttribute(attribute, valuesToString);
        }
    }

    private void setIfNotEmpty(Consumer<String> consumer, List<String> values) {
        if (values != null && !values.isEmpty()) {
            consumer.accept(values.get(0));
        }
    }

    private List<String> toPrefixedList(Object value, String prefix) {
        List<Object> values = (value instanceof List)
            ? ((List<?>) value).stream().collect(Collectors.toList())
            : Collections.singletonList(value);

        return values.stream()
            .filter(Objects::nonNull)
            .map(item -> prefix.concat(item.toString()))
            .collect(Collectors.toList());
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        LOG.debug("executing updateBrokeredUser()");
                
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        String prefix = Objects.toString(mapperModel.getConfig().get(ATTRIBUTE_PREFFIX), "");
                
        LOG.debug("Retrieved prefix: "+prefix);
        
        if (StringUtil.isNullOrEmpty(attribute)) {
            return;
        }

        Object value = getClaimValue(mapperModel, context);
        List<String> values = toPrefixedList(value, prefix);

        if (EMAIL.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setEmail, values);
        } else if (FIRST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setFirstName, values);
        } else if (LAST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setLastName, values);
        } else {
            List<String> current = user.getAttributeStream(attribute).collect(Collectors.toList());
            if (!CollectionUtil.collectionEquals(values, current)) {
                user.setAttribute(attribute, values);
            } else if (values.isEmpty()) {
                user.removeAttribute(attribute);
            }
        }
    }

    @Override
    public String getHelpText() {
        return "Import declared claim if it exists in ID, access token or the claim set returned by the user profile endpoint into the specified user property or attribute and prefix each attribute value with the provided prefix.";
    }
}
