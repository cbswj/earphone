package mondo.earphone;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import mondo.earphone.tmap.TmapActivity;

public class ForeTerminationService extends Service {


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) { //핸들링 하는 부분
        Log.e("Error","onTaskRemoved - 강제 종료 " + rootIntent);


        // 쓰레드 실행 중인경우 종료하기
        if(MainActivity.tmap_check) {
            TmapActivity.readThread.interrupt();
            TmapActivity.mRecorder.stop();
            TmapActivity.mRecorder.release();
            MainActivity.tmap_check=false;
        }


        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("UID").removeValue();
        reference.child("Result").removeValue();

        SharedPreferences detectLog = getApplicationContext().getSharedPreferences("detectMode", Activity.MODE_PRIVATE);
        int OnOff = detectLog.getInt("detectMode", -1); // 가져왔는데 없으면 기본값 -1

        // 켜져 있으면서 홈을 보고 있지 않으면
        if(OnOff==1&& !MainActivity.home_check) {
            Intent serviceIntent = new Intent(getApplicationContext(), ForegroundService.class);
            ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
        }
        stopSelf(); //서비스 종료
    }
}
