package com.milind.lazypanel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class SignUpRequestDto {

    private String username;
    private String firstName;
    private String lastName;

}
