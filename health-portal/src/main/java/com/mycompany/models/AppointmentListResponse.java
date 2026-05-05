package com.mycompany.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppointmentListResponse {
    private List<Appointment> content;
    private PageMetadata page;
    @JsonProperty("_links")
    private Map<String, Link> links;
}
