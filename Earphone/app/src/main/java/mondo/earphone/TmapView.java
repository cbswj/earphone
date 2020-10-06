package mondo.earphone;

import android.Manifest;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapTapi;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;

public class TmapView extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    TMapTapi tmaptapi; // T맵 API
    TMapView tMapView;// 티맵
    TMapData tMapData;// 티맵 데이터
    TMapGpsManager tmapgps =null;

    ProgressDialog dialog; // 로딩

    LinearLayout code_red; // 빨간색 경고
    ImageView cycleImg; // 오토바이 사진
    ImageView bikeImg; // 자전거 사진
    ImageView carImg; // 자동차 사진
    Animation carAni; // 오토바이 애니메이션
    Animation cycleAni; // 오토바이 애니메이션
    Animation bikeAni; // 자전거 애니메이션
    AlphaAnimation codeRed_anim; //화면 깜박임 애니메이션

    LinearLayout popUp_li; //팝업창 레이아웃
    ImageView popupImage; //팝업창 대표 이미지
    TextView popupTitle; //팝업창 제목
    TextView popupContent; //팝업창 내용

    int ani_condition=0; //애니메이션 조건



    //지도 위치가 바뀌었을때 호출되는 함수
    @Override
    public void onLocationChange(Location location) {
        tMapView.setLocationPoint(location.getLongitude(),location.getLatitude());
       // dialog.cancel();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tmap_view);


        // 위치권한 허용을 물어야 앱이 실행 가능
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1); //위치권한 탐색 허용 관련 내용
        }

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


        /*
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("데이터 확인중");
        dialog.show();// 프로그레스바 시작

         */

        tmaptapi = new TMapTapi(getApplicationContext());
        tmaptapi.setSKTMapAuthentication ("l7xxf9b9c4626ff64a598617074e96cfceba"); // 티맵 API 가져오기

        LinearLayout tmap_Layout = (LinearLayout) findViewById(R.id.tmap);

        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("l7xxf9b9c4626ff64a598617074e96cfceba"); // 티맵 지도 가져오기
        tmap_Layout.addView(tMapView); //레이아웃에 티맵 표시하기


        tMapData = new TMapData();
        tmapgps = new TMapGpsManager(TmapView.this);
        tmapgps.setMinTime(1000); //현재 위치를 찾을 최소 시간 (밀리초)
        tmapgps.setMinDistance(5); //현재 위치를 갱신할 최소 거리

        //tMapView.setCompassMode(true);  // 단말의 방향에 따라 지도를 회전시켜주는 기능.
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mypoint); // 핀포인트 사진
        tMapView.setIcon(bitmap); //핀포인트 설정
        tMapView.setIconVisibility(true); // 단말 점 표시
        tMapView.setZoomLevel(18);  // 지도에서 보이는 줌 레벨
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD); // 맵 표준 설정
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN); // 한국어 설정
        tMapView.setTrackingMode(true); // 지도의 위치를 현재 단말의 위치로 이동시켜 줌
        tMapView.setSightVisible(true); // 단말이 가리키는 방향이 지도에서 어디를 보여주고 있는지 나타내 줌




        tmapgps.setProvider(tmapgps.NETWORK_PROVIDER); //연결된 인터넷으로 현재위치를 찾음
                                                        //실내에서 유용


        tmapgps.OpenGps();;//네트워크 위치 탐색 허용

        popUp_li = (LinearLayout) findViewById(R.id.pop_up); // 팝업창 레이아웃
        code_red = (LinearLayout) findViewById(R.id.code_red); // 빨간색 경고
        cycleImg = (ImageView) findViewById(R.id.cycle);  // 오토바이 사진
        bikeImg = (ImageView) findViewById(R.id.bike); //자전거 사진
        carImg = (ImageView) findViewById(R.id.car);
        popupImage = (ImageView) findViewById(R.id.pop_up_img); //팝업창 대표 이미지

        carImg.setVisibility(View.INVISIBLE); // 자동차 숨기기
        cycleImg.setVisibility(View.INVISIBLE); // 오토바이 숨기기
        code_red.setVisibility(View.INVISIBLE); // 빨간 경고창 숨기기
        popUp_li.setVisibility(View.INVISIBLE); // 팝업창 숨기기
        bikeImg.setVisibility(View.INVISIBLE); // 자전거 숨기기
        carAni = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.car_ani); // 정의해놓은 자동차 애니메이션 가져오기
        bikeAni = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bike_ani); // 정의해놓은 자전거 애니메이션 가져오기
        cycleAni = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.cycle_ani); // 정의해놓은 오토바이 애니메이션 가져오기
        popupTitle = (TextView) findViewById(R.id.pop_up_title); // 팝업 제목
        popupContent = (TextView) findViewById(R.id.pop_up_content); // 팝업 내용

        //화면 깜박임 애니메이션 선언
        codeRed_anim = new AlphaAnimation(0.0f,1.0f); //투명도 설정
        codeRed_anim.setDuration(100); //지속시간
        codeRed_anim.setRepeatMode(Animation.REVERSE); //반복 유무 설정
        codeRed_anim.setRepeatCount(8);  // 반복 횟수



        //티맵 자체 터치를 막았음(막아야 될꺼 같음)
        //터치시 애니메이션 작동
        tMapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(ani_condition==0) {
                    new Handler().post(new Runnable() { // new Handler and Runnable
                        @Override
                        public void run() {
                            //화면에 나타내기
                            cycleImg.setVisibility(View.VISIBLE);
                            code_red.setVisibility(View.VISIBLE);
                            popUp_li.setVisibility(View.VISIBLE);
                            popUp_li.bringToFront(); // 제일 앞으로 가져오기


                            //팝업창 설정
                            popupImage.setImageResource(R.drawable.cycle);
                            popupTitle.setText("오토바이");
                            popupContent.setText("접근 중입니다!");


                            code_red.startAnimation(codeRed_anim); // 화면 깜박임 애니메이션 시작
                            cycleImg.startAnimation(cycleAni); //오토바이 애니메이션 시작


                        }
                    });
                }else if(ani_condition==1){
                    new Handler().post(new Runnable() { // new Handler and Runnable
                        @Override
                        public void run() {
                            //화면에 나타내기
                            bikeImg.setVisibility(View.VISIBLE);
                            code_red.setVisibility(View.VISIBLE);
                            popUp_li.setVisibility(View.VISIBLE);
                            popUp_li.bringToFront(); // 제일 앞으로 가져오기


                            //팝업창 설정
                            popupImage.setImageResource(R.drawable.bike);
                            popupTitle.setText("자전거");
                            popupContent.setText("접근 중입니다!");

                            code_red.startAnimation(codeRed_anim); // 화면 깜박임 애니메이션 시작
                            bikeImg.startAnimation(bikeAni); // 자전거 애니메이션 시작
                        }
                    });
                }else if(ani_condition==2){
                    new Handler().post(new Runnable() { // new Handler and Runnable
                        @Override
                        public void run() {
                            //화면에 나타내기
                            carImg.setVisibility(View.VISIBLE);
                            code_red.setVisibility(View.VISIBLE);
                            popUp_li.setVisibility(View.VISIBLE);
                            popUp_li.bringToFront(); // 제일 앞으로 가져오기


                            //팝업창 설정
                            popupImage.setImageResource(R.drawable.car);
                            popupTitle.setText("자동차");
                            popupContent.setText("접근 중입니다!");

                            code_red.startAnimation(codeRed_anim); // 화면 깜박임 애니메이션 시작
                            carImg.startAnimation(carAni); // 자동차 애니메이션 시작
                        }
                    });
                }


                return true;
            }
        });

        //오토바이 애니메이션 리스너
        cycleAni.setAnimationListener(new Animation.AnimationListener() {

            //애니메이션 시작
            @Override
            public void onAnimationStart(Animation animation) {

            }

            //애니메이션 끝났을경우
            @Override
            public void onAnimationEnd(Animation animation) {
                //끝날경우 다 없어져라
                popUp_li.setVisibility(View.INVISIBLE);
                code_red.setVisibility(View.INVISIBLE);
                cycleImg.setVisibility(View.INVISIBLE);
                ani_condition++;
            }

            //애니메이션 반복
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        //자전거 애니메이션 리스너
        bikeAni.setAnimationListener(new Animation.AnimationListener() {

            //애니메이션 시작
            @Override
            public void onAnimationStart(Animation animation) {

            }

            //애니메이션 끝났을경우
            @Override
            public void onAnimationEnd(Animation animation) {
                //끝날경우 다 없어져라
                popUp_li.setVisibility(View.INVISIBLE);
                code_red.setVisibility(View.INVISIBLE);
                bikeImg.setVisibility(View.INVISIBLE);
                ani_condition++;
            }

            //애니메이션 반복
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        //자동차 애니메이션 리스너
        carAni.setAnimationListener(new Animation.AnimationListener() {

            //애니메이션 시작
            @Override
            public void onAnimationStart(Animation animation) {

            }

            //애니메이션 끝났을경우
            @Override
            public void onAnimationEnd(Animation animation) {
                //끝날경우 다 없어져라
                popUp_li.setVisibility(View.INVISIBLE);
                code_red.setVisibility(View.INVISIBLE);
                carImg.setVisibility(View.INVISIBLE);
                ani_condition=0;
            }

            //애니메이션 반복
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


}