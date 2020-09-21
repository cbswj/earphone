package com.example.earphone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView mStatusView;
    private MediaRecorder mRecorder;
    private Thread runner;
    private double volume = 10000;
    private double db=0;
    private Button start;
    private Button stop;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;

    final Runnable updater = new Runnable(){

        public void run(){
            updateTv(); //여기 이벤트 바꾸면 될꺼같음
            Log.i("삽입"," "+db);
            if(db>=80){
                Log.i("성공"," "+db);
                //녹음 코드
            }
        };
    };
    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatusView =(TextView) findViewById(R.id.tv);
        start = (Button)findViewById(R.id.btn1);
        stop = (Button)findViewById(R.id.btn2);

        start.setOnClickListener(new View.OnClickListener() {
            @Override //서비스 실행
            public void onClick(View view) {
                startService(new Intent(getApplicationContext(),Earphone_Service.class));
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override //서비스 종료
            public void onClick(View view) {
                startService(new Intent(getApplicationContext(),Earphone_Service.class));
            }
        });

        if (runner == null)
        {
            runner = new Thread(){
                public void run()
                {
                    while (runner != null)
                    {
                        try
                        {
                            Thread.sleep(1000);
                            Log.i("Noise", "Tock");
                        } catch (InterruptedException e) { };
                        mHandler.post(updater);
                    }
                }
            };
            runner.start();
            Log.d("Noise", "start runner()");
        }
    }
    public void onResume()
    {
        super.onResume();  //생명주기 Resume 에 레코드를 사용
        startRecorder();
    }

    public void onPause()
    {
        super.onPause(); //Pause에 사용 중지
        stopRecorder();
    }

    public void startRecorder(){  //미디어 레코드 선언
        if (mRecorder == null)
        {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null"); //출력 버리기?
            try
            {
                mRecorder.prepare(); //recording을 준비하는 단계
            }catch (java.io.IOException ioe) {
                android.util.Log.e("[Monkey]", "IOException: " + android.util.Log.getStackTraceString(ioe));

            }catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
            }
            try
            {
                mRecorder.start(); //start 함수를 호출하면 정해놓은 audio/video source 에서 실제 레코딩을 시작하게 되고, state 는 Recording 상태가 됩니다.
            }catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " + android.util.Log.getStackTraceString(e));
            }
        }

    }
    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void updateTv(){
        mStatusView.setText(Double.toString((getDb())) + " dB");
    }


    public double soundDb(double ampl){ //데시벨 구하는 로직 이상해서 버림
        return  20 * Math.log10(getAmplitudeEMA() / ampl);
    }

    public double getDb() { //쓰레드는 그대로 쓰고 메소드부분만 다른거로 바꿈
        volume = mRecorder.getMaxAmplitude();
        if (volume > 0 && volume < 1000000) {
            db = 20 * (float)(Math.log10(volume));  //파형을 데시벨로 변환
        }
        return db;
    }
    public double getAmplitude() { // 미디어 레코드의 getMaxamplitude는 녹음 파일중 최대 진폭 double로 반환
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude()/32767);
        else
            return 0;

    }
    public double getAmplitudeEMA() { //이건 진짜 모르겠음 안되서 이것도 안씀
        double amp =  getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }
}