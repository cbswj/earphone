package mondo.earphone.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.util.ArrayList;

import mondo.earphone.ForegroundService;
import mondo.earphone.R;
import mondo.earphone.appInfo;
import mondo.earphone.tmap.TmapSearch_MyAdapter;
import mondo.earphone.tmap.TmapSearch_MyItem;

public class HomeActivity extends Fragment {




    ImageView off_Button; // 24시간 감지 끄기
    ImageView on_Button; // 24시간 감지 켜기

    public static boolean off_condition=true; // 24시간 감지 끄기
    public static boolean on_condition=false; // 24시간 감지 켜기

    SharedPreferences detectMode; // 버튼 상태 내부저장소에 저장하기 위함
    int log_count=0; // 로그 기록


    // 리싸이 클러뷰
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager; // 레이아웃 매니저 변수 // 리사이클러뷰는 레이아웃 종류가 많아서 변수로 원하는걸 받아서 정함
    ArrayList<Home_MyItem> items=null;  // 넘겨줄 데이터 모음
    Home_MyAdapter adapter;         // 리사이클러뷰 MyAdapter


    TextView no_sound;

    DatabaseReference reference;




    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_activity,container,false);



        // 배터리부분 상태바 불투명하게 하기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
        }




        no_sound = (TextView) view.findViewById(R.id.null_sound); // 감지된 로그내역이 없을경우
        off_Button = (ImageView) view.findViewById(R.id.offButton); // 24시간 감지 끄기
        on_Button = (ImageView) view.findViewById(R.id.onButton); // 24시간 가지 켜기

        Glide.with(getContext()).load(R.drawable.stopbutton).into(off_Button);
        Glide.with(getContext()).load(R.raw.startbutton).into(on_Button);


        // 광클로 인한 버그를 막기 위하여 foreground 서비스 안에 버튼 클릭시 다른 버튼은 2초 지나야 눌리게 logic이 구현되어 있음
        // 꺼져있는 버튼
        off_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 버튼을 켜는 기능
                if (off_condition) {
                    off_Button.setVisibility(View.GONE);
                    on_Button.setVisibility(View.VISIBLE);
                    startService();

                    SharedPreferences.Editor editor = detectMode.edit();
                    editor.putInt("detectMode", 1); // 버튼 켜져있는 상태로 저장
                    editor.apply();

                }else {
                    Toast.makeText(getContext(), "잠시 후 다시 클릭해주세요.", Toast.LENGTH_SHORT).show();
                }


            }
        });

        // 켜져있는 버튼
        on_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 버튼을 끄는 기능
                if(on_condition) {
                    off_Button.setVisibility(View.VISIBLE);
                    on_Button.setVisibility(View.GONE);
                    stopService();

                    SharedPreferences.Editor editor = detectMode.edit();
                    editor.putInt("detectMode", 0); // 버튼 꺼져있는 상태로 저장
                    editor.apply();




                }else {
                    Toast.makeText(getContext(), "잠시 후 다시 클릭해주세요.", Toast.LENGTH_SHORT).show();
                }


            }
        });




        // 내부 저장소 사용
        detectMode = getContext().getSharedPreferences("detectMode", Activity.MODE_PRIVATE);
        int OnOff = detectMode.getInt("detectMode", -1); // 가져왔는데 없으면 기본값 -1

        // 앱을 처음 만들때는 내부 저장소가 없으므로 생성해준다.
        if(OnOff == -1) {
            // 로그기록 마지막 index 0으로 넣어줌
            SharedPreferences.Editor editor = detectMode.edit();
            OnOff=0; // 0부터 시작하게 초기화
            editor.putInt("detectMode",OnOff); // 0이 꺼짐 1이 켜짐
            editor.apply();
            // 앱 처음킬때는 당연이 꺼져있으므로 GONE 처리
            on_Button.setVisibility(View.GONE);
        }else if(OnOff == 0){
            // 버튼이 꺼져있는 상태면 킨 버튼은 안보이게
            on_Button.setVisibility(View.GONE);
        }else if(OnOff == 1){
            // 버튼이 켜져있는 상태면 꺼져있는 버튼은 안보이게
            // glide 깜박임 문제 때문에 이미지뷰 버튼을 2개 사용하였음
            off_Button.setVisibility(View.GONE);
        }

        // 리사이클러뷰
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        layoutManager = new LinearLayoutManager(getContext()); // 리스트형으로 설정
        recyclerView.setLayoutManager(layoutManager); //리사이클러뷰와 레이아웃 매니저를 연결
        items = new ArrayList<Home_MyItem>();  // 검색 초기화

        SharedPreferences detectLog = getContext().getSharedPreferences("detectLog", Activity.MODE_PRIVATE);
        int log_count = detectLog.getInt("detectLog_length", -1); // 가져왔는데 없으면 기본값 -1

        Log.d("log_count", "크기 : "+log_count);

        // 앱을 처음 만들때는 내부 저장소가 없으므로 생성해준다.
        // 로그 기록
        if(log_count == -1) {
            // 로그내역 없을경우
            no_sound.setVisibility(View.VISIBLE);
        }else{
            // 로그 내용이 있는 경우 리사이클러뷰에 추가하기
            for(int i=log_count-1; i>=0; i--){
                String time = formatTimeString(detectLog.getLong("detect_time"+i,1));
                String content = detectLog.getString("detect_content"+i, "1");
                Log.d("내용","time: "+time +" content: "+content);
                items.add(new Home_MyItem(time, content));
            }
            // 로그내역 있을경우
            no_sound.setVisibility(View.GONE);
        }

        // 리스트뷰에 itmes 추가하기
        adapter = new Home_MyAdapter(items);
        recyclerView.setAdapter(adapter);

        return view;
    }

    // foregroundSercie 시작 함수
    public void startService() {
        Intent serviceIntent = new Intent(getContext(), ForegroundService.class);
        ContextCompat.startForegroundService(getContext(), serviceIntent);
    }

    // foregroundSercie 끝내기 함수
    public void stopService() {
        Intent serviceIntent = new Intent(getContext(), ForegroundService.class);
        getContext().stopService(serviceIntent);
    }



// 몇분전, 방금전 로직 시간 함수
    private String formatTimeString(long regTime) {
        int SEC = 60;
        int MIN = 60;
        int HOUR = 24;
        int DAY = 30;
        int MONTH = 12;

        long curTime = System.currentTimeMillis();
        long diffTime = (curTime - regTime) / 1000;
        String msg = null;
        if (diffTime < SEC) {
            msg = "방금 전";
        } else if ((diffTime /= SEC) < MIN) {
            msg = diffTime + "분 전";
        } else if ((diffTime /= MIN) < HOUR) {
            msg = (diffTime) + "시간 전";
        } else if ((diffTime /= HOUR) < DAY) {
            msg = (diffTime) + "일 전";
        } else if ((diffTime /= DAY) < MONTH) {
            msg = (diffTime) + "달 전";
        } else {
            msg = (diffTime) + "년 전";
        }
        return msg;
    }




}
