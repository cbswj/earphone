package com.example.earphone;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class Earphone_Service extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //서비스는 한번 실행되면 계속 실행된 상태로 있는다.
        //따라서 서비스 특성상 intent를 받아서 처리하기에 적합하지않다
        //intent에 대한 처리는 onstartcommand에서 처리해준다

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){
            return Service.START_STICKY; //서비스가 종료되어도 자동으로 다시 실행?
        }else{

        }
        return super.onStartCommand(intent, flags, startId);
        //시작
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //서비스 끝 시점
    }
}
