package mondo.earphone;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public class SplashActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //상태바 (배터리,시간,날짜 표시되는 제일 윗부분) 색깔지정
        View view = getWindow().getDecorView();  // 액티비티의 view 뷰 정보 가져오기
        if (Build.VERSION.SDK_INT >= 21) {
            //21 버전보다 낮으면 검은색 바탕
            getWindow().setStatusBarColor(Color.BLACK);

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                // 23 버전 이상일 때 상태바 하얀 색상에 회색 아이콘 색상을 설정
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);  // 밝은 상태바 요청
                getWindow().setStatusBarColor(Color.parseColor("#f2f2f2"));
            }
        }
        startLoading();


    }



    private void startLoading(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        },500);
    }

}
