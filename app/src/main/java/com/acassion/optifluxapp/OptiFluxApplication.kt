package com.acassion.optifluxapp

import android.app.Application
import com.getkeepsafe.relinker.ReLinker

class OptiFluxApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        //System.loadLibrary("optical_flow_calculator")
        ReLinker.loadLibrary(this, "optical_flow_calculator")
    }
}