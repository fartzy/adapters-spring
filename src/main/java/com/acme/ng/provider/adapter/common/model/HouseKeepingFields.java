package com.acme.ng.provider.adapter.common.model;

import static org.apache.coyote.http11.Constants.a;

/**
 * 
 */
public enum HouseKeepingFields {

   LAST_UPDTD_TIMESTMP_FN("LAST_UPDTD_TIMESTMP"),
   LAST_UPDTD_BY_FN("LAST_UPDTD_BY"),
   CRETD_TIMESTMP_FN("CRETD_TIMESTMP"),
   CRETD_BY_FN("CRETD_BY"),
   NEXTGEN_UPDATED_CREATED_BY("nextgen");

    private final String value;

    HouseKeepingFields(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
