package com.hsbc;

import java.util.Objects;

public class Message {
    public enum Type {
        TRADE,
        QUOTE,
        REFERENCE
    }
    private Type a;
    private String b;
    private String c;
    private long d;
    private int e;
    private boolean f;

    public Type getA() {
        return a;
    }

    public void setA(Type a) {
        this.a = a;
    }


    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
    }

    public long getD() {
        return d;
    }

    public void setD(long d) {
        this.d = d;
    }

    public int getE() {
        return e;
    }

    public void setE(int e) {
        this.e = e;
    }

    public boolean isF() {
        return f;
    }

    public void setF(boolean f) {
        this.f = f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return d == message.d && e == message.e && f == message.f && a == message.a && Objects.equals(b, message.b) && Objects.equals(c, message.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c, d, e, f);
    }

    @Override
    protected Object clone() {
        Message cloned = new Message();
        cloned.setA(a);
        cloned.setB(b);
        cloned.setC(c);
        cloned.setD(d);
        cloned.setE(e);
        cloned.setF(f);
        return cloned;
    }

    @Override
    public String toString() {
        return "Message{" +
                "a=" + a +
                ", b='" + b + '\'' +
                ", c='" + c + '\'' +
                ", d=" + d +
                ", e=" + e +
                ", f=" + f +
                '}';
    }
}
