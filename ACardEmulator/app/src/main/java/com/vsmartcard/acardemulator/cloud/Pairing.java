package com.vsmartcard.acardemulator.cloud;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/** Public pairing envelope; never accepts an existing device token from a QR. */
public final class Pairing {
    public final String server, code;
    public Pairing(String value) throws Exception {
        if (value == null || value.length() > 2048) throw new IllegalArgumentException();
        URI uri = new URI(value);
        if (!"phoenix".equals(uri.getScheme()) || !"pair".equals(uri.getHost())
                || uri.getRawUserInfo() != null || uri.getPort() != -1 || uri.getRawFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty()) || uri.getRawQuery() == null)
            throw new IllegalArgumentException();
        Map<String,String> fields = new HashMap<>();
        for (String part : uri.getRawQuery().split("&")) {
            String[] bits = part.split("=", 2);
            if (bits.length != 2 || fields.put(URLDecoder.decode(bits[0], "UTF-8"),
                    URLDecoder.decode(bits[1], "UTF-8")) != null) throw new IllegalArgumentException();
        }
        if (fields.size() != 2 || !fields.containsKey("server") || !fields.containsKey("code")) throw new IllegalArgumentException();
        URI base = new URI(fields.get("server"));
        if (!"https".equals(base.getScheme()) || base.getHost() == null || base.getRawUserInfo() != null
                || (base.getPort() != -1 && base.getPort() != 443) || base.getRawQuery() != null
                || base.getRawFragment() != null || (!base.getPath().isEmpty() && !"/".equals(base.getPath()))
                || !fields.get("code").matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException();
        server = "https://" + base.getHost();
        code = fields.get("code");
    }
}
