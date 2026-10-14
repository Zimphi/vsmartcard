package com.vsmartcard.acardemulator.cloud;
import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;
final class CloudLanguage {
    static String language(Context c){
        String saved=c.getSharedPreferences("phoenix-ui",0).getString("language","");
        if(saved.equals("de")||saved.equals("en"))return saved;
        return "en".equals(Locale.getDefault().getLanguage())?"en":"de";
    }
    static Context wrap(Context context){
        Configuration config=new Configuration(context.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(language(context)));return context.createConfigurationContext(config);
    }
    static void save(Context c,String language){
        if(!language.equals("de")&&!language.equals("en"))throw new IllegalArgumentException();
        c.getSharedPreferences("phoenix-ui",0).edit().putString("language",language).putBoolean("language_dirty",true).apply();
        if(android.os.Build.VERSION.SDK_INT>=33)c.getSystemService(android.app.LocaleManager.class).setApplicationLocales(android.os.LocaleList.forLanguageTags(language));
    }
}
