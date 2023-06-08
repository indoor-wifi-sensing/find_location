package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    int count = 0;
    TextView txt;
    private TextView textViewResponse;

    // Wifi 스캐닝, 권한 획득 관련 변수
    private PermissionSupport permission;
    WifiManager wifiManager;
    BroadcastReceiver wifiScanReceiver;

    //레이아웃 컨트롤 관련 변수
    Button scanBtn,submitBtn;
    ListView wifiList;
    List<ScanResult> wifiResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 사용자에게 필요한 권한 요청
        permissionCheck();

        scanBtn = findViewById(R.id.scanNow);
        submitBtn = findViewById(R.id.submitDB);

        wifiList = findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        txt = findViewById(R.id.distance);
        textViewResponse = findViewById(R.id.textViewResponse);
        txt.setText("20.0m");

        // 시스템에서 각종 변경 정보를 인식했을 때, 그 중에서도 Wifi 스캔 값이 변경되었을 경우 동작
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                    Log.e("wifi","scan Success!!!!!");
                    wifiAnalyzer();
                }
                else {
                    scanFailure();
                    Log.e("wifi","scan Failure.....");
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiScanReceiver, intentFilter);

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[][] wifiData = new String[500][2];
                int i = 0;
                for (ScanResult choseWifi : wifiResult) {
                    String MAC = choseWifi.BSSID;
                    int rss = choseWifi.level;
                    wifiData[i][0] = MAC;
                    wifiData[i][1] = Integer.toString(rss);
                    i++;
                }

                Gson gson = new Gson();
                String jsonWifiData = gson.toJson(wifiData); // converting wifiData to JSON format

                if (isNetworkAvailable()) {
                    new SendDataTask().execute(jsonWifiData); // passing the json string instead of String array
                } else {
                    textViewResponse.setText("Network connection not available");
                }
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                boolean success = wifiManager.startScan();
                if (!success) {
                    scanFailure();
                }
                wifiResult = wifiManager.getScanResults();

                Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult o1, ScanResult o2) {
                        return o2.level - o1.level;
                    }
                };
                Collections.sort(wifiResult, comparator);
            }
        });

    }

    //===========================================
    //========== WiFi 스캐닝 컨트롤 영역 ===========
    //===========================================
    //수집한 Wifi 정보를 배열에 뿌리는 역할
    private void wifiAnalyzer() {
        List<String> list = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        String mac, dbm, freq;
        for (ScanResult choseWifi : wifiResult) {
            mac = choseWifi.BSSID;
            dbm = Integer.toString(choseWifi.level);
            freq = Integer.toString(choseWifi.frequency);

            String completeInfo = mac + " | " + dbm + " | " + freq;
            list.add(completeInfo);
        }
        wifiList.setAdapter(adapter);
    }

    //Wifi 정보 스캔에 성공했을 경우에 행동할 것들
    private void scanSuccess() {
        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        Log.e("wifi", results.toString());

    }

    //Wifi 정보 스캔에 실패했을 경우에 행동할 것들
    private void scanFailure() {
        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        Log.e("wifi", results.toString());
        Toast.makeText(this.getApplicationContext(), "Wifi Scan Failure, Old Information may appear.", Toast.LENGTH_LONG).show();
    }

    private void permissionCheck() {
        permission = new PermissionSupport(this, this);
        if (!permission.checkPermission()) {
            permission.requestPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!permission.permissionResult(requestCode, permissions, grantResults)) {
            permission.requestPermission();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private class SendDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String urlString = "http://172.16.237.123:5000/api";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8"); // UTF-8 인코딩 설정
                conn.setDoOutput(true);

                // 요청 데이터 생성
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("data", params[0]);

                // 요청 데이터 전송
                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                Log.e("server", "성공");
                // 응답 데이터 수신
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")); // UTF-8 인코딩으로 읽기
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Network request failed: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    String message = jsonResponse.getString("message");
                    String receivedData = jsonResponse.getString("received_data");

                    textViewResponse.setText("Message: " + message + "\nReceived Data: " + receivedData);
                    txt.setText(Integer.toString(count));
                    count++;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("text", "result = " + result);
            }
        }
    }

}

