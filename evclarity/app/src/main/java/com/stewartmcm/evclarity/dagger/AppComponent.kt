package com.stewartmcm.evclarity.dagger

import com.stewartmcm.evclarity.activity.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(target: MainActivity)
}