package org.waaagh.cloudfeignapi;

import java.io.Serializable;
import lombok.Data;

@Data
public class Result <T> implements Serializable
{
    private Integer code;
    private String message;
    private T data;

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(T data) {
        this(200, "操作成功", data);
    }
}
