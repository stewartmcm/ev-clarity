package com.stewartmcm.evclarity

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.stewartmcm.evclarity.dagger.AppComponent
import com.stewartmcm.evclarity.dagger.AppModule
import com.stewartmcm.evclarity.dagger.DaggerAppComponent
import timber.log.Timber

class EvApplication : Application() {

  lateinit var evComponent: AppComponent

  override fun onCreate() {
    super.onCreate()
    evComponent = initDagger(this)
    initNotificationChannels()
  }

  private fun initDagger(app: EvApplication): AppComponent =
          DaggerAppComponent.builder()
                  .appModule(AppModule(app))
                  .build()

  private fun initNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = getString(R.string.notification_channel_name_trip_summaries)
      val description = getString(R.string.notification_channel_description_trip_summaries)
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(Constants.TRIP_SUMMARY_NOTIFICATION_CHANNEL_ID, name, importance)
      channel.description = description
      val notificationManager = getSystemService(NotificationManager::class.java)
      notificationManager?.createNotificationChannel(channel) ?: Timber.e(getString(R.string.notification_manager_null_error))
    }
  }

}