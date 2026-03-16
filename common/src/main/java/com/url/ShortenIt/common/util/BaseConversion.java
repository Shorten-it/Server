package com.url.ShortenIt.common.util;

import org.springframework.stereotype.Component;

@Component
public class BaseConversion {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public String encode(long num) {
        if (num == 0) return String.valueOf(BASE62.charAt(0));
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62.charAt((int)(num % 62)));
            num /= 62;
        }
        return sb.reverse().toString();
    }

    public long decode(String str) {
        long num = 0;
        for (char c : str.toCharArray()) {
            num = num * 62 + BASE62.indexOf(c);
        }
        return num;
    }
}
