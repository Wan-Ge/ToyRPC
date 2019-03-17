/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zxcvs.protocol;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serialization utils (based on Protostuff)
 *
 * @author Xiaohui Yang
 * Create at 2018/8/27 15:39
 */

@Slf4j
@NoArgsConstructor
public class SerializationUtils {

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>(16);

    private static Objenesis objenesis = new ObjenesisStd(true);

    /**
     * Get schema from map, create from clazz if not exist
     *
     * @param clazz Class of T
     * @param <T>   type of object
     * @return schema
     */
    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) cachedSchema.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(clazz);
            cachedSchema.put(clazz, schema);
        }

        return schema;
    }

    /**
     * serialize (object ---> byte[])
     *
     * @param obj the object which needs serialize
     * @param <T> class type
     * @return byte[] - result of serialize
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {
        Class<T> clazz = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(clazz);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (IllegalStateException e) {
            log.warn("serialize failed: {}", e);
            throw e;
        } finally {
            buffer.clear();
        }
    }

    /**
     * deserialize (byte[] ---> Object)
     *
     * @param data  the data which needs deserialize
     * @param clazz template
     * @param <T>   class type
     * @return Object - result of deserialize
     */
    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            T message = (T) objenesis.newInstance(clazz);
            Schema<T> schema = getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (IllegalStateException e) {
            log.warn("deserialize failed: {}", e);
            throw e;
        }
    }
}
