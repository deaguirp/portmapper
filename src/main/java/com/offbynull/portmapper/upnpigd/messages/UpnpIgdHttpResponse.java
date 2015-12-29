/*
 * Copyright (c) 2013-2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.portmapper.upnpigd.messages;

import com.offbynull.portmapper.common.TextUtils;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public abstract class UpnpIgdHttpResponse implements UpnpIgdMessage {

    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String RESPONSE_CODE = "200";
    private static final String TERMINATOR = "\r\n";
    private static final String HEADER_SPLIT_POINT = TERMINATOR + TERMINATOR;

    private final Map<String, String> headers;
    private final String content;

    UpnpIgdHttpResponse(Map<String, String> headers, String content) {
        Validate.notNull(headers);
        Validate.noNullElements(headers.keySet());
        Validate.noNullElements(headers.values());
//        Validate.notNull(content); // content may be null

        this.headers = new HashMap<>(headers);
        this.content = content;
    }

    UpnpIgdHttpResponse(byte[] buffer) {
        Validate.notNull(buffer);

        // Convert buffer to string
        String bufferStr = new String(buffer, Charset.forName("US-ASCII"));

        // Split buffer to header and content
        int splitIdx = bufferStr.indexOf(HEADER_SPLIT_POINT);
        String headersStr;
        String contentStr;
        if (splitIdx == -1) {
            // No content, so just grab headers and say we don't have content? -- trying to be fault tolerant here 
            headersStr = bufferStr;
            contentStr = null;
        } else {
            headersStr = bufferStr.substring(0, splitIdx);
            contentStr = bufferStr.substring(splitIdx + HEADER_SPLIT_POINT.length());
        }

        // Parse resp and headers
        StringTokenizer tokenizer = new StringTokenizer(headersStr, TERMINATOR);

        String respStr = tokenizer.nextToken();
        respStr = TextUtils.collapseWhitespace(respStr).trim(); // get resp string, collapse whitespace for fault tolerance

        String[] splitResp = StringUtils.split(respStr, ' ');
        Validate.isTrue(splitResp.length >= 2); // ignore stuff afterwards if any (reason text)? -- trying to be fault tolerant
        Validate.isTrue(HTTP_VERSION.equalsIgnoreCase(splitResp[0])); // case insensitive for fault tolerance
        Validate.isTrue(RESPONSE_CODE.equalsIgnoreCase(splitResp[1])); // case insensitive for fault tolerance


        Map<String, String> headers = new HashMap<>();
        while (tokenizer.hasMoreTokens()) {
            String headerLine = tokenizer.nextToken().trim(); // trim to be fault tolerant, in case header has extra spaces
            if (headerLine.isEmpty()) {
                break;
            }

            String[] splitLine = StringUtils.split(headerLine, ":", 1);
            if (splitLine.length != 2) {
                continue; // skip line if no : found
            }

            String key = splitLine[0].trim();
            String value = splitLine[1].trim();

            headers.put(key, value);
        }

        this.headers = Collections.unmodifiableMap(headers);
        this.content = contentStr;
    }
    
    final String getHeaderIgnoreCase(String key) {
        for (Entry<String, String> header : headers.entrySet()) {
            if (header.getKey().equalsIgnoreCase(key)) {
                return header.getValue();
            }
        }
        return null;
    }

    final void removeHeaderIgnoreCase(String key) {
        Iterator<Entry<String, String>> it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> header = it.next();
            if (header.getKey().equalsIgnoreCase(key)) {
                it.remove();
                return;
            }
        }
    }

    final String getContent() {
        return content;
    }

    @Override
    public final byte[] dump() {
        StringBuilder sb = new StringBuilder();

        sb.append(HTTP_VERSION).append(' ').append(RESPONSE_CODE).append(' ').append("OK").append(TERMINATOR);
        for (Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(TERMINATOR);
        }

        sb.append(TERMINATOR); // split

        if (content != null) {
            sb.append(content);
        }

        return sb.toString().getBytes(Charset.forName("US-ASCII"));
    }

}