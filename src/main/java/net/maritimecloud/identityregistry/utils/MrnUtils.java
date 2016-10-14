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

import java.util.regex.Pattern;

/**
 * Utility class to create MRNs and extract certain info from MRNs
 */
public class MrnUtils {

    public final static String MC_MRN_PREFIX = "urn:mrn:mcl";
    public final static String MC_MRN_ORG_PREFIX = MC_MRN_PREFIX + ":org";
    public final static Pattern URN_PATTERN = Pattern.compile("^urn:mrn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE);


    public static String getOrgShortNameFromOrgMrn(String orgMrn) {
        int idx = orgMrn.lastIndexOf(":");
        return orgMrn.substring(idx);
    }

    /**
     * Returns the org shortname of the organization responsible for validating the organization that is
     * identified by the given MRN. If MaritimeCloud is the validator "mcl" is returned.
     * @param orgMrn
     * @return
     */
    public static String getOrgValidatorFromOrgMrn(String orgMrn) {
        int idx = orgMrn.lastIndexOf(":");
        if (idx == (MC_MRN_ORG_PREFIX.length() + 1)) {
            return "mcl";
        }
        int endIdx = orgMrn.indexOf(":", MC_MRN_ORG_PREFIX.length() + 1);
        return orgMrn.substring(MC_MRN_ORG_PREFIX.length() + 1, endIdx);
    }

    public static String getOrgShortNameFromEntityMrn(String entityMrn) {
        int endIdx = entityMrn.indexOf(":user:");
        if (endIdx < 0) {
            endIdx = entityMrn.indexOf(":device:");
        }
        if (endIdx < 0) {
            endIdx = entityMrn.indexOf(":vessel:");
        }
        if (endIdx < 0) {
            endIdx = entityMrn.indexOf(":service:");
        }
        int startIdx = entityMrn.lastIndexOf(":", endIdx);
        return entityMrn.substring(startIdx, endIdx);
    }

    public static String getOrgMrnEntityMrn(String entityMrn) {
        int endIdx = entityMrn.indexOf(":user:");
        if (endIdx < 0) {
            endIdx = entityMrn.indexOf(":device:");
        }
        if (endIdx < 0) {
            endIdx = entityMrn.indexOf(":vessel:");
        }
        if (endIdx < 0) {
            endIdx = entityMrn.indexOf(":service:");
        }
        int startIdx = entityMrn.lastIndexOf(":", endIdx);
        return entityMrn.substring(0, endIdx);
    }

    public static String getEntityIdFromMrn(String entityMrn) {
        int idx = entityMrn.lastIndexOf(":");
        return entityMrn.substring(idx);
    }

    public static String getServiceTypeFromMrn(String serviceMrn) {
        int startIdx = serviceMrn.indexOf(":service:") + 9;
        int endIdx = serviceMrn.indexOf(":", startIdx);
        return serviceMrn.substring(startIdx, endIdx);
    }

    public static String generateMrnForEntity(String orgMrn, String type, String entityId) {
        // clean entity id, replace reserved URN characters with "_"
        // others: "()+,-.:=@;$_!*'"   reserved: "%/?#"
        entityId = entityId.replaceAll("[()+,-.:=@;$_!*'%/??#]", "_"); // double questionmark as escape
        String mrn = "";
        if ("service".equals(type)) {
            // <org-mrn>:service:<service-design-or-spec-id>:instance:<instance-id>
            // urn:mrn:mcl:org:dma:service:nw-nm:instance:nw-nm2
            throw new IllegalArgumentException("Generating MRN for services is not supported");
        } else {
            mrn = orgMrn + ":" + type + ":" + entityId;
        }
        return mrn;
    }

    public static boolean validateMrn(String mrn) {
        // TODO: validating of mrn based on the entity type
        if (mrn == null || mrn.trim().isEmpty()) {
            return false;
        }
        return URN_PATTERN.matcher(mrn).matches();
    }

    /**
     * Generates a client name - used for client name in Keycloak
     * @param serviceMrn
     * @return
     */
    public static String generateClientName(String serviceMrn) {
        String orgShortName = getOrgShortNameFromEntityMrn(serviceMrn);
        String orgMrn = getOrgMrnEntityMrn(serviceMrn);
        String orgValidator = getOrgValidatorFromOrgMrn(orgMrn);
        String serviceName = getEntityIdFromMrn(serviceMrn);
        String serviceType = getServiceTypeFromMrn(serviceMrn);
        String clientName = orgValidator + "_" + orgShortName + "_" + serviceType + "_" + serviceName;
        return clientName;
    }

}
