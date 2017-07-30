package com.hack.airmonitor;

import android.app.Application;

import io.rapid.Rapid;

/**
 * Created by peterma on 7/29/17.
 */

public class AirMon extends Application {
    @Override
    public void onCreate ()
    {
        super.onCreate();
        Rapid.initialize("NDA1OWE0MWo1b3AzY2Q5LnJhcGlkLmlv");

    }

}
