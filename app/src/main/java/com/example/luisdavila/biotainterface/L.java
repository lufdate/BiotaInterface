package com.example.luisdavila.biotainterface;

import android.util.Log;

public class L {
    private static final boolean SHUT_UP = false;

    public static void d(Object o){
    if(BuildConfig.DEBUG && !SHUT_UP)
        Log.d(">==< SimpleAccessory >==<",String.valueOf(o));
    }
    public static void (String s, Object ... args){
        if (BuildConfig.DEBUG && !SHUT_UP)
            Log.d(">==< SimpleAccessory >==<",String.format(s,args));
    }
    public static void e(Object o){
        if(BuildConfig.DEBUG && !SHUT_UP)
            Log.d(">==< SimpleAccessory >==<",String.valueOf(o));
    }
}
