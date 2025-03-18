package com.example.tennis.kz.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomPageResponse<T> {
    private Integer page;
    private Integer size;
    private Long totalCount;
    private List<T> data;
}
