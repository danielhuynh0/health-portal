package com.mycompany.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageMetadata {
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
}
