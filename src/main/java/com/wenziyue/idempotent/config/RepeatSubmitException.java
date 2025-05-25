package com.wenziyue.idempotent.config;

/**
 * @author wenziyue
 */
public class RepeatSubmitException extends RuntimeException{

    public RepeatSubmitException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "RepeatSubmitException{" +
                "message='" + getMessage() + '\'' +
                '}';
    }
}
