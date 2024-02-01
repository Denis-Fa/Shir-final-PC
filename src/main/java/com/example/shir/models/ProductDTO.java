package com.example.shir.models;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ProductDTO {
    private long id;
    private double rating;

    public ProductDTO(long id) {
        this.id = id;
    }

    @JsonSetter("оценка")
    public void setRating(String rating) {
        this.rating = Double.parseDouble(rating);
    }

    @JsonSetter("id")
    public void setId(String id) {
        this.id = Long.parseLong(id);
    }
}