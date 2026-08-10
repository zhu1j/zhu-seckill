package cn.net.zhu.seckill.business.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    private final int code;
    private final String message;

    public BusinessException(int code, String message){
        super(message);
        this.code = code;
        this.message = message;
    }
    public BusinessException(String message) {
        this(1, message);// 👉 调用上面的 (int code,String message)构造
    }
}
