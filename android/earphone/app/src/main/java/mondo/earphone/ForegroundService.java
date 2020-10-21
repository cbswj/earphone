package mondo.earphone;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mondo.earphone.home.HomeActivity;




public class ForegroundService extends Service {
    private final String CHANNEL_ID = appInfo.SMARTFARM_CHANNEL_ID;
    private Date date; // 푸쉬알림 보낼시의 시간

    private MediaRecorder mRecorder=null;

    private double volume = 10000;
    private double db=0;


    private Thread readThread; // 데시벨 읽는 쓰레드
    private Thread saveThread; // 데시벨 읽는 쓰레드


    private saveRunnable saverun;

    private WavRecorder wavRecorder;



    // 내부 저장소
    int log_count;
    SharedPreferences detectLog;

    // s3
    private final String BUCKET_NAME = "wav-earphone"; // S3 버킷 이름 (저장소 이름)
    private CognitoCachingCredentialsProvider credentialsProvider; // 자격 증명 풀
    private TransferObserver uploadObserver; // 파일 업로드 시 모니터링을 하기 위한 객체


    private  DatabaseReference   reference;





    // 시스템은 서비스가 처음 생성되었을 때(즉 서비스가 onStartCommand() 또는 onBind()를 호출하기 전에)
    // 이 메서드를 호출하여 일회성 설정 절차를 수행합니다. 서비스가 이미 실행 중인 경우, 이 메서드는 호출되지 않음음    @Override
    public void onCreate() {
        super.onCreate();

        reference = FirebaseDatabase.getInstance().getReference();


        detectLog = getApplicationContext().getSharedPreferences("detectLog", Activity.MODE_PRIVATE);
        log_count = detectLog.getInt("detectLog_length", 0); // 가져왔는데 없으면 기본값 -1

        // 광클로 인한 서비스 버그 방지용
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HomeActivity.on_condition = true;
                HomeActivity.off_condition = false;
            }
        }, 2000);


        // AWS 자격인증을 얻는 코드
        // Cognito를 이용하면 개발자 인증서를 앱에 직접 심지 않아도 되어 apk가 털려서 인증서가 유출 될 위험이 없다.
        // 개발자 인증 자격 증명을 사용하면 를 사용하여 사용자 데이터를 동기화하고 AWS 리소스에 액세스하면서도
        // 자신의 기존 인증 프로세스를 통해 사용자를 등록 및 인증할 수 있다.
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-2:6d58538f-d438-48f3-bd0e-dc7455119ea3", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2  // 물리적인 저장 위치 서울
        );


        reference.child("Result").child(appInfo.android_id).addChildEventListener(new ChildEventListener() {  //데이터 변화 감지중 (러닝결과 언제오나)
            @Override public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                chatListener(dataSnapshot);
            }

            @Override public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    // 서비스는 기본적으로 앱이 구동되는 프로세스의 메인 쓰레드에서 실행된다. 다시 말해서,
    // 서비스는 별도의 프로세스나 쓰레드에서 실행되는 것이 아니기 때문에
    // 서비스에서 CPU 사용량이 많은 작업, 또는 mp3 재생이나 네크워킹과 같은 blocking 작업
    // 한마디로 실행 흐름을 막는 작업(blocking operation)을 하게 되면 액티비티의 동작이 느려질 수 있기 때문에
    // 서비스에서 별도의 쓰레드를 생성하여 그 안에서 작업이 수행되도록 구현해야 한다
    // 이렇게 별도의 쓰레드를 사용함으로써, 응답 지연 문제(ANR)를 예방하고,
    // 앱의 메인 스레드가 사용자와의 상호작용에 집중할 수 있도록 해줍니다. (앱을 사용중일시시)

    class readRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    getDb();
                    Log.i("삽입"," "+db);
                    if(db>=80){
                        Log.i("성공"," "+db);

                        long now = System.currentTimeMillis();
                        // 현재시간의 날짜 생성하기
                        date = new Date(now);

                        // 로그로 저장하기
                        saveLog(now, "경보기");

                        stopRecorder();
                        wavRecorder = new WavRecorder(appInfo.android_id+".wav");

                        // wav 파일 저장
                        saveThread = new Thread(saverun);
                        saveThread.start();
                        saveThread.join();
                        volume = 10000;
                        db=0;
                        startRecorder();
                    }
                    Thread.sleep(1000);
                }
            }catch (InterruptedException e) {
                System.out.println("interrupted 발생");
                e.printStackTrace();
            }
        }
    }


    class saveRunnable implements Runnable {
        @Override
        public void run() {
            saveWavFile();
        }
    }


    private void saveWavFile(){

//        // 녹음 끄기
//        mRecorder.stop();
//        mRecorder.release();
//        mRecorder = null;
//
//        // 녹음 다시 시작
//        startRecorder();



        wavRecorder.startRecording();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        Log.d("저장?","저장 시작 시간");
        File file = wavRecorder.stopRecording();

     //   awsS3Upload(file,appInfo.android_id+".wav");
        Drawable drawable = getResources().getDrawable(R.drawable.danger);

// drawable 타입을 bitmap으로 변경
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        NotificationSomethings(123,bitmap,"경보기 소리가 감지되었습니다");
    }








    // startService()를 호출함으로써 서비스 실행을 요청할 때, 시스템이 호출해주는 콜백 메소드이다.
    // 이 메소드가 실행되면 서비스는 started 상태가 되며, 후면(background)에서 작업을 수행한다.
    // 만약 이 메소드를 구현한다면, 서비스가 할 작업을 모두 마쳤을 때 서비스를 중지하기 위해
    // stopSelf()나 stopService()를 호출하는 부분도 구현해야 한다
    // 이유는 stopSelf()나 stopService()를 호출되기 전까지 서비스는 멈추지 않기 때문 (서비스를 사용하고 싶지 않을때는 대비해서)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {






        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        //OREO API 26 이상에서는 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // icon setting 유일한 필수 콘텐츠입니다 푸쉬 알림이 안됩니다.
            builder.setSmallIcon(R.drawable.push_logo); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남


        }else builder.setSmallIcon(R.mipmap.earphone5); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남

        // null 아니라는걸 확인
        assert notificationManager != null;


        // 푸쉬알림을 ID로 구분함
        notificationManager.notify(111, builder.build());


        // 앱이 포그라운드 서비스를 생성해야 하는 경우, 해당 앱은 startForegroundService()를 호출해야 합니다.
        // 이 메서드는 백그라운드 서비스를 생성하지만, 메서드가 시스템에 신호를 보내 서비스가 자체적으로 포그라운드로 승격될 것이라고 알려줌
        // 서비스가 생성되면 5초 이내에 startForeground() 메서드를 호출해야 합니다.
        // 호출하지 않으면 foreground 서비스가 제대로 작동하지 않아서 24시간 가동이 되지 않는다.

        startForeground(111, builder.build());

        // foregorundservice시 강제로 생기는 푸쉬 알림 cancel
        NotificationSomethings(111, null, null);



        startRecorder();


        saverun = new saveRunnable();



        // 데시벨 읽는 쓰레드
        readRunnable readrun = new readRunnable();
        readThread = new Thread(readrun);
        readThread.start();












        // 정수형의 값을 리턴해야 해야 하는 이유는
        // 시스템이 메모리가 부족하여 서비스를 종료한 이후에, 다시 여유가 생겼을 때
        // 서비스를 이어서 실행할 것인지 여부를 나타내며, 총 미리 선언된 3가지 정수가 있다.

        // START_NOT_STICKY : 값이 리턴된 후, 시스템이 서비스를 종료시켰다면, 다시 서비스가 실행될 수 있는 여건이 되더라도 서비스를 다시 생성하지 않습니다.

        // START_STICKY : onStartCommand()에서 이 값이 리턴된 후, 시스템이 서비스를 종료시켰다면, 다시 서비스가 실행될 수 있는 여건이 되었을 때,
        //                서비스를 생성하고 onStartCommand()를 호출해 줍니다 intent 값이 null로 되서 초기화 된다.

        // START_REDELIVER_INTENT : START_STICKY와 마찬가지로 Service가 종료 되었을 경우 시스템이 다시 Service를 재시작 시켜 주지만
        //                          intent 값을 그대로 유지 시켜 줍니다.



        return START_STICKY;
    }


    // 서비스가 더이상 사용되지 않아 종료될 때 호출되는 콜백 메소드입니다.
    // 이 메소드는 서비스의 생명주기에서 가장 마지막에 호출되는 콜백 메소드이기 때문에,
    // 여기에서는 서비스에서 사용하던 리소스들(쓰레드, 등록된 리스너, 리시버 등)을 모두 정리해줘야(clean up) 합니다.
    @Override
    public void onDestroy() {
        // 스레드가 일시 정지 상태에 있을 때 InterruptedException 예외를 발생시키는 역할
        //서비스 종료시 쓰레드 중지하기
        readThread.interrupt();
        stopRecorder();
        // 광클로 인한 서비스 버그 방지용
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HomeActivity.off_condition = true;
                HomeActivity.on_condition = false;
            }
        }, 2000);
        super.onDestroy();
    }



    // 마치 클라이언트-서버 와 같이 동작을 하는데 서비스가 서버 역할을 한다.
    // 결론: 서비스가 돌아가는 동안 지속적으로 액티비티와 커뮤니케이션 하기 위해 사용
    // 또한 bind타입 Service는 앱 내부의 기능을 외부로 제공할 때 사용한다.
    //  바인딩을 원하지 않으면 null 리턴
    // 앱 간의 통신 지원 가능하게 해줌 리턴값을 받을수 있다.


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




    private void NotificationSomethings(int notifyID,Bitmap webImg_bitmap, String content) {
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // 푸쉬 알림 터치 시 이동할 클래스 설정
        Intent notificationIntent = new Intent(this,  MainActivity.class);

        // 기존에 쌓여있던 스택을 모두 없애고 task를 새로 만든다. (activity를 새로 만든다.)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;

        // PendingIntent는 Intent를 가지고 있는 클래스로, 기본 목적은 다른 애플리케이션(다른 프로세스)의
        // 권한을 허가하여 가지고 있는 Intent를 마치 본인 앱의 프로세스에서 실행하는 것처럼 사용하게 하는 것입니다.
        // Notification으로 작업을 수행할 때 인텐트가 실행되도록 합니다.
        // Notification은 안드로이드 시스템의 NotificationManager가 Intent를 실행합니다.
        // 즉 다른 프로세스에서 수행하기 때문에 Notification으로 Intent수행시 PendingIntent의 사용이 필수 입니다.
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,  PendingIntent.FLAG_UPDATE_CURRENT);

        // 푸쉬알림 임시로 만들었을때 시간 설정 (forgroundsercie라 어쩌피 지울 푸쉬 알림)
        if(notifyID==111){
            date = new Date(System.currentTimeMillis());
        }

        // NotificationCompat는 푸쉬 알림을 만드는 최신버전의 라이브러리리
        // 생성자의 경우 채널 ID를 제공해야 한다.
        // Android 8.0(API 수준 26) 이상에서는 호환성을 유지하기 위해 필요하지만 이전 버전에서는 무시된다.




        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(content)
                .setContentText("소리가 감지되었습니다")
                .setPriority(Notification.PRIORITY_HIGH) // 알람의 우선순위 최고
                .setContentIntent(pendingIntent) // 사용자가 노티피케이션을 탭시 pendingIntent로 이동하도록 설정
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(date.getTime()) // 시간 표시
                .setLargeIcon(webImg_bitmap) // 푸쉬알림 오른쪽 사진 등록
                .setStyle(new NotificationCompat.BigPictureStyle()  // 푸쉬알림 밑에 화살표 터치시 보여줄 큰 사진 등록
                        .bigPicture(webImg_bitmap)
                        .bigLargeIcon(null))
                .setAutoCancel(true); // 푸쉬 알림을 터치하고 난 후에는 지우는 옵션




        //OREO API 26 이상에서는 푸쉬알림 생성 시 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            builder.setSmallIcon(R.drawable.push_logo); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남


        }else builder.setSmallIcon(R.mipmap.earphone5); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남

        // null 아니라는걸 확인
        assert notificationManager != null;
        notificationManager.notify(notifyID, builder.build()); // 고유숫자로 노티피케이션 동작시킴

        // 푸쉬알림 임시로 만든것 삭제 (foreground 서비스를 background 처럼 이용하기 위한 편법법
        if(notifyID==111) {
            notificationManager.cancel(notifyID);
        }
    }

    //미디어 레코드 선언
    public void startRecorder(){
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile("/dev/null"); //출력 버리기?
        try {
            mRecorder.prepare(); //recording을 준비하는 단계
        } catch (java.io.IOException ioe) {
            android.util.Log.e("[Monkey]", "IOException: " + android.util.Log.getStackTraceString(ioe));

        } catch (java.lang.SecurityException e) {
            android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
        }
        try {
            mRecorder.start(); //start 함수를 호출하면 정해놓은 audio/video source 에서 실제 레코딩을 시작하게 되고, state 는 Recording 상태가 됩니다.
        } catch (java.lang.SecurityException e) {
            android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
        }

    }

    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getDb() { //쓰레드는 그대로 쓰고 메소드부분만 다른거로 바꿈
        volume = mRecorder.getMaxAmplitude();
        if (volume > 0 && volume < 1000000) {
            db = 20 * (float)(Math.log10(volume));  //파형을 데시벨로 변환
        }
        return db;
    }

    // 내부저장소에 로그 남기기
    private void saveLog(long time, String content){
        SharedPreferences.Editor editor = detectLog.edit();
        editor.putLong("detect_time"+log_count, time); // 버튼 꺼져있는 상태로 저장
        Log.d("확인하겠습낟.","확인 : "+content);
        editor.putString("detect_content"+log_count, content); // 버튼 꺼져있는 상태로 저장
        editor.putInt("detectLog_length",++log_count); // log 숫자 추가하고 넣기
        editor.apply();
    }


    //  S3 사진 업로드 함수
    private void awsS3Upload(File wavFile, String fileName){

        reference = FirebaseDatabase.getInstance().getReference().child("UID").child(appInfo.android_id);   // 123은 나중에 실 휴대폰 고유 아이디 쓸예정   서버 반응하기 위해 데베 생성

        // AWS s3를 이용하기 위해 생성된 인증 자격을 통하여 객체를 생성하고
        // 데이터를 송수신 하기 위해 TransferUtility 객체를 생성한다.
        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());

        // upload 메서드가 리턴하는 걸 TransferObserver라는 객체에다가 던져준 다음 이걸로
        // TransferListener를 만들면 업로드 현황을 모니터링하면서 몇 퍼센트 업로드가 완료됐는지,
        // 또는 무엇 때문에 업로드에 실패했는지 등을 알 수가 있다.

        uploadObserver = transferUtility.upload(
                BUCKET_NAME,    // 업로드할 버킷 이름
                "wavfile/"+fileName,    // 버킷에 저장할 파일의 이름 확장자명도 붙여줘야댐 png (이름이 key로 쓰임)
                wavFile       // 버킷에 저장할 파일
        );

        //전송 상태 또는 진행률이 변경 될 때 알림을받는 데 사용되는 리스너
        uploadObserver.setTransferListener(new TransferListener() {

            // 전송 상태가 변경 될 때 호출됩니다.
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    // 람다 함수 호출
                    Log.d("저장","저장완료");

                    Map<String, Object> map = new HashMap<>();
                    String Key = reference.push().getKey();
                    reference.updateChildren(map);

                    DatabaseReference root = reference.child(Key);
                    Map<String, Object> objectMap = new HashMap<String, Object>();
                    objectMap.put("FileName", appInfo.android_id+".wav");    // 파일 다올라가면 데베에 파일이름을 올려서 서버에게 반응시킴  서버는 그 파일 이름 받고 다운받고 러닝 돌리고 데베에 올려줌
                    root.updateChildren(objectMap);


                }
            }

            // 전송 중일떄 호출
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            // 전송 중 에러시
            @Override
            public void onError(int id, Exception ex) {

            }
        });

    }


    private void chatListener(DataSnapshot dataSnapshot) { // 러닝한 결과 가져다 주는곳
        Iterator i = dataSnapshot.getChildren().iterator();



        // 밑에 로그 주석풀면 String result 문 에러남 i.next 가 두개라 데아터가 하나인데 next 두번해서 에러
        //Log.d(   dataSnapshot.getValue().toString(), ((DataSnapshot) i.next()).getValue().toString());  //((DataSnapshot) i.next()).getValue().toString() 러징 결과 =? 0 1 2  => 추후에 %작업 서버에서 할듯듯
        String result =(String) ((DataSnapshot) i.next()).getValue();

        switch (result){
            case"0":
                result="경적 소리";
                break;

            case"1":
                result="엔진 소리";

                break;
            case"2":
                result="사이렌 소리";
                break;
        }
        Toast.makeText(getApplicationContext(),  "러닝 결과 "+result, Toast.LENGTH_SHORT).show();  // 0 = 자동차 경적 , 1= 엔진  , 2=사이렌
    }


}