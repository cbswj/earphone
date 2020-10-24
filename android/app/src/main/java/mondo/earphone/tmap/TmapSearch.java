package mondo.earphone.tmap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;

import java.util.ArrayList;

import mondo.earphone.R;



public class TmapSearch extends AppCompatActivity {

    // 리싸이 클러뷰
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager; // 레이아웃 매니저 변수 // 리사이클러뷰는 레이아웃 종류가 많아서 변수로 원하는걸 받아서 정함
    ArrayList<TmapSearch_MyItem> items=null;  // 넘겨줄 데이터 모음
    TmapSearch_MyAdapter adapter;         // 리사이클러뷰 MyAdapter

    // 현재 위치
    double current_Latitude; // 위도
    double current_longitude; // 경도
    TMapPoint tmapint;

    // 검색
    ImageView search_button;
    EditText search_edit;

    // 핸들러를 위한 이벤트
    Message message = null; // 데이터 로딩 후 메인 UI 업데이트 메시지
    private final int FINISH = 999; // 핸들러 메시지 구분 ID
    private final int SEARCH_NULL = 888; // 핸들러 메시지 구분 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tmap_search);

        // 배터리 색깔
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

        //

        // 검색버튼
        search_button = (ImageView) findViewById(R.id.search_button);
        // 검색내용
        search_edit = (EditText) findViewById(R.id.search_edit);

        // 현재 위치 위도 경도 intent 받는 구문
        try {
            Intent intent = getIntent();
            current_Latitude = intent.getDoubleExtra("current_Latitude",0);
            current_longitude = intent.getDoubleExtra("current_longitude",0);
        }catch (NullPointerException e) {
            Log.d("intent","intent가 null 값을 전달합니다.");
        }


        // 내 현재 위치를 담기 (목적지까지의 거리를 알기 위해)
        tmapint = new TMapPoint(current_Latitude, current_longitude);

        // 리사이클러뷰
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        layoutManager = new LinearLayoutManager(this); // 리스트형으로 설정
        recyclerView.setLayoutManager(layoutManager); //리사이클러뷰와 레이아웃 매니저를 연결


        // 검색 돋보기 버튼 클릭 시
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 검색하기 기능
                // POI검색, 경로검색 등의 지도데이터를 관리하는 클래스

                // 널값인 경우 밑에 구문들이 실행 안되게 예외처리
                if(TextUtils.isEmpty(search_edit.getText().toString())){
                    Toast.makeText(getApplicationContext(), "검색 내용을 입력해 주세요.",Toast.LENGTH_SHORT).show();
                    return;
                }

                TMapData tMapData = new TMapData();


                tMapData.findTitlePOI(search_edit.getText().toString(), 30, new TMapData.FindTitlePOIListenerCallback() {
                    @Override
                    public void onFindTitlePOI(ArrayList poiItem) {
                        items = new ArrayList<TmapSearch_MyItem>();  // 검색 초기화

                        // 검색 내용이 없을경우
                        if(poiItem==null) {
                            message = mHandler.obtainMessage(); // 핸들러의 메시지 객체 획득
                            message.what = SEARCH_NULL;
                            mHandler.sendMessage(message);
                            return;
                        }

                        for (int i = 0; i < poiItem.size(); i++) {

                            TMapPOIItem item = (TMapPOIItem) poiItem.get(i);

                            // 확인 로그
                            Log.d("content","POI Name: " + item.getPOIName().toString() + ", " + // 장소 이름
                                    "Address: " + item.getPOIAddress().replace("null", "") + ", " + // 주소
                                    "Point: " + item.getPOIPoint().toString()+", "+ // 위도 경도
                                    "distance: "+item.getDistance(tmapint)); // 거리

                            // M 단위 까지만 소수점으로 남게하고 나머지 소수점은 버리는 로직
                            float distance = ((int)((((int)item.getDistance(tmapint))*10)/1000f))/10f;

                            // 목적지 위도 경도
                            String gps[] = item.getPOIPoint().toString().split(" ");

                            TmapSearch_MyItem model = new TmapSearch_MyItem(item.getPOIName(), item.getPOIAddress().replace("null", ""), distance+"km",  Double.valueOf(gps[1]), Double.valueOf(gps[3]));
                            items.add(model);
                        }

                        adapter = new TmapSearch_MyAdapter(items);

                        // 리싸이클러뷰 클릭 이벤트
                        adapter.setOnItemClickListener(new TmapSearch_MyAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(View v, int pos) {
                                TmapSearch_MyItem model = items.get(pos);

                                Intent intent = new Intent();

                                intent.putExtra("current_Latitude", current_Latitude); // 현재 위도
                                intent.putExtra("current_longitude", current_longitude); // 현재 경도

                                intent.putExtra("destination_Latitude", model.getLatitude()); // 도착지 위도
                                intent.putExtra("destination_longitude", model.getLongitude()); // 도착지 경도
                                setResult(RESULT_OK,intent);
                                finish();
                            }
                        });




                        message = mHandler.obtainMessage(); // 핸들러의 메시지 객체 획득
                        message.what = FINISH;

                        // tMapData가 background에서 서브 스레드로 돌기 때문에 여기서 UI 작업을 하는
                        // 리싸이클러뷰를 사용하면 팅기게 된다.
                        // 메인 쓰레드가 UI작업을 하게 핸들러를 사용해야 한다.
                        mHandler.sendMessage(message);


                    }
                });
            }
        });

    }

    // UI 변경을 위한 핸들러
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // Tmap 검색 내용이 있을 경우
                case FINISH :
                    recyclerView.setAdapter(adapter);
                    break;
                    // Tmap 검색 내용이 없을경우
                case SEARCH_NULL:
                    Toast.makeText(getApplicationContext(), "해당 장소를 찾을 수 없습니다.",Toast.LENGTH_SHORT).show();
                    break;
                // TODO : add case.
            }
        }
    } ;
}