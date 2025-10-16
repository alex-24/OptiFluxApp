package com.acassion.optifluxapp

import android.app.Application
import com.getkeepsafe.relinker.ReLinker

class OptiFluxApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        //System.loadLibrary("optifluxapp")
        ReLinker.loadLibrary(this, "optifluxapp")
    }
}