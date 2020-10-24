package mondo.earphone;


import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;


public class appInfo extends Application {
    
    public static final String SMARTFARM_CHANNEL_ID = "69981";
    NotificationChannel channel; // 푸쉬 알림 채널 객체
    public static String android_id;

    public static CognitoCachingCredentialsProvider credentialsProvider; // 자격 증명 풀



    @Override
    public void onCreate() {

//        가장 명확한 방법이며, 디바이스가 최초 Boot 될때 생성 되는 64-bit 값이다. (공장 초기화 안하면 항상 그대로)
//        ANDROID_ID는 디바이스의 고유한 번호를 획득 하기 위해 가장 좋은 선택이 될 수 있음 다른것이 위험도가 더 높다고 한다.
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.i("디바이스 코드", android_id);

        CharSequence channelName  = "earphone channel";
        int importance = NotificationManager.IMPORTANCE_HIGH; // 긴급: 알림음이 울리며 헤드업 알림으로 표시됩니다.

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(SMARTFARM_CHANNEL_ID, channelName, importance);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);


            // 노티피케이션 채널을 시스템에 등록
            assert notificationManager != null;

            notificationManager.createNotificationChannel(channel);
        }

        // AWS 자격인증을 얻는 코드
        // Cognito를 이용하면 개발자 인증서를 앱에 직접 심지 않아도 되어 apk가 털려서 인증서가 유출 될 위험이 없다.
        // 개발자 인증 자격 증명을 사용하면 를 사용하여 사용자 데이터를 동기화하고 AWS 리소스에 액세스하면서도
        // 자신의 기존 인증 프로세스를 통해 사용자를 등록 및 인증할 수 있다.
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-2:6d58538f-d438-48f3-bd0e-dc7455119ea3", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2  // 물리적인 저장 위치 서울
        );

        super.onCreate();
    }



}
