package com.searchengine.util;

import lombok.experimental.UtilityClass;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.TreeMap;

@UtilityClass
public class UrlUtils {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final Set<String> PRIVATE_HOSTNAMES = Set.of(
        "localhost", "127.0.0.1", "::1", "0.0.0.0"
    );

    public static String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        URI uri;
        try {
            uri = new URI(rawUrl.strip()).normalize();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL: " + rawUrl);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported URL scheme: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host: " + rawUrl);
        }

        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        } else {
            if (path.endsWith("/") && path.length() > 1) {
                path = path.replaceAll("/+$", "");
            }
        }

        String query = normalizeQuery(uri.getRawQuery());

        int port = uri.getPort();
        String lowerScheme = scheme.toLowerCase();
        if ((lowerScheme.equals("http") && port == 80)
            || (lowerScheme.equals("https") && port == 443)) {
            port = -1;
        }

        try {
            URI normalized = new URI(
                lowerScheme,
                null,
                host.toLowerCase(),
                port,
                path,
                query,
                null
            );
            return normalized.toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not normalize URL: " + rawUrl);
        }
    }

    public static void checkSsrf(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL: " + url);
        }

        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("URL has no host");
        }

        if (PRIVATE_HOSTNAMES.contains(host.toLowerCase())) {
            throw new IllegalArgumentException("Requests to private addresses are not allowed");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("Cannot resolve host: " + host);
            }
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new IllegalArgumentException("Requests to private/reserved IP ranges are not allowed");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress()
            || address.isSiteLocalAddress()
            || address.isLinkLocalAddress()
            || address.isAnyLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (b.length == 4) {
            int o0 = b[0] & 0xFF;
            int o1 = b[1] & 0xFF;
            // 100.64.0.0/10 CGNAT
            if (o0 == 100 && o1 >= 64 && o1 <= 127) return true;
            // 192.0.0.0/24, 192.0.2.0/24, 198.18.0.0/15, 198.51.100.0/24, 203.0.113.0/24 (reserved/test)
            if (o0 == 192 && o1 == 0) return true;
            if (o0 == 198 && (o1 == 18 || o1 == 19)) return true;
            // 0.0.0.0/8
            if (o0 == 0) return true;
        } else if (b.length == 16) {
            int first = b[0] & 0xFF;
            // fc00::/7 unique local addresses
            if ((first & 0xFE) == 0xFC) return true;
        }
        return false;
    }

    private static String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        TreeMap<String, String> params = new TreeMap<>();
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                params.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else if (!pair.isBlank()) {
                params.put(pair, "");
            }
        }
        if (params.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(k);
            if (!v.isEmpty()) sb.append('=').append(v);
        });
        return sb.toString();
    }

    public static String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
