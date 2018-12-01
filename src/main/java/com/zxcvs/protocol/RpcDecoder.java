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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RPC Decoder
 *
 * @author Xiaohui Yang
 * Create at 2018/8/27 15:33
 */

@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {

    private static final int MIN_BYTES_LENGTH = 4;

    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf == null) {
            log.warn("ByteBuf is empty!");
            return;
        }

        buf.markReaderIndex();
        int dataLength = buf.readInt();
        int readableLength = buf.readableBytes();

        // length check
        if (readableLength < MIN_BYTES_LENGTH || readableLength < dataLength) {
            log.warn("readable bytes length less than 4 or less than data length!");
            buf.resetReaderIndex();
            return;
        }

        byte[] data = new byte[dataLength];
        buf.readBytes(data);

        Object object = SerializationUtils.deserialize(data, genericClass);
        out.add(object);
    }
}
