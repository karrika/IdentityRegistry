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
 * Utility class to create, validate and extract certain info from MRNs
 */
public class MrnUtil {

    public final static String MC_MRN_PREFIX = "urn:mrn";
    // TODO: "mcl" probably shouldn't be hardcoded...
    public final static String MC_MRN_OWNER_PREFIX = MC_MRN_PREFIX + ":mcl";
    public final static String MC_MRN_ORG_PREFIX = MC_MRN_OWNER_PREFIX + ":org";
    public final static Pattern URN_PATTERN = Pattern.compile("^urn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE);
    public final static Pattern MRN_PATTERN = Pattern.compile("^urn:mrn:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE);
    public final static Pattern MRN_SERVICE_INSTANCE_PATTERN = Pattern.compile("^urn:mrn:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+?:service:instance:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE);
    public final static Pattern MRN_USER_PATTERN = Pattern.compile("^urn:mrn:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+?:user:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE);
    public final static Pattern MRN_VESSEL_PATTERN = Pattern.compile("^urn:mrn:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+?:vessel:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE);
    public final static Pattern MRN_DEVICE_PATTERN = Pattern.compile("^urn:mrn:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+?:device:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$", Pattern.CASE_INSENSITIVE);


    public static String getOrgShortNameFromOrgMrn(String orgMrn) {
        int idx = orgMrn.lastIndexOf(":") + 1;
        return orgMrn.substring(idx);
    }

    /**
     * Returns the org shortname of the organization responsible for validating the organization that is
     * identified by the given shortname. If MaritimeCloud is the validator "mcl" is returned.
     * @param orgShortname
     * @return
     */
    public static String getOrgValidatorFromOrgShortname(String orgShortname) {
        if (orgShortname.contains("@")) {
            // This handles the nested validators
            String[] dividedShotname = orgShortname.split("@", 2);
            return dividedShotname[1];
        } else {
            return "mcl";
        }
    }

    public static String getOrgShortNameFromEntityMrn(String entityMrn) {
        // An entity MRN looks like this: urn:mrn:mcl:user:<org-shortname>:<user-id>
        int tmpIdx = entityMrn.indexOf(":user:");
        int startIdx = tmpIdx + 6;
        if (tmpIdx < 0) {
            tmpIdx = entityMrn.indexOf(":device:");
            startIdx = tmpIdx + 8;
        }
        if (tmpIdx < 0) {
            tmpIdx = entityMrn.indexOf(":vessel:");
            startIdx = tmpIdx + 8;
        }
        if (tmpIdx < 0) {
            tmpIdx = entityMrn.indexOf(":service:instance:");
            startIdx = tmpIdx + 18;
        }
        if (tmpIdx < 0) {
            throw new IllegalArgumentException("MRN is not a valid entity MRN!");
        }
        int endIdx = entityMrn.indexOf(":", startIdx);
        return entityMrn.substring(startIdx, endIdx);
    }

    public static String getEntityIdFromMrn(String entityMrn) {
        int idx = entityMrn.lastIndexOf(":") + 1;
        return entityMrn.substring(idx);
    }

    public static String getServiceTypeFromMrn(String serviceMrn) {
        if (!serviceMrn.contains(":instance:") || !serviceMrn.contains(":service:")) {
            throw new IllegalArgumentException("The MRN must belong to a service instance!");
        }
        int startIdx = serviceMrn.indexOf(":service:instance:") + 18;
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
        if (mrn == null || mrn.trim().isEmpty()) {
            throw new IllegalArgumentException("MRN is empty");
        }
        if (!MRN_PATTERN.matcher(mrn).matches()) {
            throw new IllegalArgumentException("MRN is not in a valid format");
        }
        // validate mrn based on the entity type
        if (mrn.contains(":service:") && !MRN_SERVICE_INSTANCE_PATTERN.matcher(mrn).matches()) {
            throw new IllegalArgumentException("MRN is not in a valid format for a service instances");
        } else if (mrn.contains(":user:") && !MRN_USER_PATTERN.matcher(mrn).matches()) {
            throw new IllegalArgumentException("MRN is not in a valid format for a user");
        } else if (mrn.contains(":vessel:") && !MRN_VESSEL_PATTERN.matcher(mrn).matches()) {
            throw new IllegalArgumentException("MRN is not in a valid format for a vessel");
        } else if (mrn.contains(":device:") && !MRN_DEVICE_PATTERN.matcher(mrn).matches()) {
            throw new IllegalArgumentException("MRN is not in a valid format for a device");
        }
        return true;
    }

    /**
     * Generates a client name - used for client name in Keycloak
     * @param serviceMrn
     * @return
     */
    public static String generateClientName(String serviceMrn) {
        String orgShortName = getOrgShortNameFromEntityMrn(serviceMrn);
        String orgValidator = getOrgValidatorFromOrgShortname(orgShortName);
        String serviceName = getEntityIdFromMrn(serviceMrn);
        String serviceType = getServiceTypeFromMrn(serviceMrn);
        String clientName = orgValidator + "_" + orgShortName + "_" + serviceType + "_" + serviceName;
        return clientName;
    }

}
