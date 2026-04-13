package com.mycompany.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressValidationResponse {
    private boolean valid;
    private Address normalizedAddress;
    private String message;
}
