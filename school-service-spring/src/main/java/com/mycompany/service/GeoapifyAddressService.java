package com.mycompany.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycompany.config.GeoapifyProperties;
import com.mycompany.models.Address;
import com.mycompany.models.AddressValidationResponse;

@Service
public class GeoapifyAddressService {

    // min confidence threshold to consider an address valid
    private static final double MIN_CONFIDENCE = 0.6;

    private final GeoapifyProperties props;
    private final RestClient restClient;

    public GeoapifyAddressService(GeoapifyProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    public AddressValidationResponse validate(String street, String city, String state, String zip) {
        GeoapifyResponse resp;
        try {
            resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                    .path("/v1/geocode/search")
                    .queryParam("street", street)
                    .queryParam("city", city)
                    .queryParam("state", state)
                    .queryParam("postcode", zip)
                    .queryParam("country", "United States")
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("apiKey", props.getApiKey())
                    .build())
                    .retrieve()
                    .body(GeoapifyResponse.class);
        } catch (HttpClientErrorException e) {
            return new AddressValidationResponse(false, null,
                    "Address lookup failed: " + e.getStatusCode());
        }

        if (resp == null || resp.results == null || resp.results.isEmpty()) {
            return new AddressValidationResponse(false, null, "Address not found");
        }

        GeoapifyResult result = resp.results.get(0);

        if (result.rank == null || result.rank.confidence < MIN_CONFIDENCE) {
            return new AddressValidationResponse(false, null,
                    "Address could not be verified (low confidence)");
        }

        // normalize the address from Geoapify's standardized fields
        String normalizedStreet = result.addressLine1 != null ? result.addressLine1 : street;
        String normalizedCity = result.city != null ? result.city : city;
        String normalizedState = result.stateCode != null ? result.stateCode : state.toUpperCase();
        String normalizedZip = result.postcode != null ? result.postcode : zip;

        Address normalized = new Address(normalizedStreet, normalizedCity, normalizedState, normalizedZip);
        return new AddressValidationResponse(true, normalized, "Address verified successfully");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeoapifyResponse {

        @JsonProperty("results")
        public List<GeoapifyResult> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeoapifyResult {

        @JsonProperty("address_line1")
        public String addressLine1;
        @JsonProperty("city")
        public String city;
        @JsonProperty("state")
        public String state;
        @JsonProperty("state_code")
        public String stateCode;
        @JsonProperty("postcode")
        public String postcode;
        @JsonProperty("formatted")
        public String formatted;
        @JsonProperty("rank")
        public GeoapifyRank rank;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeoapifyRank {

        @JsonProperty("confidence")
        public double confidence;
    }
}
