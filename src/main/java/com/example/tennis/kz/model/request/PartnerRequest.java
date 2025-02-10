package com.example.tennis.kz.model.request;

import com.example.tennis.kz.model.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRequest {
    private UserInfo userInfo;

}
