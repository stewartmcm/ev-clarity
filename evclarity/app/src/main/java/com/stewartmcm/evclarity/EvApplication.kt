package com.stewartmcm.evclarity

import android.app.Application
import com.stewartmcm.evclarity.dagger.AppComponent
import com.stewartmcm.evclarity.dagger.AppModule
import com.stewartmcm.evclarity.dagger.DaggerAppComponent

class EvApplication : Application() {

  lateinit var evComponent: AppComponent

  override fun onCreate() {
    super.onCreate()
    evComponent = initDagger(this)
  }

  private fun initDagger(app: EvApplication): AppComponent =
          DaggerAppComponent.builder()
                  .appModule(AppModule(app))
                  .build()

}