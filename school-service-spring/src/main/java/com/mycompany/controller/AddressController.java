package com.mycompany.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.models.AddressValidationResponse;
import com.mycompany.service.GeoapifyAddressService;

@RestController
@RequestMapping("/address")
public class AddressController {

    private static final Logger log = LoggerFactory.getLogger(AddressController.class);

    private final GeoapifyAddressService geoapifyAddressService;

    public AddressController(GeoapifyAddressService geoapifyAddressService) {
        this.geoapifyAddressService = geoapifyAddressService;
    }

    @GetMapping("/validate")
    public ResponseEntity<AddressValidationResponse> validate(
            @RequestParam String street,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String zip) {
        log.info("GET /address/validate  street={} city={} state={} zip={}", street, city, state, zip);
        return ResponseEntity.ok(geoapifyAddressService.validate(street, city, state, zip));
    }
}
