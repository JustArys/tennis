package com.example.tennis.kz.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultDto {
    private Long id;
    private String firstName;
    private String lastName;
    private Float rating;
}