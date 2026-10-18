package com.vsmartcard.acardemulator.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

final class DeviceVault {
    private static final String ALIAS = "phoenix-cloud-device-v1";
    private final SharedPreferences prefs;
    DeviceVault(Context context) { prefs = context.getSharedPreferences("phoenix-cloud",Context.MODE_PRIVATE); }
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build()); gen.generateKey();
        }
        return (SecretKey)store.getKey(ALIAS,null);
    }
    synchronized void save(String server,String token) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] data=new JSONObject().put("server",server).put("token",token).toString().getBytes("UTF-8");
        if (!prefs.edit().putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
            .putString("sealed",Base64.encodeToString(cipher.doFinal(data),Base64.NO_WRAP)).commit()) throw new IllegalStateException();
    }
    synchronized JSONObject load() throws Exception {
        if (!prefs.contains("sealed")) return null;
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),
            new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(prefs.getString("sealed",""),Base64.NO_WRAP)),"UTF-8"));
    }
    synchronized void clear() { if(!prefs.edit().clear().commit())throw new IllegalStateException("Pairing could not be removed"); }
}
