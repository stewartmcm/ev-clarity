package com.stewartmcm.evclarity.dagger

import com.stewartmcm.evclarity.activity.MainActivity
import com.stewartmcm.evclarity.fragment.EnergySettingsFragment
import com.stewartmcm.evclarity.fragment.TripListFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(target: MainActivity)
    fun inject(target: TripListFragment)
    fun inject(target: EnergySettingsFragment)
}