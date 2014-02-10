/*
 * Copyright 2009 Roger Kapsi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.griddynamics;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static java.lang.System.identityHashCode;

/**
 * An implementation of of OutputStream that can produce
 * Bencoded (Bee-Encoded) data.
 */
public class BencodingOutputStream extends FilterOutputStream 
        implements DataOutput {

    /**
     * The String charset.
     */
    private final String charset;


    private Queue<Object> queue = new LinkedList<Object>();

    /**
     * Creates a BencodingOutputStream with the default charset.
     */
    public BencodingOutputStream(OutputStream out) {
        this(out, BencodingUtils.UTF_8);
    }

    /**
     * Creates a BencodingOutputStream with the given encoding.
     */
    public BencodingOutputStream(OutputStream out, String charset) {
        super(out);
        
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        
        this.charset = charset;
    }
    
    /**
     * Returns the charset that is used to encode String.
     * The default value is UTF-8.
     */
    public String getCharset() {
        return charset;
    }
    
    /**
     * Writes an Object.
     */
    @SuppressWarnings("unchecked")
    public void writeObject(Object value) throws IOException, IllegalAccessException {
        
        if (value == null) {
            writeNull();
            
        } else if (value instanceof byte[]) {
            writeBytes((byte[])value);
            
        } else if (value instanceof Boolean) {
            writeBoolean((Boolean)value);
        
        } else if (value instanceof Character) {
            writeChar((Character)value);
            
        } else if (value instanceof Number) {
            writeNumber((Number)value);
            
        } else if (value instanceof String) {
            writeString((String)value);
            
        } else if (value instanceof Collection<?>) {
            writeCollection((Collection<?>)value);
        
        } else if (value instanceof Map<?, ?>) {
            writeMap((Map<String, ?>)value);
            
        } else if (value instanceof Enum<?>) {
            writeEnum((Enum<?>)value);
        
        } else if (value.getClass().isArray()) {
            writeArray(value);
            
        } else {
            writeCustom(value);
        }
    }

    public void writeNull() throws IOException {
        throw new IOException("Null is not supported");
    }
    
    /**
     * Method writes custom type object.
     */
    protected void writeCustom(Object value) throws IOException, IllegalAccessException {

        Map<String,Object> map = new TreeMap<String, Object>();

        Integer hash = identityHashCode(value);
        map.put(BencodingUtils.ROOT,hash.toString());
        queue.offer(value);

        while(!queue.isEmpty()) {
            Object cur = queue.poll();
            hash = identityHashCode(cur);

            /*Check for cyclic reference*/

            if(map.get(hash.toString()) != null) {
                continue;
            }

            Class clazz = cur.getClass();

            Set<Class> interfaces = new HashSet<Class>(Arrays.asList(clazz.getInterfaces()));

            if( !interfaces.contains(Serializable.class)) {
                throw new NotSerializableException();
            }

            Field[] fields = clazz.getDeclaredFields();
            List<Object> fieldList = new LinkedList<Object>();
            fieldList.add(clazz.getName());

            for(Field f : fields) {
                f.setAccessible(true);
                List<Object> info = new LinkedList<Object>();

                if(Modifier.isTransient(f.getModifiers())) {
                    continue;
                }

                if(isTerminal(f.getType())) {
                    fieldList.add(BencodingUtils.PRIMITIVE);
                    info.add(f.getType().getName());
                    info.add(f.getName());
                    info.add(f.get(cur));
                    fieldList.add(info);
                } else {
                    fieldList.add(BencodingUtils.REFERENCE);
                    info.add(f.getType().getName());
                    info.add(f.getName());
                    hash = identityHashCode(f.get(cur));
                    info.add(hash.toString());
                    fieldList.add(info);

                    Object ob = f.get(cur);
                    queue.add(ob);
                }
            }
            hash = identityHashCode(cur);
            map.put(hash.toString(), fieldList);
        }

        map.put(BencodingUtils.OBJECT,"");

        writeObject(map);
    }


    public boolean isTerminal(Class value) {
        return PrimitiveContainer.contains(value);
    }

    /**
     * Writes the given byte-Array
     */
    public void writeBytes(byte[] value) throws IOException {
        writeBytes(value, 0, value.length);
    }
    
    /**
     * Writes the given byte-Array 
     */
    public void writeBytes(byte[] value, int offset, int length) throws IOException {
        write(Integer.toString(length).getBytes(charset));
        write(BencodingUtils.LENGTH_DELIMITER);
        write(value, offset, length);
    }
    
    /**
     * Writes a boolean
     */
    @Override
    public void writeBoolean(boolean value) throws IOException {
        writeNumber(value ? BencodingUtils.TRUE : BencodingUtils.FALSE);
    }
    
    /**
     * Writes a char
     */
    @Override
    public void writeChar(int value) throws IOException {
        writeString(Character.toString((char)value));
    }
    
    /**
     * Writes a byte
     */
    @Override
    public void writeByte(int value) throws IOException {
        writeNumber(Byte.valueOf((byte)value));
    }
    
    /**
     * Writes a short
     */
    @Override
    public void writeShort(int value) throws IOException {
        writeNumber(Short.valueOf((short)value));
    }
    
    /**
     * Writes an int
     */
    @Override
    public void writeInt(int value) throws IOException {
        writeNumber(Integer.valueOf(value));
    }
    
    /**
     * Writes a long
     */
    @Override
    public void writeLong(long value) throws IOException {
        writeNumber(Long.valueOf(value));
    }
    
    /**
     * Writes a float
     */
    @Override
    public void writeFloat(float value) throws IOException {
        writeNumber(Float.valueOf(value));
    }
    
    /**
     * Writes a double
     */
    @Override
    public void writeDouble(double value) throws IOException {
        writeNumber(Double.valueOf(value));
    }
    
    /**
     * Writes a Number.
     */
    public void writeNumber(Number value) throws IOException {
        String num = value.toString();
        write(BencodingUtils.NUMBER);
        write(num.getBytes(charset));
        write(BencodingUtils.EOF);
    }
    
    /**
     * Writes a String.
     */
    public void writeString(String value) throws IOException {
        writeBytes(value.getBytes(charset));
    }
    
    /**
     * Writes a Collection.
     */
    public void writeCollection(Collection<?> value) throws IOException, IllegalAccessException {
        write(BencodingUtils.LIST);
        for (Object element : value) {
            writeObject(element);
        }
        write(BencodingUtils.EOF);
    }
    
    /**
     * Writes a Map.
     */
    public void writeMap(Map<?, ?> map) throws IOException, IllegalAccessException {
        if (!(map instanceof SortedMap<?, ?>)) {
            map = new TreeMap<Object, Object>(map);
        }
        
        write(BencodingUtils.DICTIONARY);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            
            if (key instanceof String) {
                writeString((String)key);
            } else {
                writeBytes((byte[])key);
            }
            
            writeObject(value);
        }
        write(BencodingUtils.EOF);
    }
    
    /**
     * Writes an Enum.
     */
    public void writeEnum(Enum<?> value) throws IOException {
        writeString(value.name());
    }
    
    /**
     * Writes an array
     */
    public void writeArray(Object value) throws IOException, IllegalAccessException {
        write(BencodingUtils.LIST);
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            writeObject(Array.get(value, i));
        }
        write(BencodingUtils.EOF);
    }

    /**
     * Writes the given String.
     */
    @Override
    public void writeBytes(String value) throws IOException {
        writeString(value);
    }

    /**
     * Writes the given String.
     */
    @Override
    public void writeChars(String value) throws IOException {
        writeString(value);
    }

    /**
     * Writes an UTF encoded String.
     */
    @Override
    public void writeUTF(String value) throws IOException {
        writeBytes(value.getBytes(BencodingUtils.UTF_8));
    }
}
