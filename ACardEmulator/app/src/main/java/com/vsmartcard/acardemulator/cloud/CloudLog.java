package com.vsmartcard.acardemulator.cloud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Bounded diagnostic metadata; never pass credentials, APDU bodies or exception messages. */
final class CloudLog {
    static final class Entry {
        final long time=System.currentTimeMillis();
        final String tag,message;final int messageResource;final boolean error;
        Entry(String tag,String message,boolean error){this.tag=tag;this.message=message;this.messageResource=0;this.error=error;}
        Entry(String tag,int resource,boolean error){this.tag=tag;this.message=null;this.messageResource=resource;this.error=error;}
    }
    private static final ArrayDeque<Entry> entries=new ArrayDeque<>();
    static synchronized void add(String tag,String message,boolean error){
        entries.addLast(new Entry(tag,message,error));while(entries.size()>120)entries.removeFirst();
    }
    static synchronized void add(String tag,int resource,boolean error){entries.addLast(new Entry(tag,resource,error));while(entries.size()>120)entries.removeFirst();}
    static synchronized List<Entry> snapshot(){return new ArrayList<>(entries);}
}
