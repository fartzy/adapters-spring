package com.acme.ng.provider.adapter.rest.extension.service.controller;

import com.acme.ng.provider.adapter.rest.extension.service.service.NestedTableWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.OK;

@RestController
public class RestExtensionController {

    @Autowired
    private NestedTableWriter nstdTblWriter;

    @ResponseStatus(OK)
    @GetMapping("/tableWriter")
    public void executeTableWriter() {
        nstdTblWriter.run();
    }
}