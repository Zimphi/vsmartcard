package com.vsmartcard.acardemulator.cloud;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class CloudDeviceTest {
    @Test public void jsonPairingUsesTheSameHttpsAndOneTimeCodeValidation() throws Exception {
        String json=new JSONObject().put("format","phoenix-phone-pairing-v1").put("server","https://example.org")
            .put("pairing_code","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").toString();
        Pairing pair=Pairing.readJson(new java.io.ByteArrayInputStream(json.getBytes("UTF-8")));
        assertEquals("https://example.org",pair.server);
        for(String invalid:new String[]{new JSONObject(json).put("server","http://example.org").toString(),new JSONObject(json).put("bridge_token","not-accepted").toString(),new String(new char[2049]).replace('\0',' ')}){
            try{Pairing.readJson(new java.io.ByteArrayInputStream(invalid.getBytes("UTF-8")));fail("invalid JSON pairing accepted");}
            catch(Exception expected){}
        }
    }
    @Test public void keystoreRoundTripNeverPersistsPlaintextToken() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        DeviceVault vault=new DeviceVault(context);JSONObject original=vault.load();
        String token="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        try{
            vault.save("https://example.org",token);
            assertEquals(token,vault.load().getString("token"));
            for(Object value:context.getSharedPreferences("phoenix-cloud",Context.MODE_PRIVATE).getAll().values())
                assertFalse(String.valueOf(value).contains(token));
            vault.clear();assertNull(vault.load());
        }finally{if(original!=null)vault.save(original.getString("server"),original.getString("token"));else vault.clear();}
    }
    private static final class FakeApi extends CloudApi {
        final List<String> calls=new ArrayList<>();
        long revision;
        boolean uncertain;
        JSONObject statusPacket;
        String id="12345678-1234-4234-8234-123456789abc";
        FakeApi(){super("https://example.org",null);}
        @Override JSONObject post(String endpoint,JSONObject body)throws Exception{
            if(endpoint.equals("status")){calls.add("status");return statusPacket!=null?statusPacket:new JSONObject().put("session_id",id).put("active",true);}
            if(endpoint.equals("open")){calls.add("open");revision=0;return new JSONObject().put("session_id",id);}
            String op=body.getString("operation");calls.add(op);
            if(uncertain&&op.equals("apdu"))throw new java.io.IOException();
            assertEquals(revision,body.getJSONObject("session").getLong("revision"));
            return new JSONObject().put("session",new JSONObject().put("id",id).put("revision",++revision).put("checkpoint","sealed"))
                .put("apdu_response","9000");
        }
    }
    @Test public void profileMetadataIsOptionalAndClearedWhenServerRemovesSelection()throws Exception{
        FakeApi api=new FakeApi();api.heartbeat(new JSONObject());
        assertTrue(api.profile.active);assertNull(api.profile.id);
        api.statusPacket=new JSONObject().put("session_id",api.id).put("active",true).put("profile",
            new JSONObject().put("profile_id","erika-mustermann").put("kind","eid").put("version",1));
        api.heartbeat(new JSONObject());assertEquals("erika-mustermann",api.profile.id);assertEquals("eid",api.profile.kind);
        api.statusPacket.getJSONObject("profile").put("kind","unknown");
        api.heartbeat(new JSONObject());assertNull(api.profile.id);
        api.statusPacket.put("active",false);api.heartbeat(new JSONObject());
        assertFalse(api.profile.active);assertNull(api.profile.id);
    }
    @Test public void deviceAccountShowsOnlyPairedOwnerDisplayAndSupportedLanguage()throws Exception{
        FakeApi api=new FakeApi();
        api.statusPacket=new JSONObject().put("session_id",api.id).put("active",false)
            .put("account",new JSONObject().put("display_name","Test User").put("language","en"));
        api.heartbeat(new JSONObject());assertEquals("Test User",api.account.name);assertEquals("en",api.account.language);
        api.statusPacket.getJSONObject("account").put("language","unknown");api.heartbeat(new JSONObject());assertNull(api.account.language);
        api.statusPacket.remove("account");api.heartbeat(new JSONObject());assertNull(api.account);
    }
    @Test public void cardSessionMountsOnceAndAdvancesEveryApdu()throws Exception{
        FakeApi api=new FakeApi();
        assertArrayEquals(new byte[]{(byte)0x90,0},api.apdu(new byte[]{0, (byte)0xa4,4,0}));
        api.apdu(new byte[]{0,(byte)0xb0,0,0});
        assertEquals(java.util.Arrays.asList("open","mount","apdu","apdu"),api.calls);
        api.end();assertEquals("close",api.calls.get(api.calls.size()-1));
    }
    @Test public void uncertainApduIsNotRetried()throws Exception{
        FakeApi api=new FakeApi();api.uncertain=true;
        try{api.apdu(new byte[]{0,(byte)0xa4,4,0});fail("uncertain response accepted");}catch(java.io.IOException expected){}
        assertEquals(java.util.Arrays.asList("open","mount","apdu"),api.calls);
    }
    @Test public void changedProfileDiscardsTheOldCardSession()throws Exception{
        FakeApi api=new FakeApi();api.apdu(new byte[]{0,(byte)0xa4,4,0});
        api.id="22345678-1234-4234-8234-123456789abc";
        api.heartbeat(new JSONObject());api.apdu(new byte[]{0,(byte)0xa4,4,0});
        assertEquals(java.util.Arrays.asList("open","mount","apdu","status","open","mount","apdu"),api.calls);
    }
}
