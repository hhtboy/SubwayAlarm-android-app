package com.example.subway_alarm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.subway_alarm.ui.activities.MainActivity
import java.util.*

class AlarmService: Service() {

    private var isServiceOn = false
    private var timerTask: TimerTask? = null

    companion object {
        const val SERVICE_ID = 1
        const val START_FOREGROUND = "START"
        const val STOP_FOREGROUND = "STOP"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("새로운 intent : $intent / $this")

        val time = intent?.getIntExtra("time", 0)?:0
        when(intent?.action){
            START_FOREGROUND -> {
                // 실행 중인 알람이 없을 때
                if( !isServiceOn )
                    startForegroundService(time)

                // 실행 중인 알람이 있을 때
                else  {
                    timerTask?.cancel()
                    startForegroundService(time)
                }
            }

            STOP_FOREGROUND -> {
                if( isServiceOn )
                    stopForegroundService()
                else stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundService(time: Int) {
        val notificationBuilder: NotificationCompat.Builder = AlarmNotification.createAlarmNotification(this)
        val notiManager = getSystemService(NotificationManager::class.java)

        val timer = Timer()
        var timeCount = time
        println(timeCount)
        isServiceOn = true
         startForeground(SERVICE_ID, notificationBuilder.build())

        timerTask = object : TimerTask() {
            override fun run() {
                if (timeCount <= 0) {
                    notificationBuilder
                        .setContentText("설정한 시간이 되었습니다!")
                        .setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .clearActions()
                    notiManager.notify(SERVICE_ID, notificationBuilder.build())
                    this.cancel()
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                    return
                }
                notificationBuilder
                    .setContentText("${(timeCount) / 60}분 ${(timeCount) % 60}초 후에 알람이 울립니다.")

                notiManager.notify(SERVICE_ID, notificationBuilder.build())
                timeCount --
            }
        }

        timer.schedule(timerTask, 0, 1000)

    }

    private fun stopForegroundService(){
        sendMessage()
        isServiceOn = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendMessage() {
        val intent = Intent("alarm")
        intent.putExtra("alarmStop", true)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        println("service 종료 : $this")
        timerTask?.cancel()
    }
}