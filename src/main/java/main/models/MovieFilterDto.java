package main.models;

import lombok.Data;

@Data
public class MovieFilterDto {
    private boolean like;
    private String decade;
    private String genre;
}