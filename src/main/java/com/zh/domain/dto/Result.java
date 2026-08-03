package com.zh.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;
    private String message;
    private Object data;
    private String timestamp;


    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    public static Result ok() {return new Result(0, "ok", null, now());}

    public static Result ok(Object data) {return new Result(0, "ok", data, now());}

    public static Result ok(String message, Object data) {return new Result(0, message, data, now());}

    public static Result fail() {return new Result(-1, "操作失败", null, now());}

    public static Result fail(String message) {return fail();}
}