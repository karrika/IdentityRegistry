/* Copyright 2016 Danish Maritime Authority.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.maritimecloud.identityregistry.utils;

import net.maritimecloud.identityregistry.model.database.IdentityProviderAttribute;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class KeycloakAdminUtil {
    // Load the info needed to log into the Keycloak instance that is used as ID Broker (hosts ID Providers) 
    @Value("${net.maritimecloud.idreg.keycloak-broker-admin-user}")
    private String keycloakBrokerAdminUser;
    @Value("${net.maritimecloud.idreg.keycloak-broker-admin-password}")
    private String keycloakBrokerAdminPassword;
    @Value("${net.maritimecloud.idreg.keycloak-broker-admin-client}")
    private String keycloakBrokerAdminClient;
    @Value("${net.maritimecloud.idreg.keycloak-broker-realm}")
    private String keycloakBrokerRealm;
    @Value("${net.maritimecloud.idreg.keycloak-broker-base-url}")
    private String keycloakBrokerBaseUrl;

    // Load the info needed to log into the Keycloak instance that is used as to host project users
    @Value("${net.maritimecloud.idreg.keycloak-project-users-admin-user}")
    private String keycloakProjectUsersAdminUser;
    @Value("${net.maritimecloud.idreg.keycloak-project-users-admin-password}")
    private String keycloakProjectUsersAdminPassword;
    @Value("${net.maritimecloud.idreg.keycloak-project-users-admin-client}")
    private String keycloakProjectUsersAdminClient;
    @Value("${net.maritimecloud.idreg.keycloak-project-users-realm}")
    private String keycloakProjectUsersRealm;
    @Value("${net.maritimecloud.idreg.keycloak-project-users-base-url}")
    private String keycloakProjectUsersBaseUrl;

    // Load client template name used when creating clients in keycloak
    @Value("${net.maritimecloud.idreg.keycloak-client-template}")
    private String keycloakClientTemplate;

    // Type of user
    public static final int NORMAL_USER = 0;
    public static final int ADMIN_USER = 1;
    
    // Type of instance 
    public static final int BROKER_INSTANCE = 0;
    public static final int USER_INSTANCE = 1;

    private Keycloak keycloakBrokerInstance = null;
    private Keycloak keycloakUserInstance = null;

    // Used in createIdpMapper
    private static final Map<String, String> oidcDefaultMappers = new HashMap<String, String>();
    static {
        oidcDefaultMappers.put("firstNameAttr", null);
        oidcDefaultMappers.put("lastNameAttr", null);
        oidcDefaultMappers.put("emailAttr", null);
        oidcDefaultMappers.put("permissionsAttr", "permissions");
    }

    private static final Map<String, String> samlDefaultMappers = new HashMap<String, String>();
    static {
        samlDefaultMappers.put("firstNameAttr", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");
        samlDefaultMappers.put("lastNameAttr", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");
        samlDefaultMappers.put("emailAttr", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        samlDefaultMappers.put("permissionsAttr", "http://schemas.microsoft.com/ws/2008/06/identity/claims/role");
    }

    private static final Map<String, String> attrNames2Keycloak = new HashMap<String, String>();
    static {
        attrNames2Keycloak.put("firstNameAttr", "firstName");
        attrNames2Keycloak.put("lastNameAttr", "lastName");
        attrNames2Keycloak.put("emailAttr", "email");
        attrNames2Keycloak.put("permissionsAttr", "permissions");
    }


    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminUtil.class);

    /**
     * Constructor.
     */
    public KeycloakAdminUtil() {
    }

    /**
     * Init the keycloak instance. Will only initialize the instance defined by the type
     * 
     * @param type  The type of instance to initialize.
     */
    public void init(int type) {
        //keycloakInstance = Keycloak.getInstance(deployment.getAuthServerBaseUrl(), deployment.getRealm(), "idreg-admin", "idreg-admin", "mcidreg", "1b1f1686-1391-4b25-b770-906a2ffc7db9");
        //keycloakInstance = Keycloak.getInstance(keycloakBaseUrl, keycloakRealm, "idreg-admin", "idreg-admin", "security-admin-console");
        if (type == BROKER_INSTANCE) {
            keycloakBrokerInstance = Keycloak.getInstance(keycloakBrokerBaseUrl, keycloakBrokerRealm, keycloakBrokerAdminUser, keycloakBrokerAdminPassword, keycloakBrokerAdminClient);
        } else if (type == USER_INSTANCE) {
            keycloakUserInstance = Keycloak.getInstance(keycloakProjectUsersBaseUrl, keycloakProjectUsersRealm, keycloakProjectUsersAdminUser, keycloakProjectUsersAdminPassword, keycloakProjectUsersAdminClient);
        }
    }
    
    private RealmResource getBrokerRealm() {
        return keycloakBrokerInstance.realm(keycloakBrokerRealm);
    }

    private RealmResource getProjectUserRealm() {
        return keycloakUserInstance.realm(keycloakProjectUsersRealm);
    }

    /**
     * Get IDP info by parsing info from wellKnownUrl json
     * 
     * @param infoUrl The url to parse
     * @param providerId The provider type, can be "keycloak-oidc","oidc" or "saml"
     * @return  The IDP
     * @throws IOException
     */
    private Map<String, String> getIdpSetupUrl(String infoUrl, String providerId) throws IOException {
        // Get IDP info by using keycloaks builtin parser
        Map<String, Object> importFromUrl = new HashMap<String, Object>();
        importFromUrl.put("fromUrl", infoUrl);
        importFromUrl.put("providerId", providerId); // providerId can be either "keycloak-oidc", "oidc" or "saml"
        Map<String, String> importConf = getBrokerRealm().identityProviders().importFrom(importFromUrl);
        // Do some checks to validate the returned
        if (importConf == null || importConf.isEmpty()) {
            throw new IOException("Could not find needed information using the provided URL!");
        }
        return importConf;
    }

    private Map<String, String> idpAttributes2Map(List<IdentityProviderAttribute> input) {
        logger.debug("In idpAttributes2Map, number of attrs: " + input.size());
        Map<String, String> ret = new HashMap<>();
        for (IdentityProviderAttribute atr : input) {
            ret.put(atr.getAttributeName(), atr.getAttributeValue());
            logger.debug("idp attr name: " + atr.getAttributeName()+ ", value: " + atr.getAttributeValue());
        }
        return ret;
    }

    /**
     * Creates or updates an IDP.
     * 
     * @param orgMrn        mrn of the IDP
     * @param input         map containing data about the IDP
     * @throws IOException
     */
    public void createIdentityProvider(String orgMrn, List<IdentityProviderAttribute> input) throws IOException {
        String name = MrnUtil.getOrgShortNameFromOrgMrn(orgMrn);
        Map<String, String> idpAtrMap = idpAttributes2Map(input);
        // Check for valid input
        String providerType = idpAtrMap.get("providerType");
        if (providerType == null || providerType.isEmpty()) {
            throw new IllegalArgumentException("Missing providerType");
        }
        if (!"oidc".equals(providerType) && !"saml".equals(providerType)) {
            throw new IllegalArgumentException("Illegal providerType, must be \"oidc\" or \"saml\"");
        }
        // Get data from URL if supplied
        Map<String, String> importConf;
        if (idpAtrMap.containsKey("importUrl")) {
            importConf = getIdpSetupUrl(idpAtrMap.get("importUrl"), idpAtrMap.get("providerType"));
        } else {
            importConf = new HashMap<String, String>(idpAtrMap);
            importConf.remove("providerType");
        }
        if ("oidc".equals(providerType)) {
            // Check for valid input
            String clientId = idpAtrMap.get("clientId");
            String clientSecret = idpAtrMap.get("clientSecret");
            if (clientId == null || clientId.isEmpty()) {
                throw new IllegalArgumentException("Missing clientId");
            }
            if (clientSecret == null || clientSecret.isEmpty()) {
                throw new IllegalArgumentException("Missing clientSecret");
            }

            importConf.put("clientId", clientId);
            importConf.put("clientSecret", clientSecret);
        }
        // Insert data into IDP data structure
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(name);
        idp.setEnabled(true);
        idp.setProviderId(providerType); // can be "keycloak-oidc","oidc" or "saml"
        idp.setTrustEmail(true);
        idp.setStoreToken(false);
        idp.setAddReadTokenRoleOnCreate(false);
        idp.setFirstBrokerLoginFlowAlias("Auto first broker login");
        idp.setConfig(importConf);

        // Check if the IDP already exists
        IdentityProviderResource oldIdpRes = getBrokerRealm().identityProviders().get(name);
        IdentityProviderRepresentation oldIdp = null;
        try {
            oldIdp = oldIdpRes.toRepresentation();
        } catch(NotFoundException nfe) {
        }
        if (oldIdp != null) {
            getBrokerRealm().identityProviders().get(name).update(idp);
        } else {
            Response ret = getBrokerRealm().identityProviders().create(idp);
            logger.debug("Returned status from creating IDP: " + ret.getStatus());
            if (ret.getStatus() != 201) {
                throw new IOException("Could not create IDP");
            }
        }

        // Create the mappers for the IDP
        createIdpMappers(name, idpAtrMap, orgMrn);
    }

    private void createIdpMappers(String idpName, Map<String, String> idpAtrMap, String orgMrn) {

        String providerType = idpAtrMap.get("providerType");
        IdentityProviderResource newIdpRes = getBrokerRealm().identityProviders().get(idpName);
        // Delete any existing mapper
        for (IdentityProviderMapperRepresentation mapper : newIdpRes.getMappers()) {
            newIdpRes.delete(mapper.getId());
        }
        // Create mapper for hardcoded org value
        String orgMapperName = "org mapper";
        IdentityProviderMapperRepresentation orgMapper = new IdentityProviderMapperRepresentation();
        orgMapper.setIdentityProviderAlias(idpName);
        orgMapper.setIdentityProviderMapper("hardcoded-attribute-idp-mapper");
        orgMapper.setName(orgMapperName);
        Map<String, String> orgMapperConf = new HashMap<String, String>();
        orgMapperConf.put("attribute.value", orgMrn);
        orgMapperConf.put("attribute", "org");
        orgMapper.setConfig(orgMapperConf);
        newIdpRes.addMapper(orgMapper);

        // Create username mapper
        String usernameMapperName = "username mapper";
        IdentityProviderMapperRepresentation usernameMapper = new IdentityProviderMapperRepresentation();
        usernameMapper.setIdentityProviderAlias(idpName);
        usernameMapper.setName(usernameMapperName);
        Map<String, String> usernameMapperConf = new HashMap<String, String>();
        if ("oidc".equals(providerType)) {
            // Create OIDC specific mapper
            usernameMapper.setIdentityProviderMapper("oidc-username-idp-mapper");
            // Import username to an mrn in the form: urn:mrn:mcl:user:<org-id>:<user-id>
            usernameMapperConf.put("template", "urn:mrn:mcl:user:${ALIAS}:${CLAIM." + idpAtrMap.getOrDefault("usernameAttr", "preferred_username") + "}");
        } else {
            usernameMapper.setIdentityProviderMapper("saml-username-idp-mapper");
            // Import username to an mrn in the form: urn:mrn:mcl:user:<org-id>:<user-id>
            usernameMapperConf.put("template", "urn:mrn:mcl:user:${ALIAS}:${" + idpAtrMap.getOrDefault("usernameAttr", "NAMEID") + "}");
        }
        usernameMapper.setConfig(usernameMapperConf);
        newIdpRes.addMapper(usernameMapper);

        // Add other mappers as needed
        // The mappers are set up differently based on the provider type
        Map<String, String> defaultMappers = null;
        String mapperConfKey;
        if ("oidc".equals(providerType)) {
            defaultMappers = oidcDefaultMappers;
            mapperConfKey = "claim";
        } else {
            defaultMappers = samlDefaultMappers;
            mapperConfKey = "attribute.name";
        }
        String mapperType = providerType + "-user-attribute-idp-mapper";
        for (Map.Entry<String, String> entry: defaultMappers.entrySet()) {
            String attrName = attrNames2Keycloak.get(entry.getKey());
            String attrValue = idpAtrMap.getOrDefault(entry.getKey(), entry.getValue());
            // Skip creating this mapper if no value is defined
            if (attrValue == null) {
                continue;
            }
            String attrMapperName = attrName + " mapper";
            IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
            mapper.setIdentityProviderAlias(idpName);
            mapper.setIdentityProviderMapper(mapperType);
            mapper.setName(attrMapperName);
            Map<String, String> mapperConf = new HashMap<String, String>();
            mapperConf.put(mapperConfKey, attrValue);
            mapperConf.put("user.attribute", attrName);
            mapper.setConfig(mapperConf);
            newIdpRes.addMapper(mapper);
        }
    }

    /**
     * Delete Identity Provider with the given alias
     * 
     * @param orgMrn  Alias of the IDP to delete.
     */
    public void deleteIdentityProvider(String orgMrn) {
        String alias = MrnUtil.getOrgShortNameFromOrgMrn(orgMrn);
        // First delete any users associated with the IDP
        List<UserRepresentation> users = getBrokerRealm().users().search(/* username*/ orgMrn + ":user:", /* firstName */ null, /* lastName */ null, /* email */ null,  /* first */ 0, /* max*/ 0);
        for (UserRepresentation user : users) {
            if (user.getUsername().startsWith(orgMrn + ":user:")) {
                getBrokerRealm().users().get(user.getId()).remove();
            }
        }
        // Delete IDP
        getBrokerRealm().identityProviders().get(alias).remove();
    }

    /**
     * Creates a user in keycloak.
     * 
     * @param userMrn       MRN of the user
     * @param firstName     first name of user
     * @param lastName      last name of user
     * @param password      password of the user
     * @param email         email of the user
     * @param orgMrn        MRN of the org
     * @throws IOException
     */
    public void createUser(String userMrn, String password, String firstName, String lastName, String email, String orgMrn, String permissions, boolean enabled) throws IOException {
        logger.debug("creating user: " + userMrn);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEnabled(enabled);
        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email);
            user.setEmailVerified(true);
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setLastName(lastName);
        }
        // Set attributes
        Map<String, List<String>> attr = new HashMap<String,List<String>>();
        attr.put("org", Arrays.asList(orgMrn));
        attr.put("mrn", Arrays.asList(userMrn));
        attr.put("permissions",  Arrays.asList(permissions));
        user.setAttributes(attr);
        Response ret = getProjectUserRealm().users().create(user);
        String errMsg = ret.readEntity(String.class);
        if (ret.getStatus() != 201) {
            logger.debug("creating user failed, status: " + ret.getStatus() + ", " + errMsg);
            throw new IOException("User creation failed: " + errMsg);
        }
        logger.debug("created user, status: " + ret.getStatus() + ", " + errMsg);
        ret.close();
        
        // Set credentials
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        // Make sure the user updates the password on first login
        cred.setTemporary(true);
        // Find the user by searching for the username
        user = getProjectUserRealm().users().search(email, null, null, null, -1, -1).get(0);
        user.setCredentials(Arrays.asList(cred));
        logger.debug("setting password for user: " + user.getId());
        getProjectUserRealm().users().get(user.getId()).resetPassword(cred);
        logger.debug("created user");
    }


    /**
     * Updates the user in keycloak
     * 
     * @param userMrn       MRN of the user
     * @param firstName     first name of user
     * @param lastName      last name of user
     * @param email         email of the user
     * @throws IOException 
     */
    public void updateUser(String userMrn,  String firstName, String lastName, String email, String newPermissions, boolean enabled) throws IOException {
        List<UserRepresentation> userReps = getProjectUserRealm().users().search(email, null, null, null, -1, -1);
        if (userReps.size() != 1) {
            logger.debug("Skipping user update! Found " + userReps.size() + " users while trying to update, expected 1");
            throw new IOException("User update failed! Found " + userReps.size() + " users while trying to update, expected 1");
        }
        UserRepresentation user = userReps.get(0);
        boolean updated = false;
        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email);
            user.setEmailVerified(true);
            updated = true;
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName);
            updated = true;
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setLastName(lastName);
            updated = true;
        }
        Map<String, List<String>> attr = user.getAttributes();
        if (attr.containsKey("permissions")) {
            List<String> oldPermissions = (List<String>) attr.get("permissions");
            if (oldPermissions != null && !oldPermissions.isEmpty()) {
                String permission = oldPermissions.get(0);
                if (permission == null || !permission.equals(newPermissions)) {
                    attr.put("permissions", Arrays.asList(newPermissions));
                    user.setAttributes(attr);
                    updated = true;
                }
            }
        } else {
            if (newPermissions != null) {
                attr.put("permissions", Arrays.asList(newPermissions));
                user.setAttributes(attr);
                updated = true;
            }
        }
        if (attr.containsKey("mrn")) {
            List<String> oldMrn = (List<String>) attr.get("mrn");
            if (oldMrn != null && !oldMrn.isEmpty()) {
                String mrn = oldMrn.get(0);
                if (mrn == null || !mrn.equals(userMrn)) {
                    attr.put("mrn", Arrays.asList(userMrn));
                    user.setAttributes(attr);
                    updated = true;
                }
            }
        } else {
            attr.put("mrn", Arrays.asList(userMrn));
            user.setAttributes(attr);
            updated = true;
        }
        if (updated) {
            getProjectUserRealm().users().get(user.getId()).update(user);
        }
    }

    /**
     * Delete a user from Keycloak
     * 
     * @param email  email of the user to delete
     */
    public void deleteUser(String email) {
        // Find the user by searching for the username
        List<UserRepresentation> users = getProjectUserRealm().users().search(email, null, null, null, -1, -1);
        // If we found one, delete it
        if (!users.isEmpty()) {
            getProjectUserRealm().users().get(users.get(0).getId()).remove();
        }
    }

    /**
     * Creates an OpenId Connect client in keycloak
     *
     * @param clientId       The client id
     * @param type           The client type, can be public, bearer-only or confidential
     * @param redirectUri    The redirect uri
     * @return               Returns the generated client secret, unless the type is public, in which case an empty string is returned.
     * @throws IOException
     */
    public String createClient(String clientId, String type, String redirectUri) throws IOException {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setClientAuthenticatorType("client-secret");
        client.setRedirectUris(Arrays.asList(redirectUri));
        client.setWebOrigins(Arrays.asList("+"));
        client.setDirectAccessGrantsEnabled(false);
        client.setProtocol("openid-connect");
        client.setEnabled(true);
        client.setConsentRequired(false);
        client.setClientTemplate(keycloakClientTemplate); // the template includes the mappers needed
        if ("public".equals(type)) {
            client.setBearerOnly(false);
            client.setPublicClient(true);
        } else if ("bearer-only".equals(type)) {
            client.setBearerOnly(true);
            client.setPublicClient(false);
        } else {
            // Fallback to "confidential"
            client.setBearerOnly(false);
            client.setPublicClient(false);
        }
        // Create the client
        getBrokerRealm().clients().create(client);
        if (!"public".equals(type)) {
            // The client secret can't be retrived by the ClientRepresentation (bug?), so we need to use the ClientResource
            ClientRepresentation createdClient = getBrokerRealm().clients().findByClientId(clientId).get(0);
            String secret = getBrokerRealm().clients().get(createdClient.getId()).getSecret().getValue();
            return secret;
        } else {
            return "";
        }
    }

    /**
     * Updates an OpenId Connect client in keycloak
     *
     * @param clientId
     * @param type
     * @param redirectUri
     * @return               Returns the generated client secret, unless the type is public, in which case an empty string is returned.
     */
    public String updateClient(String clientId, String type, String redirectUri) {
        ClientRepresentation client = getBrokerRealm().clients().findByClientId(clientId).get(0);
        client.setClientAuthenticatorType(type);
        client.setRedirectUris(Arrays.asList(redirectUri));
        if ("public".equals(type)) {
            client.setBearerOnly(false);
            client.setPublicClient(true);
        } else if ("bearer-only".equals(type)) {
            client.setBearerOnly(true);
            client.setPublicClient(false);
        } else {
            // Fallback to "confidential"
            client.setBearerOnly(false);
            client.setPublicClient(false);
        }
        getBrokerRealm().clients().get(client.getId()).update(client);
        if (!type.equals("public")) {
            // The client secret can't be retrived by the ClientRepresentation (bug?), so we need to use the ClientResource
            String secret = getBrokerRealm().clients().get(client.getId()).getSecret().getValue();
            return secret;
        } else {
            return "";
        }
    }

    /**
     * Deletes an OpenId Connect client in keycloak
     *
     * @param clientId
     */
    public void deleteClient(String clientId) {
        ClientRepresentation client = getBrokerRealm().clients().findByClientId(clientId).get(0);
        getBrokerRealm().clients().get(client.getId()).remove();
    }

    /**
     * Gets the keycloak.json for this client.
     *
     * @param clientId client id/name
     * @return the keycloak json
     */
    public String getClientKeycloakJson(String clientId) {
        ClientRepresentation client = getBrokerRealm().clients().findByClientId(clientId).get(0);
        String token = keycloakBrokerInstance.tokenManager().getAccessTokenString();
        String url = keycloakBrokerBaseUrl + "admin/realms/" + keycloakBrokerRealm + "/clients/" + client.getId() + "/installation/providers/keycloak-oidc-keycloak-json";
        return getFromKeycloak(url, token);
    }

    /**
     * Gets the keycloak.json for this client.
     *
     * @param clientId client id/name
     * @return the keycloak json
     */
    public String getClientJbossXml(String clientId) {
        ClientRepresentation client = getBrokerRealm().clients().findByClientId(clientId).get(0);
        String token = keycloakBrokerInstance.tokenManager().getAccessTokenString();
        String url = keycloakBrokerBaseUrl + "admin/realms/" + keycloakBrokerRealm + "/clients/" + client.getId() + "/installation/providers/keycloak-oidc-jboss-subsystem";
        return getFromKeycloak(url, token);
    }

    /**
     * Helper function to GET from keycloak api that isn't supported by the client
     *
     * @param url The url to GET
     * @param token The access_token to use for identification
     * @return Returns a string representation of the result
     */
    private String getFromKeycloak(String url, String token) {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            logger.debug("get url: " + url);
            HttpGet get = new HttpGet(url);
            get.addHeader("Authorization", "Bearer " + token);
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.debug("" + response.getStatusLine().getStatusCode());
                    return null;
                }
                String content = getContent(response.getEntity());
                return content;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper function to extract string from HttpEntity
     *
     * @param entity
     * @return
     * @throws IOException
     */
    private static String getContent(HttpEntity entity) throws IOException {
        if (entity == null) return null;
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            String data = new String(bytes);
            return data;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }
}
