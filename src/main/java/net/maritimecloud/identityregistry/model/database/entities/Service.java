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
package net.maritimecloud.identityregistry.model.database.entities;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import net.maritimecloud.identityregistry.model.database.Certificate;
import net.maritimecloud.identityregistry.validators.InPredefinedList;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

/**
 * Model object representing a service
 */

@Entity
@Table(name = "services")
public class Service extends NonHumanEntityModel {

    public Service() {
    }

    @ApiModelProperty(value = "Access type of the OpenId Connect client", allowableValues = "public, bearer-only, confidential")
    @Column(name = "oidc_access_type")
    @InPredefinedList(acceptedValues = {"public", "bearer-only", "confidential"})
    private String oidcAccessType;

    @ApiModelProperty(value = "The client id of the service in Maritime Cloud. Will be generated.", readOnly = true)
    @Column(name = "oidc_client_id")
    private String oidcClientId;

    @ApiModelProperty(value = "The client secret of the service in Maritime Cloud. Will be generated.", readOnly = true)
    @Column(name = "oidc_client_secret")
    private String oidcClientSecret;

    @ApiModelProperty(value = "The OpenId Connect redirect uri of service.")
    @Column(name = "oidc_redirect_uri")
    @URL
    private String oidcRedirectUri;

    @ApiModelProperty(value = "The domain name the service will be available on. Used in the issued certificates for the service.")
    @Column(name = "cert_domain_name")
    private String certDomainName;

    @ApiModelProperty(value = "Cannot be created/updated by editing in the model. Use the dedicate create and revoke calls.")
    @OneToMany(mappedBy = "service")
    //@Where(clause="UTC_TIMESTAMP() BETWEEN start AND end")
    private List<Certificate> certificates;

    /** Copies this service into the other */
    public Service copyTo(EntityModel target) {
        Service service = (Service) super.copyTo(target);
        service.setOidcAccessType(oidcAccessType);
        service.setOidcClientId(oidcClientId);
        service.setOidcClientSecret(oidcClientSecret);
        service.setOidcRedirectUri(oidcRedirectUri);
        service.setCertDomainName(certDomainName);
        service.getCertificates().clear();
        service.getCertificates().addAll(certificates);
        service.setChildIds();
        return service;
    }

    /** Copies this service into the other
     * Only update things that are allowed to change on update */
    public Service selectiveCopyTo(EntityModel target) {
        Service service = (Service) super.selectiveCopyTo(target);
        service.setOidcAccessType(oidcAccessType);
        service.setOidcRedirectUri(oidcRedirectUri);
        service.setCertDomainName(certDomainName);
        service.setChildIds();
        return service;
    }

    public void assignToCert(Certificate cert){
        cert.setService(this);
    }

    public boolean hasSensitiveFields() {
        return true;
    }

    public void clearSensitiveFields() {
        this.setOidcAccessType(null);
        this.setOidcClientId(null);
        this.setOidcClientSecret(null);
        this.setOidcRedirectUri(null);
    }

    /******************************/
    /** Getters and setters      **/
    /******************************/
    public List<Certificate> getCertificates() {
        return certificates;
    }

    public String getOidcAccessType() {
        return oidcAccessType;
    }

    public void setOidcAccessType(String oidcAccessType) {
        this.oidcAccessType = oidcAccessType;
    }

    public String getOidcClientId() {
        return oidcClientId;
    }

    public void setOidcClientId(String oidcClientId) {
        this.oidcClientId = oidcClientId;
    }

    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    public void setOidcClientSecret(String oidcClientSecret) {
        this.oidcClientSecret = oidcClientSecret;
    }

    public String getOidcRedirectUri() {
        return oidcRedirectUri;
    }

    public void setOidcRedirectUri(String oidcRedirectUri) {
        this.oidcRedirectUri = oidcRedirectUri;
    }

    public String getCertDomainName() {
        return certDomainName;
    }

    public void setCertDomainName(String certDomainName) {
        this.certDomainName = certDomainName;
    }
}

