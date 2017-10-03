package com.dataheaps.beanszoo.exceptions;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Created by cs186076 on 29/9/17.
 */
public class BeansZooException extends RuntimeException{

    String message;
    Throwable cause;

    public BeansZooException(String message){
    }

    public BeansZooException(Throwable cause){
        this.cause = cause;
    }

}
