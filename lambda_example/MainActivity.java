package com.smartfarm.www;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {


    final String BUCKET_NAME = "mondo-earphone";

    ImageView img;
    Button button;

    TransferUtility transferUtility; // S3 SDK에 들어있는 클래스
    TransferObserver uploadObserver; // 파일 업로드 시 모니터링을 하기 위한 객체

    File file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 위험 권한 요청 함수수
        checkSelfPermission();

        // S3 SDK에 들어있는 클래스
        transferUtility = AwsConnector.getInitializeAwsS3(this);


        img = (ImageView) findViewById(R.id.img);
        button = (Button) findViewById(R.id.button);


        // 이미지뷰에서 비트맵으로 변환
        BitmapDrawable img_draw  = (BitmapDrawable)img.getDrawable();
        Bitmap img_bit = img_draw.getBitmap();


        //비트맵 파일로 변환
        file = SaveBitmapToFile(img_bit);





        // AWS로 데이터 전송 버튼
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // upload 메서드가 리턴하는 걸 TransferObserver라는 객체에다가 던져준 다음 이걸로
                // TransferListener를 만들면 업로드 현황을 모니터링하면서 몇 퍼센트 업로드가 완료됐는지,
                // 또는 무엇 때문에 업로드에 실패했는지 등을 알 수가 있다.

                uploadObserver = transferUtility.upload(
                        BUCKET_NAME,    // 업로드할 버킷 이름
                        "image/"+"img.png",    // 버킷에 저장할 파일의 이름 확장자명도 붙여줘야댐 png (이름이 key로 쓰임)
                        file       // 버킷에 저장할 파일
                );


                //전송 상태 또는 진행률이 변경 될 때 알림을받는 데 사용되는 리스너
                uploadObserver.setTransferListener(new TransferListener() {

                    // 전송 상태가 변경 될 때 호출됩니다.
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state == TransferState.COMPLETED) {
                            Toast.makeText(getApplicationContext(), "업로드 완료", Toast.LENGTH_SHORT).show();
                            // 캐시로 임시 저장한 파일 삭제
                            file.delete();
                        }
                        Log.d("upload", "onStateChanged: " + id + ", " + state.toString());

                    }


                    // 전송 중일떄 호출
                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                        int percentDone = (int)percentDonef;
                        Log.d("upload", "ID:" + id + " bytesCurrent: " + bytesCurrent + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
                    }

                    // 전송 중 에러시
                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e("upload", ex.getMessage());
                        // 캐시로 임시 저장한 파일 삭제
                        file.delete();
                    }
                });
            }
        });


        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                this,
                "ap-northeast-2:6d58538f-d438-48f3-bd0e-dc7455119ea3", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2  // 물리적인 저장 위치 서울
        );

// Create LambdaInvokerFactory, to be used to instantiate the Lambda proxy.
        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.AP_NORTHEAST_2 , credentialsProvider);

// Create the Lambda proxy object with a default Json data binder.
// You can provide your own data binder by implementing
// LambdaDataBinder.
        final MyInterface myInterface = factory.build(MyInterface.class);

        RequestClass request = new RequestClass("test2.png");
// The Lambda function invocation results in a network call.
// Make sure it is not called from the main thread.
        new AsyncTask<RequestClass, Void, ResponseClass>() {
            @Override
            protected ResponseClass doInBackground(RequestClass... params) {
                // invoke "echo" method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    //Log.d("Tag", "log : "+myInterface.fireDetection(params[0]));
                    //Log.d("Tag", "log : "+myInterface.test(params[1]));
                    return myInterface.fireDetection(params[0]);
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke echo", lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ResponseClass result) {
                if (result == null) {
                    return;
                }

                // Do a toast
                Toast.makeText(MainActivity.this, result.getBody(), Toast.LENGTH_LONG).show();
            }
        }.execute(request);




        // 날씨 정보 가져오기
        //new GetWeatherTask().execute();

    }





    // 현재 권한이 있는지를 체크하고 없으면 권한을 요청하는 event
    public void checkSelfPermission() {
        String temp = "";

        //파일 읽기 권한 확인
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.READ_EXTERNAL_STORAGE + " ";
        }

        //파일 쓰기 권한 확인
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.WRITE_EXTERNAL_STORAGE + " ";
        }


        // 한꺼번에 문자열에 담은 권한들을 요청함함
       if (TextUtils.isEmpty(temp) == false) {
            // 권한 요청
            ActivityCompat.requestPermissions(this, temp.trim().split(" "),1);
        }else {
            // 모두 허용 상태

        }
    }


    // 비트맵 파일로 변환  (변환할 사진, 경로)
    public File SaveBitmapToFile(Bitmap bitmap) {
        // 외부 저장소의 캐시 디렉터리에 temp라는 파일 객체 생성
        File file = new File(this.getExternalCacheDir(),"temp.png");
        OutputStream out = null;
        try {
            // 빈 파일생성
            file.createNewFile();
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return file;
    }


}