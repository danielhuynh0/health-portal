package com.mycompany.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycompany.config.GeoapifyProperties;
import com.mycompany.models.Address;
import com.mycompany.models.Clinic;

@Service
public class GeoapifyPlacesService {

    private static final Logger log = LoggerFactory.getLogger(GeoapifyPlacesService.class);
    private static final String HEALTHCARE_CATEGORIES =
            "healthcare.clinic_or_praxis,healthcare.hospital,healthcare.pharmacy";
    private static final int RADIUS_METERS = 16_000; // ~10 miles

    private final GeoapifyProperties props;
    private final RestClient restClient;

    public GeoapifyPlacesService(GeoapifyProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    /**
     * Search for real-world healthcare facilities near the given ZIP / city
     * using the Geoapify Places API. Returns a page of results as Clinic objects.
     */
    public List<Clinic> searchNearby(String zip, String city, int limit, int offset) {
        try {
            double[] latLon = geocode(zip, city);
            if (latLon == null) return List.of();
            return fetchPlaces(latLon[0], latLon[1], limit, offset);
        } catch (RestClientException e) {
            log.warn("Geoapify Places lookup failed for zip={} city={}: {}", zip, city, e.getMessage());
            return List.of();
        }
    }

    // Step 1 — geocode ZIP / city to lat/lon

    private double[] geocode(String zip, String city) {
        GeocodeResponse resp = restClient.get()
                .uri(b -> b.path("/v1/geocode/search")
                        .queryParamIfPresent("postcode", java.util.Optional.ofNullable(zip))
                        .queryParamIfPresent("city",     java.util.Optional.ofNullable(city))
                        .queryParam("filter", "countrycode:us")
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .queryParam("apiKey", props.getApiKey())
                        .build())
                .retrieve()
                .body(GeocodeResponse.class);

        if (resp == null || resp.results == null || resp.results.isEmpty()) {
            log.warn("Geoapify geocode returned no results for zip={} city={}", zip, city);
            return null;
        }
        GeocodeResult r = resp.results.get(0);
        double confidence = (r.rank != null) ? r.rank.confidence : 0;
        if (confidence < 0.5) {
            log.warn("Geoapify geocode low confidence ({}) for zip={} city={} — treating as not found",
                    confidence, zip, city);
            return null;
        }
        log.info("Geocoded zip={} city={} → lat={} lon={} confidence={}", zip, city, r.lat, r.lon, confidence);
        return new double[]{r.lat, r.lon};
    }

    // Step 2 — search Places API in a radius around that point

    private List<Clinic> fetchPlaces(double lat, double lon, int limit, int offset) {
        PlacesResponse resp = restClient.get()
                .uri(b -> b.path("/v2/places")
                        .queryParam("categories", HEALTHCARE_CATEGORIES)
                        .queryParam("filter", "circle:" + lon + "," + lat + "," + RADIUS_METERS)
                        .queryParam("bias", "proximity:" + lon + "," + lat)
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("apiKey", props.getApiKey())
                        .build())
                .retrieve()
                .body(PlacesResponse.class);

        if (resp == null || resp.features == null) return List.of();

        return resp.features.stream()
                .map(f -> mapToClinic(f.properties))
                .filter(c -> c != null)
                .toList();
    }

    private Clinic mapToClinic(PlaceProperties p) {
        if (p == null || p.name == null) return null;

        // Deterministic UUID derived from Geoapify's place_id so the same
        // physical location always gets the same ID within this response.
        UUID id = UUID.nameUUIDFromBytes(
                (p.placeId != null ? p.placeId : p.name).getBytes());

        String street = p.addressLine1 != null ? p.addressLine1
                : (p.housenumber != null ? p.housenumber + " " + p.street : p.street);

        Address addr = new Address(
                street   != null ? street           : "",
                p.city   != null ? p.city           : "",
                p.stateCode != null ? p.stateCode   : (p.state != null ? p.state : ""),
                p.postcode  != null ? p.postcode    : ""
        );

        Clinic clinic = new Clinic();
        clinic.setId(id);
        clinic.setName(p.name);
        clinic.setAddress(addr);
        clinic.setPhone(p.phone);
        return clinic;
    }

    // Internal response shapes

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeocodeResponse {
        @JsonProperty("results") public List<GeocodeResult> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeocodeResult {
        @JsonProperty("lat")  public double lat;
        @JsonProperty("lon")  public double lon;
        @JsonProperty("rank") public GeocodeRank rank;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeocodeRank {
        @JsonProperty("confidence") public double confidence;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlacesResponse {
        @JsonProperty("features") public List<PlaceFeature> features;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceFeature {
        @JsonProperty("properties") public PlaceProperties properties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceProperties {
        @JsonProperty("name")          public String name;
        @JsonProperty("place_id")      public String placeId;
        @JsonProperty("address_line1") public String addressLine1;
        @JsonProperty("housenumber")   public String housenumber;
        @JsonProperty("street")        public String street;
        @JsonProperty("city")          public String city;
        @JsonProperty("state")         public String state;
        @JsonProperty("state_code")    public String stateCode;
        @JsonProperty("postcode")      public String postcode;
        @JsonProperty("phone")         public String phone;
    }
}
