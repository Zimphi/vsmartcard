package com.vsmartcard.acardemulator.cloud;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;
public class DiagnosticEvidenceTest {
    @Test public void exportUsesAnAllowlistAndNeverIncludesFreeformSecrets()throws Exception{
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONObject report=LabControl.device().put("test_id",java.util.UUID.randomUUID().toString()).put("kind","eid")
            .put("mode","reader").put("source","phone-reader").put("passed",true).put("attempts",10).put("successes",10)
            .put("checks",new JSONArray().put("application_selection").put("read_card_access")).put("tested_at_ms",System.currentTimeMillis())
            .put("token","PLANTED_TOKEN").put("pin","PLANTED_PIN").put("apdu","PLANTED_APDU").put("profile_id","PLANTED_DOCUMENT");
        JSONObject clean=DiagnosticEvidence.clean(report);assertTrue(DiagnosticEvidence.eligible(clean));
        assertFalse(clean.has("token"));assertFalse(clean.has("pin"));assertFalse(clean.has("apdu"));assertFalse(clean.has("profile_id"));
        CloudLog.add("EVIL_TAG_SECRET","PLANTED_LOG_SECRET",true);
        String exported=DiagnosticExport.snapshot(context).toString();
        assertFalse(exported.contains("PLANTED_"));assertFalse(exported.contains("EVIL_TAG_SECRET"));
        assertTrue(exported.contains("app_version"));assertTrue(exported.length()<65536);
        clean.put("checks",new JSONArray().put("pace"));assertFalse(DiagnosticEvidence.eligible(clean));
        clean.put("mode","card").put("source","cloud-eac");assertFalse(DiagnosticEvidence.eligible(clean));
    }
    @Test public void storedEvidenceIsBoundedAndVersionSpecific()throws Exception{
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.SharedPreferences prefs=context.getSharedPreferences("phoenix-diagnostics",0);String original=prefs.getString("records",null);
        try {
            prefs.edit().remove("records").commit();DiagnosticEvidence store=new DiagnosticEvidence(context);
            for(int i=0;i<25;i++)store.record(LabControl.device().put("test_id",java.util.UUID.randomUUID().toString()).put("kind","eid")
                .put("mode","reader").put("source","phone-reader").put("passed",false).put("attempts",1).put("successes",0)
                .put("checks",new JSONArray()).put("tested_at_ms",System.currentTimeMillis()));
            assertEquals(20,store.records().length());assertNotNull(store.latest("reader","eid"));
            assertNull(store.latest("card","eid"));assertNull(store.latest("reader","epassport"));
            JSONObject changed=LabControl.device().put("test_id",java.util.UUID.randomUUID().toString()).put("kind","epassport")
                .put("mode","card").put("source","cloud-eac").put("passed",true).put("attempts",1).put("successes",1)
                .put("checks",new JSONArray()).put("tested_at_ms",System.currentTimeMillis()).put("app_version","older-build");
            store.record(changed);assertNull(store.latest("card","epassport"));
        }finally{if(original==null)prefs.edit().remove("records").commit();else prefs.edit().putString("records",original).commit();}
    }
}
