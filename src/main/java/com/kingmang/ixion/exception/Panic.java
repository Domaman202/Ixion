package com.kingmang.ixion.exception;

import com.kingmang.ixion.api.IxApi;

public class Panic {

    private final String R = "\u001B[31m";
    private final String RESET = "\u001B[0m";
    private final String message;

    public Panic(String message){
        this.message = message;
    }

    public void send(){
        IxApi.exit(R + ("panic: " + message) + RESET, 1);
    }
}
