package com.mycompany.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Link {
    private String href;
    private String rel;
    private String method;
}
