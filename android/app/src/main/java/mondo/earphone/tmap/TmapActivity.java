package mondo.earphone.tmap;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapTapi;
import com.skt.Tmap.TMapView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.LogManager;

import mondo.earphone.ForegroundService;
import mondo.earphone.R;
import mondo.earphone.WavRecorder;
import mondo.earphone.appInfo;
import mondo.earphone.home.HomeActivity;

import static android.app.Activity.RESULT_OK;

public class TmapActivity extends Fragment {

    TMapTapi tmaptapi; // T맵 API
    TMapView tMapView;// 티맵


    LinearLayout search_li; // 검색창 틀
    TextView search_edit; // 검색어

    TMapData tMapData;// 티맵 데이터

    ProgressDialog dialog; // 로딩

    boolean loading = false;

    // 현재 위치
    double current_Latitude; // 위도
    double current_longitude; // 경도

    private final int FINISH = 999; // 핸들러 메시지 구분 ID


    // 데시벨 감지
    public static MediaRecorder mRecorder=null;
    private double volume = 10000;
    private double db=0;
    public static Thread readThread; // 데시벨 읽는 쓰레드
    private readRunnable readrun;
    private Thread saveThread; // 데시벨 읽는 쓰레드
    private saveRunnable saverun;
    private WavRecorder wavRecorder;
    int count=0;

    // 팝업창
    LinearLayout code_red; // 빨간색 경고
    LinearLayout popUp_li; //팝업창 레이아웃
    ImageView popupImage; //팝업창 대표 이미지
    TextView popupTitle; //팝업창 제목
    TextView popupContent; //팝업창 내용

    AlphaAnimation codeRed_anim; //화면 깜박임 애니메이션
    Message message = null; // 데이터 로딩 후 메인 UI 업데이트 메시지

    private  DatabaseReference   reference; // 데이터베이스

    // s3
    private final String BUCKET_NAME = "wav-earphone"; // S3 버킷 이름 (저장소 이름)

    private TransferObserver uploadObserver; // 파일 업로드 시 모니터링을 하기 위한 객체


    boolean isGPSEnabled; // GPS 위치 찾기 가능 유무 확인
    boolean isNetworkEnabled; // 인터넷 위치 찾기 가능 유무 확인

    int route_guide; // Tmap 목적지 탐색을 또한 경우


    int OnOff; //24 알람 on/off 확인용



    private Date date; // 푸쉬알림 보낼시의 시간
    long now; // 데시벨 발생한 시간
    boolean deep_wait=false; // 딥러닝 기다리기 위한


    public static int tmapId_count=0;  // 파베 UID 변경 (오류 제어를 위해)

    SharedPreferences detectLog; // 내부저장소


    @Override
    public void onPause() {
        super.onPause();

        reference = FirebaseDatabase.getInstance().getReference();
        reference.child("UID").removeValue();
        reference.child("Result").removeValue();

    }

    @Override
    public void onDestroy() {
        Log.d("안녕","과연 Destory");
        // 파이어베이스 삭제
        reference = FirebaseDatabase.getInstance().getReference();
        reference.child("UID").removeValue();
        reference.child("Result").removeValue();


        detectLog = getContext().getSharedPreferences("detectMode", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = detectLog.edit();
        editor.putInt("tmapId_count",++tmapId_count);
        editor.apply();

        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tmap_activity, container, false);


        detectLog = getContext().getSharedPreferences("detectMode", Activity.MODE_PRIVATE);
        tmapId_count = detectLog.getInt("tmapId_count", 0); // 가져왔는데 없으면 기본값 0

        // 내부저장소에서 현재 on/off 상태 가져옴
        OnOff = detectLog.getInt("detectMode", 0); // 가져왔는데 없으면 기본값 -1


        // 배터리부분 상태바 불투명하게 하기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
        }


        popUp_li = (LinearLayout) view.findViewById(R.id.pop_up); // 팝업창 레이아웃
        code_red = (LinearLayout) view.findViewById(R.id.code_red); // 빨간색 경고
        popupImage = (ImageView) view.findViewById(R.id.pop_up_img); //팝업창 대표 이미지
        popupTitle = (TextView) view.findViewById(R.id.pop_up_title); // 팝업 제목
        popupContent = (TextView) view.findViewById(R.id.pop_up_content); // 팝업 내용

        //화면 깜박임 애니메이션 선언
        codeRed_anim = new AlphaAnimation(0.0f,1.0f); //투명도 설정
        codeRed_anim.setDuration(100); //지속시간
        codeRed_anim.setRepeatMode(Animation.REVERSE); //반복 유무 설정
        codeRed_anim.setRepeatCount(8);  // 반복 횟수


        // 처음 들어왔을때 로딩 띄우기
        dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("데이터 확인중");
        dialog.show();// 프로그레스바 시작

        search_li = (LinearLayout) view.findViewById(R.id.search_li); // 검색창틀
        search_edit = (TextView) view.findViewById(R.id.search_edit); // 검색어 입력기



        // 검색창틀 터치 시 주소 검색 class로 넘기는 이벤트트
       search_li.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Intent intent = new Intent(getContext(), TmapSearch.class);
                intent.putExtra("current_Latitude", current_Latitude);
                intent.putExtra("current_longitude", current_longitude);
                startActivityForResult(intent, 100);
            }
        });

       // 텍스트창 터치 시에도 주소 검색 class로 넘어가게 부모 뷰 레이아웃 강제 클릭 이벤트 발생 시킴
        search_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search_li.performClick();
            }
        });



        Context mFcontext = getActivity();

        tmaptapi = new TMapTapi(mFcontext);
        tmaptapi.setSKTMapAuthentication("l7xxf9b9c4626ff64a598617074e96cfceba"); // 티맵 API 가져오기

        ConstraintLayout tmap_Layout = (ConstraintLayout) view.findViewById(R.id.tmap);

        tMapView = new TMapView(mFcontext);
        tMapView.setSKTMapApiKey("l7xxf9b9c4626ff64a598617074e96cfceba"); // 티맵 지도 가져오기

        // 주소 검색을 위한 객체 생성
        tMapData = new TMapData();


        //tMapView.setCompassMode(true);  // 단말의 방향에 따라 지도를 회전시켜주는 기능.
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mypoint); // 핀포인트 사진
        tMapView.setIcon(bitmap); //핀포인트 설정
        tMapView.setIconVisibility(true); // 단말 점 표시
        tMapView.setZoomLevel(18);  // 지도에서 보이는 줌 레벨
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD); // 맵 표준 설정
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN); // 한국어 설정
        tMapView.setTrackingMode(true); // 지도의 위치를 현재 단말의 위치로 이동시켜 줌
        tMapView.setSightVisible(true); // 단말이 가리키는 방향이 지도에서 어디를 보여주고 있는지 나타내 줌

        tmap_Layout.addView(tMapView); //레이아웃에 티맵 표시하기

        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        // GPS 프로바이더 사용가능여부
        isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // 네트워크 프로바이더 사용가능여부
        isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        // 현재 위치를 찾아서 표시
        setGps();


        // 티맵보다 상단에 보이기 위하여 제일 앞으로 가져옴
        search_li.bringToFront();


        //자동차 애니메이션 리스너
        codeRed_anim.setAnimationListener(new Animation.AnimationListener() {

            //애니메이션 시작
            @Override
            public void onAnimationStart(Animation animation) {

            }

            //애니메이션 끝났을경우
            @Override
            public void onAnimationEnd(Animation animation) {
                //끝날경우 다 없어져라
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        popUp_li.setVisibility(View.INVISIBLE);
                        code_red.setVisibility(View.INVISIBLE);
                    }
                }, 3000);  // 1 초 후에 실행
            }

            //애니메이션 반복
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        code_red.setVisibility(View.INVISIBLE);
        popUp_li.setVisibility(View.INVISIBLE);
        code_red.bringToFront(); // 제일 앞으로
        popUp_li.bringToFront(); // 제일 앞으로 가져오기






        // 쓰레드 생성
        readrun = new readRunnable();
        readThread = new Thread(readrun);

        // 버그 방지를 위한 딜레이
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 데시벨 녹음 시작
                startRecorder();
                saverun = new saveRunnable();
                // 데시벨 읽는 쓰레드
                readThread.start();
            }
        }, 2000);




        reference = FirebaseDatabase.getInstance().getReference();



        reference.child("Result").child(appInfo.android_id+"-tmap"+tmapId_count).addChildEventListener(new ChildEventListener() {  //데이터 변화 감지중 (러닝결과 언제오나)
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


        return view;
    }



    // intent 보낸 것 반환 받는 함수
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("onActivityResult","순서");

        // 검색하러간 intent 돌아왔을경우 이벤트
        if(requestCode == 100){



            // 주소 선택 후 돌아 왔을 경우
            if (resultCode == RESULT_OK){

                // 목적지 탐색중 표시
                route_guide=1;

                current_Latitude = data.getDoubleExtra("current_Latitude", 1);
                current_longitude = data.getDoubleExtra("current_longitude", 1);

                Log.d("도착지 위도", " : "+data.getDoubleExtra("destination_Latitude",1));
                Log.d("도착지 경도", " : "+data.getDoubleExtra("destination_longitude",1));

                double destination_Latitude = data.getDoubleExtra("destination_Latitude",1);
                double destination_longitude = data.getDoubleExtra("destination_longitude",1);

                TMapPoint tMapPointStart = new TMapPoint(current_Latitude, current_longitude);
                TMapPoint tMapPointEnd = new TMapPoint(destination_Latitude, destination_longitude);

                tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, tMapPointStart, tMapPointEnd, new TMapData.FindPathDataListenerCallback()  {
                    @Override
                    public void onFindPathData(TMapPolyLine polyLine) {
                        tMapView.addTMapPath(polyLine);
                    }
                });


            }


        }
    }




//    TMapGpsManager 의 생성자로 넘긴 파라미터인 getActivity() 는 Fragment 의 부모 Activity 를 가르킵니다.
//    onLocationChange 는 Fragment 가 아닌 부모 Activity 의 onLocationChange 가 호출되기 때문에
//    따라서 Fragment 의 부모 Activity 에서 구현한 onLocationChange 함수에서 위치값을 받아서 Fragment 로 전달해야 함
//    그것보다는 간단하게 안드로이드 SDK 를 이용해서 위치정보를 가져오는 코드를 구현하시는 것이 낫다
//    아래는 안드로이드 SDK 를 이용한 위치정보 가져오는 코드


    // 인터넷을 통한 얻어낸 위치를 반영하여 Tmap을 변경하는 코드 (리스너)
    private final LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            if(location != null){
                current_Latitude = location.getLatitude();
                current_longitude = location.getLongitude();
                tMapView.setLocationPoint(current_longitude, current_Latitude);
                tMapView.setCenterPoint(current_longitude, current_Latitude);

                // 처음 화면을 보였을 경우만 로딩 끄기 (위치가 바뀔때마다 계속 실행시키지 않기 위하여)
                if(loading == false){
                    dialog.cancel();

                    LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }

                    // GPS로 찾으면 위치를 찾는데 매우 느리기 때문에 (건물안에 있다는 전제하에)
                    // 네트워크 인터넷으로 위치를 찾아야 매우 빨리 찾을수 있다.
                    if (isGPSEnabled) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, // 등록할 위치제공자
                                1000, // 통지사이의 최소 시간간격 (miliSecond)
                                1, // 통지사이의 최소 변경거리 (m)
                                mLocationListener);
                    }
                    else {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자
                                1000, // 통지사이의 최소 시간간격 (miliSecond)
                                1, // 통지사이의 최소 변경거리 (m)
                                mLocationListener);
                    }
                    loading = true;
                }else {

                }
            }

        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    // 위치 권한 여부를 확인하고
    // 위치를 업데이트 해달라고 요청함
    public void setGps() {

        final LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }



        // GPS로 찾으면 위치를 찾는데 매우 느리기 때문에 (건물안에 있다는 전제하에)
        // 네트워크 인터넷으로 위치를 찾아야 매우 빨리 찾을수 있다.
        if (isNetworkEnabled) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자
                    1000, // 통지사이의 최소 시간간격 (miliSecond)
                    1, // 통지사이의 최소 변경거리 (m)
                    mLocationListener);
        }
        else {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, // 등록할 위치제공자
                    1000, // 통지사이의 최소 시간간격 (miliSecond)
                    1, // 통지사이의 최소 변경거리 (m)
                    mLocationListener);
        }

    }

    class readRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    getDb();
                    Log.i("삽입"," "+db);
                    if(db>=80){
                        // 딥러닝 진행중이니 데시벨 측정 막기
                        deep_wait=true;
                        stopRecorder();
                        Log.i("성공"," "+db);

                        now = System.currentTimeMillis();
                        // 현재시간의 날짜 생성하기
                        date = new Date(now);




                        wavRecorder = new WavRecorder(appInfo.android_id+".wav");

                        // wav 파일 저장
                        saveThread = new Thread(saverun);
                        saveThread.start();
                        saveThread.join();
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
            Log.d("저장?","저장 시작 시간");

            wavRecorder.startRecording();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            File file = wavRecorder.stopRecording();

            awsS3Upload(file,appInfo.android_id+".wav");






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


        // 딥러닝 아직 진행중일 경우 (현재 소리 감지 스레드가 꺼져있으므로 기다려주기 위하여 사용)
        if(deep_wait){
            db=0;
            return 0;
        }

        volume = mRecorder.getMaxAmplitude();
        if (volume > 0 && volume < 1000000) {
            db = 20 * (float)(Math.log10(volume));  //파형을 데시벨로 변환
        }
        return db;
    }


    // 데이터 전송 롼료시 UI 변경을 위한 핸들러
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FINISH :
                    String img= msg.obj.toString();

                    if(img.equals("자동차")){
                        popupImage.setImageResource(R.drawable.car);
                    }else if (img.equals("경보기")){
                        popupImage.setImageResource(R.drawable.danger);
                    }else if (img.equals("오토바이")){
                        popupImage.setImageResource(R.drawable.cycle);
                    }


                    code_red.startAnimation(codeRed_anim); // 화면 깜박임 애니메이션 시작



                    // 소리 종류 텍스트
                    popupTitle.setText(img);

                    popupContent.setText("소리가 감지되었습니다!");
                    code_red.setVisibility(View.VISIBLE);
                    popUp_li.setVisibility(View.VISIBLE);

                    // 2초 동안 진동
                    Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(2000);



                    break ;
                // TODO : add case.
            }
        }
    } ;


    //  S3 사진 업로드 함수
    private void awsS3Upload(File wavFile, String fileName){

        reference = FirebaseDatabase.getInstance().getReference().child("UID").child(appInfo.android_id+"-tmap"+tmapId_count);   // 123은 나중에 실 휴대폰 고유 아이디 쓸예정   서버 반응하기 위해 데베 생성

        // AWS s3를 이용하기 위해 생성된 인증 자격을 통하여 객체를 생성하고
        // 데이터를 송수신 하기 위해 TransferUtility 객체를 생성한다.
        AmazonS3 s3 = new AmazonS3Client(appInfo.credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3, getContext());

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
        message = mHandler.obtainMessage(); // 핸들러의 메시지 객체 획득
        message.what = FINISH;
        boolean check=false;


        // 밑에 로그 주석풀면 String result 문 에러남 i.next 가 두개라 데아터가 하나인데 next 두번해서 에러
        //Log.d(   dataSnapshot.getValue().toString(), ((DataSnapshot) i.next()).getValue().toString());  //((DataSnapshot) i.next()).getValue().toString() 러징 결과 =? 0 1 2  => 추후에 %작업 서버에서 할듯듯
        String result =(String) ((DataSnapshot) i.next()).getValue();

        switch (result){
            case"0":
                result="자동차 소리";
                message.obj = "자동차";
                check=true;
                break;

            case"1":
                result="오토바이 소리";
                message.obj = "오토바이";
                check=true;
                break;
            case"2":
                result="경보기 소리";
                message.obj = "경보기";
                check=true;
                break;
        }

        // 0 = 자동차 경적 , 1= 오토바이  , 2=사이렌
        Log.d("감지 결과", "종류 : "+result);

        // 감지 된게 있을시에만
        if(check) {
            mHandler.sendMessage(message);
        }



        // 남아있으면 나머지가 제대로 안됨
        reference = FirebaseDatabase.getInstance().getReference();
        reference.child("Result").child(appInfo.android_id+"-tmap"+tmapId_count).removeValue();




        // 딥러닝 완료시 데시벨 측정하게 풀어줌
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                volume = 10000;
                db=0;
                startRecorder();
                deep_wait=false;
            }
        }, 2000);



    }

}
