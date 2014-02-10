package com.griddynamics;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Work
 * Date: 11.11.13
 * Time: 3:18
 * To change this template use File | Settings | File Templates.
 */
public class Classes {
}

class A implements Serializable {
    public B b;
}

class B implements Serializable {
    public A a;
}


class Class1 implements Serializable{
    int a = 2;

    public Class1() {

    }

    public int getA() {
        return a;
    }

    @Override
    public boolean equals(Object ob) {
        if(this ==  ob) return true;

        if(!this.getClass().equals(ob.getClass())) return false;
        Class1 class1 = (Class1) ob;
        return this.a == class1.a;
    }
}

class Class2 implements Serializable{
    private int c = 3;
    private Class1 class1 = new Class1();

    public void setClass1(Class1 class11) {
        this.class1 = class11;
    }

    public Class1 getClass1() {
        return class1;
    }

    @Override
    public boolean equals(Object ob) {
        if(this == ob) return true;

        if(this.getClass().equals(ob.getClass()) != true) {
            return false;
        }

        if(this.c != ((Class2)ob).c) {
            return false;
        }

        if(!this.class1.equals(((Class2) ob).getClass1())) {
            return false;
        }

        return true;
    }
}


class TestTransientField implements Serializable{
    transient int a = 2;
    int b = 3;
}

