package com.searchengine.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class SimHash {

    private static final int BITS = 64;
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    public static long compute(String text) {
        if (text == null || text.isBlank()) {
            return 0L;
        }

        int[] counters = new int[BITS];
        boolean hasToken = false;

        for (String token : text.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (token.isEmpty()) {
                continue;
            }
            hasToken = true;
            long hash = hash64(token);
            for (int bit = 0; bit < BITS; bit++) {
                if (((hash >>> bit) & 1L) == 1L) {
                    counters[bit]++;
                } else {
                    counters[bit]--;
                }
            }
        }

        if (!hasToken) {
            return 0L;
        }

        long simhash = 0L;
        for (int bit = 0; bit < BITS; bit++) {
            if (counters[bit] > 0) {
                simhash |= (1L << bit);
            }
        }
        return simhash;
    }

    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    private static long hash64(String token) {
        long hash = FNV_OFFSET_BASIS;
        for (byte b : token.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xffL);
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
