package com.hsbc.output;

public class ConsoleOutput implements Output {
    @Override
    public void print(Object msg) {
        System.out.println(msg.toString());
    }
}
