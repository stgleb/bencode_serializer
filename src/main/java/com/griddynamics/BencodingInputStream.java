

package com.griddynamics;

import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import static java.lang.System.identityHashCode;


public class BencodingInputStream extends FilterInputStream implements DataInput {
    
    /**
     * The charset that is being used for String.
     */
    private final String charset;
    
    /**
     * Whether or not all byte-Arrays should be decoded as String.
     */

    private final boolean decodeAsString;

    /*Gettin instance of Unsafe class to instantiate objects safely*/

    public static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");

        singleoneInstanceField.setAccessible(true);
        return (Unsafe) singleoneInstanceField.get(null);
    }
    /**
     * Creates a BencodingInputStream with the default encoding.
     */

    public BencodingInputStream(InputStream in) {
        this(in, BencodingUtils.UTF_8, false);
    }
    
    /**
     * Creates a BencodingInputStream with the given encoding.
     */
    public BencodingInputStream(InputStream in, String charset) {
        this(in, charset, false);
    }
    
    /**
     * Creates a BencodingInputStream with the default encoding.
     */
    public BencodingInputStream(InputStream in, boolean decodeAsString) {
        this(in, BencodingUtils.UTF_8, decodeAsString);
    }
    
    /**
     * Creates a BencodingInputStream with the given encoding.
     */
    public BencodingInputStream(InputStream in, 
            String charset, boolean decodeAsString) {
        super(in);
        
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        
        this.charset = charset;
        this.decodeAsString = decodeAsString;
    }
    
    /**
     * Returns the charset that is used to decode String.
     * The default value is UTF-8.
     */
    public String getCharset() {
        return charset;
    }
    
    /**
     * Returns true if all byte-Arrays are being turned into String.
     */
    public boolean isDecodeAsString() {
        return decodeAsString;
    }
    
    /**
     * Reads and returns an Object.
     */
    public Object readObject() throws IOException {
        int token = read();
        if (token == -1) {
            throw new EOFException();
        }
        
        return readObject(token);
    }
    
    /**
     * Reads and returns an Object of the given type
     */
    protected Object readObject(int token) throws IOException {
        if (token == BencodingUtils.DICTIONARY) {
            Map<String,?> map = readMap0(Object.class);

            Object ob = map.get(BencodingUtils.OBJECT);
            map.remove(BencodingUtils.OBJECT);

            if(ob != null) {
                ob = readCustom(map);
                return ob;
            } else {
                return readMap0(Object.class);
            }
        } else if (token == BencodingUtils.LIST) {
            return readList0(Object.class);
            
        } else if (token == BencodingUtils.NUMBER) {
            return readNumber0();
            
        } else if (isDigit(token)) {
            byte[] data = readBytes(token);
            return decodeAsString ? new String(data, charset) : data;
            
        } else {
            return null;
        }
    }
    
    /**
     * Method intended to read custom objects.
     */
    private Object readCustom(Map<String,?> map) throws IOException{
        //Tree map represented initial graph.
        //Map<String,?> map =  new TreeMap<String, Object>();
        //Graph with restored references,
        Map<String, Object> restored = new TreeMap<String, Object>();
        //variable used to restore String values;
        byte[] bytes = null;
        bytes = (byte[]) (map.get(BencodingUtils.ROOT) == null ? "" : map.get(BencodingUtils.ROOT));
        String rootHash = new String(bytes);
        map.remove(BencodingUtils.ROOT);

        Set<? extends Map.Entry<String,?>> entries = map.entrySet();

        //Instantiate all objects in graph.
        try {
            //Get instance of Unsafe class for objects instantiation.
            Unsafe unsafe = getUnsafe();

            for(Map.Entry<String,?> e : entries) {
                 String id = e.getKey();
                 List<Object> fieldList = (List<Object>) e.getValue();

                bytes = (byte[]) fieldList.get(0);
                String className = new String(bytes);

                Class clazz = Class.forName(className);

                Object ob = unsafe.allocateInstance(clazz);
                fieldList.add(ob);
                Integer hash = identityHashCode(ob);
                restored.put(e.getKey(),ob);

                Field[] fields = clazz.getDeclaredFields();

                for(int i = 1;i < fieldList.size() - 1;i++) {
                    bytes = (byte[]) fieldList.get(i);
                    String desrciption = new String(bytes);

                    Field field = null;

                    if(desrciption.equals(BencodingUtils.PRIMITIVE)) {
                        i++;
                        List<Object> params = (List<Object>) fieldList.get(i);
                        String typeName = null;
                        String fieldName = null;
                        String ref = null;

                        bytes = (byte[]) params.get(0);
                        typeName = new String(bytes);
                        bytes = (byte[]) params.get(1);
                        fieldName = new String(bytes);

                        field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);

                        if(field.getType().equals(String.class)) {
                            bytes = (byte[]) params.get(2);
                            String value = new String(bytes);
                            field.set(ob, value);
                        } else {
                            field.set(ob, params.get(2));
                        }

                    } else {
                        i++;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchFieldException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        //Restore relations between objects.

        Object root = getObject(restored, rootHash, entries);

        return root;
     }

    private Object getObject(Map<String, Object> restored, String rootHash, Set<? extends Map.Entry<String, ?>> entries) {
        byte[] bytes;
        try {

            for(Map.Entry<String,?> e : entries) {
                String id = e.getKey();
                List<Object> fieldList = (List<Object>) e.getValue();
                Object ob = fieldList.get(fieldList.size() - 1);

                for(int i = 1;i < fieldList.size() - 1;i++) {
                    bytes = (byte[]) fieldList.get(i);
                    String desrciption = new String(bytes);

                    Field field = null;
                    i++;

                    if(desrciption.equals(BencodingUtils.REFERENCE))
                    {
                        List<Object> params = (List<Object>) fieldList.get(i);
                        String typeName = null;
                        String fieldName = null;

                        bytes = (byte[]) params.get(0);
                        typeName = new String(bytes);
                        bytes = (byte[]) params.get(1);
                        fieldName = new String(bytes);
                        bytes = (byte[]) params.get(2);
                        String hashKey = new String(bytes);

                        field = ob.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);

                        Object ref = restored.get(hashKey);

                        field.set(ob,ref);
                    }
                }

            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchFieldException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Object root = null;

        if(!"".equals(rootHash)) {
            root = restored.get(rootHash);
        }
        return root;
    }

    /*Checks whether the object is Terminal type*/
    public boolean isTerminal(Class value) {
          return PrimitiveContainer.contains(value);
    }

    /**
     * Reads and returns a byte-Array.
     */
    public byte[] readBytes() throws IOException {
        int token = read();
        if (token == -1) {
            throw new EOFException();
        }
        
        return readBytes(token);
    }
    
    /**
     * 
     */
    private byte[] readBytes(int token) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append((char)token);
        
        while ((token = read()) != BencodingUtils.LENGTH_DELIMITER) {
            if (token == -1) {
                throw new EOFException();
            }
            
            buffer.append((char)token);
        }
        
        int length = Integer.parseInt(buffer.toString());
        byte[] data = new byte[length];
        readFully(data);
        return data;
    }
    
    /**
     * Reads and returns a String.
     */
    public String readString() throws IOException {
        return readString(charset);
    }
    
    /**
     * 
     */
    private String readString(String encoding) throws IOException {
        return new String(readBytes(), encoding);
    }
    
    /**
     * Reads and returns an Enum.
     */
    public <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
        return Enum.valueOf(clazz, readString());
    }
    
    /**
     * Reads and returns a char.
     */
    @Override
    public char readChar() throws IOException {
        return readString().charAt(0);
    }
    
    /**
     * Reads and returns a boolean.
     */
    @Override
    public boolean readBoolean() throws IOException {
        return readInt() != 0;
    }
    
    /**
     * Reads and returns a byte.
     */
    @Override
    public byte readByte() throws IOException {
        return readNumber().byteValue();
    }
    
    /**
     * Reads and returns a short.
     */
    @Override
    public short readShort() throws IOException {
        return readNumber().shortValue();
    }
    
    /**
     * Reads and returns an int.
     */
    @Override
    public int readInt() throws IOException {
        return readNumber().intValue();
    }
    
    /**
     * Reads and returns a float.
     */
    @Override
    public float readFloat() throws IOException {
        return readNumber().floatValue();
    }
    
    /**
     * Reads and returns a long.
     */
    @Override
    public long readLong() throws IOException {
        return readNumber().longValue();
    }
    
    /**
     * Reads and returns a double.
     */
    @Override
    public double readDouble() throws IOException {
        return readNumber().doubleValue();
    }
    
    /**
     * Reads and returns a Number.
     */
    public Number readNumber() throws IOException {
        int token = read();
        if (token == -1) {
            throw new EOFException();
        }
        
        if (token != BencodingUtils.NUMBER) {
            throw new IOException();
        }
        
        return readNumber0();
    }
    
    /**
     * 
     */
    private Number readNumber0() throws IOException {
        StringBuilder buffer = new StringBuilder();
        
        boolean decimal = false;
        int token = -1;
        
        while ((token = read()) != BencodingUtils.EOF) {
            if (token == -1) {
                throw new EOFException();
            }
            
            if (token == '.') {
                decimal = true;
            }
            
            buffer.append((char)token);
        }
        
        try {
            if (decimal) {
                return new Double(buffer.toString());
            } else {
                return new Integer(buffer.toString());
            }
        } catch (NumberFormatException err) {
            throw new IOException("NumberFormatException", err);
        }
    }
    
    /**
     * Reads and returns an array of Objects.
     */
    public Object[] readArray() throws IOException {
        return readList().toArray();
    }
    
    /**
     * Reads and returns an array of Objects.
     */
    public <T> T[] readArray(T[] a) throws IOException {
        return readList().toArray(a);
    }
    
    /**
     * Reads and returns a List.
     */
    public List<?> readList() throws IOException {
        return readList(Object.class);
    }
    
    /**
     * Reads and returns a List.
     */
    public <T> List<T> readList(Class<T> clazz) throws IOException {
        int token = read();
        if (token == -1) {
            throw new EOFException();
        }
        
        if (token != BencodingUtils.LIST) {
            throw new IOException();
        }
        
        return readList0(clazz);
    }
    
    /**
     * 
     */
    private <T> List<T> readList0(Class<T> clazz) throws IOException {
        List<T> list = new ArrayList<T>();
        int token = -1;
        while ((token = read()) != BencodingUtils.EOF) {
            if (token == -1) {
                throw new EOFException();
            }
            
            list.add(clazz.cast(readObject(token)));
        }
        return list;
    }
    
    /**
     * Reads and returns a Map.
     */
    public Map<String, ?> readMap() throws IOException {
        return readMap(Object.class);
    }
    
    /**
     * Reads and returns a {@link java.util.Map}.
     */
    public <T> Map<String, T> readMap(Class<T> clazz) throws IOException {
        int token = read();
        if (token == -1) {
            throw new EOFException();
        }
        
        if (token != BencodingUtils.DICTIONARY) {
            throw new IOException();
        }
        
        return readMap0(clazz);
    }
    
    /**
     * 
     */
    private <T> Map<String, T> readMap0(Class<T> clazz) throws IOException {
        Map<String, T> map = new TreeMap<String, T>();
        int token = -1;
        while ((token = read()) != BencodingUtils.EOF) {
            if (token == -1) {
                throw new EOFException();
            }
            
            String key = new String(readBytes(token), charset);
            T value = clazz.cast(readObject());
            
            map.put(key, value);
        }
        
        return map;
    }

    /**
     * 
     */
    @Override
    public void readFully(byte[] dst) throws IOException {
        readFully(dst, 0, dst.length);
    }
    
    /**
     * 
     */
    @Override
    public void readFully(byte[] dst, int off, int len) throws IOException {
        int total = 0;
        
        while (total < len) {
            int r = read(dst, total, len-total);
            if (r == -1) {
                throw new EOFException();
            }
            
            total += r;
        }
    }

    @Override
    public String readLine() throws IOException {
        return readString();
    }

    /**
     * Reads and returns an unsigned byte.
     */
    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    /**
     * Reads and returns an unsigned short.
     */
    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads and returns an UTF encoded String.
     */
    @Override
    public String readUTF() throws IOException {
        return readString(BencodingUtils.UTF_8);
    }

    /**
     * Skips the given number of bytes.
     */
    @Override
    public int skipBytes(int n) throws IOException {
        return (int)skip(n);
    }
    
    /**
     * Returns true if the given token is a digit.
     */
    private static boolean isDigit(int token) {
        return '0' <= token && token <= '9';
    }
}
