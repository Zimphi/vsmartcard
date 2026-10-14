package com.vsmartcard.acardemulator.cloud;
import org.junit.Test;
import static org.junit.Assert.*;

public class PairingTest {
    private final String code = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    @Test public void acceptsOneTimeHttpsPairing() throws Exception {
        Pairing p = new Pairing("phoenix://pair?server=https%3A%2F%2Fpreview.epassport-simulator.de&code="+code);
        assertEquals("https://preview.epassport-simulator.de",p.server);
        assertEquals(code,p.code);
    }
    @Test public void rejectsAmbiguousAndUnsafeEnvelopes() throws Exception {
        for (String uri : new String[]{
            "phoenix://pair?server=http%3A%2F%2Fexample.org&code="+code,
            "phoenix://pair?server=https%3A%2F%2Fuser%3Apass%40example.org&code="+code,
            "phoenix://pair?server=https%3A%2F%2Fexample.org%2Fpath&code="+code,
            "phoenix://pair?server=https%3A%2F%2Fexample.org&code="+code+"&code="+code,
            "phoenix://pair?server=https%3A%2F%2Fexample.org&code=short",
            "phoenix://pair?server=https%3A%2F%2Fexample.org&code="+code+"&token=stolen",
            "phoenix://pair?server=https%3A%2F%2Fexample.org&code="+code+"#fragment"}) {
            try {new Pairing(uri);fail("Unsafe QR accepted");} catch (IllegalArgumentException expected) {}
        }
    }
}
