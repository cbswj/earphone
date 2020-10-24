package mondo.earphone;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;


import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;


import android.text.TextUtils;

import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import mondo.earphone.home.HomeActivity;
import mondo.earphone.tmap.TmapActivity;

public class MainActivity extends AppCompatActivity   {

    private final long FINISH_INTERVAL_TIME = 2000; // 2초안에 뒤로가기 버튼 2번 누르면 꺼짐
    private long backPressedTime = 0;


    private BottomNavigationView bottomNavigationView; // 바텀 네비게이션 뷰

    private FragmentManager fm; // 프래그먼트를 전환하기 위한 객체
    private FragmentTransaction ft; //

    public HomeActivity home_frag; // 홈 프래그먼트

    SharedPreferences detectMode;
    int OnOff;

    public static boolean home_check;
    public static boolean tmap_check=false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 종료시점 알기 위한 서비스 시작
        startService(new Intent(this, ForeTerminationService.class));


        // 프래그먼트 전환을 위환 생성
        home_frag =new HomeActivity();


        // 권한 묻기기
        checkSelfPermission();


        //바텀네비게이션
        bottomNavigationView = findViewById(R.id.bottomNavi);

        //바텀네비게이션 이벤트
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
            {
                switch (menuItem.getItemId())
                {
                    case R.id.home:
                        setFrag(0);
                        break;
                    case R.id.gps:
                        setFrag(1);
                        break;
                }
                return true;
            }
        });

        // 앱 시작 시 화면은 home
        setFrag(0);




    }

    // 프레그먼트 교체
    private void setFrag(int n)
    {
        fm = getSupportFragmentManager();
        ft = fm.beginTransaction();
        detectMode = getApplicationContext().getSharedPreferences("detectMode", Activity.MODE_PRIVATE);
        OnOff = detectMode.getInt("detectMode", -1); // 가져왔는데 없으면 기본값 -1
        switch (n)
        {
            case 0:
                // tmap 쓰레드가 켜져 있다면
                if(tmap_check) {
                    TmapActivity.readThread.interrupt();
                    TmapActivity.mRecorder.stop();
                    TmapActivity.mRecorder.release();
                    tmap_check=false;

                    // 파이어베이스 삭제
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    reference.child("UID").removeValue();
                    reference.child("Result").removeValue();


                    SharedPreferences.Editor editor = detectMode.edit();
                    editor.putInt("tmapId_count",++TmapActivity.tmapId_count);
                    editor.apply();
                }


                // 앱 다시 켰을때 중복 실행 막기 위한 확인
                if (OnOff == 1 && !ForegroundService.current) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent serviceIntent = new Intent(getApplicationContext(), ForegroundService.class);
                            ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
                        }
                    }, 2000);
                }

                // 홈을 보고 있다.
                home_check=true;


                ft.replace(R.id.Main_Frame,home_frag);
                ft.addToBackStack(null);
                ft.commit();
                break;

            case 1:
                tmap_check=true;
                if(OnOff==1){
                    Intent serviceIntent = new Intent(getApplicationContext(), ForegroundService.class);
                    getApplicationContext().stopService(serviceIntent);

                }

                // 홈을 안 보고 있다.
                home_check=false;
                ft.replace(R.id.Main_Frame,new mondo.earphone.tmap.TmapActivity());
                ft.addToBackStack(null);
                ft.commit();
                break;

        }
    }


    // 권한 허용 묻기
    public void checkSelfPermission() {
        String temp = "";
        //파일 읽기 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.READ_EXTERNAL_STORAGE + " "; }

        //파일 쓰기 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.WRITE_EXTERNAL_STORAGE + " "; }

        // 오디오 녹음 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.RECORD_AUDIO + " "; }

        // 위치 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.ACCESS_FINE_LOCATION + " "; }

        // 위치 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.ACCESS_COARSE_LOCATION + " "; }

        // 디바이스 정보 권한 (녹음 장치 저장할때 기본키로 사용하기 위하여여)
       if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.ACCESS_COARSE_LOCATION + " "; }

        if (TextUtils.isEmpty(temp) == false) {
            // 권한 요청
            ActivityCompat.requestPermissions(this, temp.trim().split(" "),1);
        }
        else { // 모두 허용 상태

        }
    }





    // 뒤로가기 2번 누를시 앱 종료
    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
            ActivityCompat.finishAffinity(this);
        } else {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(), "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }

}