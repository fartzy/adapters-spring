package com.acme.ng.provider.adapter.common.service;

import com.acme.ng.provider.adapter.common.domain.SyncResult;
import com.acme.ng.provider.model.adapter.AdapterEvent;

/**
 * Created 4/26/2018.
 */



public interface ExternalDbSyncService {

    SyncResult sync(AdapterEvent adapterEvent);
}
