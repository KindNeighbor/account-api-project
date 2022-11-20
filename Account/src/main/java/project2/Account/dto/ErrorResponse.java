package project2.Account.dto;

import lombok.*;
import project2.Account.type.ErrorCode;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private ErrorCode errorCode;
    private String errorMessage;
}
