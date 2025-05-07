package com.saza.intellical

import androidx.multidex.MultiDexApplication
import com.saza.commons.extensions.checkUseEnglish

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }
}
