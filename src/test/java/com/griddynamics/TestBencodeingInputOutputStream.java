package com.griddynamics;

import org.junit.Test;

import java.io.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Work
 * Date: 11.11.13
 * Time: 2:08
 * To change this template use File | Settings | File Templates.
 */
public class TestBencodeingInputOutputStream {

    @Test
    public void testNumber() throws IOException {
        String fileName = "test.txt";

        BencodingOutputStream bos = new BencodingOutputStream(new FileOutputStream(fileName));
        BencodingInputStream bis = new BencodingInputStream(new FileInputStream(fileName));

        Integer argInt = 5;
        Integer resultInt = null;

        Long argLong = 7l;
        Long resultLong = null;

        bos.writeInt(argInt);
        resultInt = bis.readInt();

        bos.writeLong(argLong);
        resultLong = bis.readLong();

        assertEquals(argInt, resultInt);
        assertEquals(argLong,resultLong);

    }

    @Test
    public void testString() throws IOException {
        String fileName = "test.txt";

        BencodingOutputStream bos = new BencodingOutputStream(new FileOutputStream(fileName));
        BencodingInputStream bis = new BencodingInputStream(new FileInputStream(fileName));

        String arg = "abc";
        String result = null;

        bos.writeString(arg);
        result = bis.readString();

        assertEquals(arg,result);
    }


    @Test
    public void testObject() throws IOException, IllegalAccessException {
        String fileName = "test.txt";

        BencodingOutputStream bos = new BencodingOutputStream(new FileOutputStream(fileName));
        BencodingInputStream bis = new BencodingInputStream(new FileInputStream(fileName));

        Class1 class1 = new Class1();
        Class2 class2 = new Class2();
        class2.setClass1(class1);

        bos.writeObject(class2);
        Class2 class3 = (Class2) bis.readObject();

        assertTrue(class3.equals(class2));
    }

    @Test
    public void testCyclicReferences() throws IOException, IllegalAccessException {
        String fileName = "test.txt";

        BencodingOutputStream bos = new BencodingOutputStream(new FileOutputStream(fileName));
        BencodingInputStream bis = new BencodingInputStream(new FileInputStream(fileName));

        A a = new A();
        B b = new B();

        a.b = b;
        b.a = a;

        A a2 = new A();

        bos.writeObject(a);
        a2 = (A) bis.readObject();

        assertNotSame(a,a2);
    }


    @Test
    public void testTransientField() throws IOException, IllegalAccessException {
        String fileName = "test.txt";

        BencodingOutputStream bos = new BencodingOutputStream(new FileOutputStream(fileName));
        BencodingInputStream bis = new BencodingInputStream(new FileInputStream(fileName));

        TestTransientField testTransientField = new TestTransientField();
        testTransientField.a = 10;
        testTransientField.b = 1;

        TestTransientField t;

        bos.writeObject(testTransientField);
        t = (TestTransientField) bis.readObject();

        assertEquals(t.b,testTransientField.b);
        assertNotSame(t.a,testTransientField.a);
    }

    public class NonSerializableClass {

    }

    @Test
    public void testNobSerializable() throws IOException, IllegalAccessException {
        String fileName = "test.txt";

        BencodingOutputStream bos = new BencodingOutputStream(new FileOutputStream(fileName));
        BencodingInputStream bis = new BencodingInputStream(new FileInputStream(fileName));

        NonSerializableClass nonSerializableClass = new NonSerializableClass();

        try{
        bos.writeObject(nonSerializableClass);
        } catch (NotSerializableException e) {

        }
    }

}
