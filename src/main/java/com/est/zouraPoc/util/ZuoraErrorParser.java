package com.est.zouraPoc.util;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ZuoraErrorParser {

    public static int resolveStatus(int code) {
        return switch (code % 100) {
            case 20 -> HttpStatus.BAD_REQUEST.value();
            case 40 -> HttpStatus.CONFLICT.value();
            default -> HttpStatus.NOT_FOUND.value();
        };
    }
}
