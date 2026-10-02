package com.caselock.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Consistent response envelope used by every endpoint in the API.
 * Success: {"success": true, "message": "...", "data": {...}}
 * Failure: {"success": false, "message": "...", "errorCode": "..."}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }
}
