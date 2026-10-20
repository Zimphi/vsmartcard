package com.vsmartcard.acardemulator.cloud;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class CloudDeviceTest {
    @Test public void emptyReadBinaryIsNotEvidenceThatCardAccessWasRead(){
        byte[] read={0,(byte)0xB0,0,0,0},select={0,(byte)0xA4,4,0};
        assertFalse(PhoneReader.validProbeResponse(read,new byte[]{(byte)0x90,0}));
        assertTrue(PhoneReader.validProbeResponse(read,new byte[]{0x31,0,(byte)0x90,0}));
        assertTrue(PhoneReader.validProbeResponse(select,new byte[]{(byte)0x90,0}));
        assertFalse(PhoneReader.validProbeResponse(select,new byte[]{0x6A,(byte)0x82}));
    }
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
    @Test public void aLateRevocationCannotEraseANewerPairing()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();DeviceVault vault=new DeviceVault(context);JSONObject original=vault.load();
        try{
            vault.save("https://example.invalid","old-token");vault.save("https://example.invalid","new-token");
            new DeviceVault(context).clearIfMatches("https://example.invalid","old-token");assertEquals("new-token",vault.load().getString("token"));
            vault.clearIfMatches("https://example.invalid","new-token");assertNull(vault.load());
        }finally{if(original==null)vault.clear();else vault.save(original.getString("server"),original.getString("token"));}
    }
    private static class FakeApi extends CloudApi {
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
    private static final class SlowHeartbeatApi extends FakeApi {
        final java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release=new java.util.concurrent.CountDownLatch(1);
        @Override JSONObject post(String endpoint,JSONObject body)throws Exception{
            if(!endpoint.equals("status"))return super.post(endpoint,body);
            JSONObject captured=new JSONObject().put("session_id",id).put("active",true);
            calls.add("status");entered.countDown();
            if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new java.io.IOException();
            return captured;
        }
    }
    @Test public void heartbeatNetworkWaitDoesNotBlockTheNextApdu()throws Exception{
        SlowHeartbeatApi api=new SlowHeartbeatApi();api.apdu(new byte[]{0, (byte)0xa4,4,0});
        java.util.concurrent.ExecutorService tasks=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            java.util.concurrent.Future<?> beat=tasks.submit(()->{api.heartbeat(new JSONObject());return null;});
            assertTrue(api.entered.await(2,java.util.concurrent.TimeUnit.SECONDS));
            java.util.concurrent.Future<byte[]> command=tasks.submit(()->api.apdu(new byte[]{0,(byte)0xb0,0,0}));
            assertArrayEquals(new byte[]{(byte)0x90,0},command.get(1,java.util.concurrent.TimeUnit.SECONDS));
            api.release.countDown();beat.get(2,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1,java.util.Collections.frequency(api.calls,"open"));
        }finally{api.release.countDown();tasks.shutdown();tasks.awaitTermination(3,java.util.concurrent.TimeUnit.SECONDS);}
    }
    @Test public void lateHeartbeatCannotClearOrOverwriteANewerCardSession()throws Exception{
        SlowHeartbeatApi api=new SlowHeartbeatApi();api.apdu(new byte[]{0, (byte)0xa4,4,0});
        java.util.concurrent.ExecutorService tasks=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            java.util.concurrent.Future<?> beat=tasks.submit(()->{api.heartbeat(new JSONObject());return null;});
            assertTrue(api.entered.await(2,java.util.concurrent.TimeUnit.SECONDS));
            api.id="22345678-1234-4234-8234-123456789abc";
            tasks.submit(()->{api.begin();return null;}).get(1,java.util.concurrent.TimeUnit.SECONDS);
            api.release.countDown();beat.get(2,java.util.concurrent.TimeUnit.SECONDS);
            api.apdu(new byte[]{0,(byte)0xb0,0,0});
            assertEquals(2,java.util.Collections.frequency(api.calls,"open"));
            assertFalse(api.profile.active);
        }finally{api.release.countDown();tasks.shutdown();tasks.awaitTermination(3,java.util.concurrent.TimeUnit.SECONDS);}
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
        api.statusPacket.getJSONObject("account").put("display_name","admin@users.phoenix.invalid");
        api.heartbeat(new JSONObject());assertEquals("admin",api.account.name);
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
    @Test public void recoveryDiscardsOpaqueStateAndNeverReplaysTheOldApdu()throws Exception{
        FakeApi api=new FakeApi();api.apdu(new byte[]{0,(byte)0xa4,4,0});
        api.invalidate();api.apdu(new byte[]{0,(byte)0xb0,0,0});
        assertEquals(java.util.Arrays.asList("open","mount","apdu","open","mount","apdu"),api.calls);
    }
    @Test public void revokeRequiresAckButAnAlreadyDeniedTokenCanBeRemoved()throws Exception{
        CloudApi api=new CloudApi("https://example.invalid",null){@Override JSONObject post(String endpoint,JSONObject value)throws Exception{
            assertEquals("disconnect",endpoint);assertEquals(0,value.length());throw new Denied(true);
        }};api.revoke();
        CloudApi offline=new CloudApi("https://example.invalid",null){@Override JSONObject post(String endpoint,JSONObject value)throws Exception{throw new java.io.IOException();}};
        try{offline.revoke();fail("offline revocation claimed success");}catch(java.io.IOException expected){}
        CloudApi gateway=new CloudApi("https://example.invalid",null){@Override JSONObject post(String endpoint,JSONObject value)throws Exception{throw new Denied();}};
        try{gateway.revoke();fail("unconfirmed gateway denial claimed revocation");}catch(CloudApi.Denied expected){}
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
