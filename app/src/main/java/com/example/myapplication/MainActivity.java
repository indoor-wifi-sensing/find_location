package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView txt;

    // Wifi 스캐닝, 권한 획득 관련 변수
    private PermissionSupport permission;
    WifiManager wifiManager;
    BroadcastReceiver wifiScanReceiver;

    // DB 관리 관련 변수
    SQLiteDatabase db;
    NewSQLiteOpenHelper dbHelper;

    //레이아웃 컨트롤 관련 변수
    Button scanBtn,submitBtn;
    ListView wifiList;
    List<ScanResult> wifiResult;

    public static Toast mtoast;

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

        copyDatabaseFromAssets();
        dbHelper = new NewSQLiteOpenHelper(MainActivity.this, "person.db", null, 1);

        txt = findViewById(R.id.distance);
        txt.setText("20.0m");

        String wifiData[][] = select();

        String[][] wifi = new String[7][2];
        wifi[0][0] = "94:64:24:9f:12:20";
        wifi[0][1] = "-48"; // 0
        wifi[1][0] = "94:64:24:9f:12:21";
        wifi[1][1] = "-48"; // 0
        wifi[2][0] = "94:64:24:9f:12:01";
        wifi[2][1] = "-49"; // 10
        wifi[3][0] = "1e:39:29:50:eb:13";
        wifi[3][1] = "-49"; // 7
        wifi[4][0] = "94:64:24:9f:12:00";
        wifi[4][1] = "-51"; // 11
        wifi[5][0] = "94:64:24:9f:03:e0";
        wifi[5][1] = "-55"; // 0
        wifi[6][0] = "94:64:24:9f:03:e2";
        wifi[6][1] = "-55"; // 0

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

                String pos = wifiData[0][2];
                double[] disList = new double[500];
                String[] disPos = new String[500];
                int count = 0;
                int[] check = new int[7];
                int num = 0;
                disPos[count] = pos;

                for (String[] WiFi : wifiData) {
//                    Log.e("pos", pos);
                    if(WiFi[2] == null) {
                        break;
                    }
                    if (!(WiFi[2].equals(pos))) {
                        disList[count] = disList[count] / num;
                        num = 0;
                        count++;
                        pos = WiFi[2];
                        disPos[count] = pos;
                        Log.e("dispos", disPos[count]);
                    }
                    num++;
                    int checkPos = 0;
                    for (ScanResult choseWifi : wifiResult) {
                        String MAC = choseWifi.BSSID;
                        if(MAC.equals((WiFi[0]))) {
                            int a = Integer.parseInt(WiFi[1]) - choseWifi.level;
                            a = a*a;
                            disList[count] += Math.sqrt(a);
                            checkPos = 1;
                            break;
                        }
                    }
                    if(checkPos == 0) {
                        int a = Integer.parseInt(WiFi[1]);
                        a = a*a;
                        disList[count] += Math.sqrt(a);
                    }
                }

                double min = disList[0];
                int index = 0;
                for (int i = 1; i < disList.length; i++) {
                    if(disList[i] == 0) {
                        break;
                    }
                    if (disList[i] < min) {
                        min = disList[i];
                        index = i;
                    }
                }

                if(!(mtoast == null)) {
                    mtoast.cancel();
                }

                mtoast = Toast.makeText(MainActivity.this, "distance = " + disPos[index], Toast.LENGTH_LONG);
                mtoast.show();
            }
        });

//
//        submitBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
//                    @Override
//                    public int compare(ScanResult o1, ScanResult o2) {
//                        return o2.level - o1.level;
//                    }
//                };
//                Collections.sort(wifiResult, comparator);
//                double distance = 0;
//                int[] check = new int[7];
//                for (ScanResult choseWifi : wifiResult) {
//                    String MAC = choseWifi.BSSID;
//                    for (int i = 0; i < 7; i++) {
//                        if(MAC.equals(wifi[i][0])) {
//                            int a = Integer.parseInt(wifi[i][1]) - choseWifi.level;
//                            Log.e("dBm1", choseWifi.BSSID + Integer.toString(choseWifi.level));
//                            a = a * a;
//                            distance += Math.sqrt(a);
//                            check[i] = 1;
//                            Log.e("distance", "distance = " + distance);
//                            break;
//                        }
//                    }
//                }
//                for (int i = 0; i < 7; i++) {
//                    if (!(check[i] == 1)) {
//                        int a = Integer.parseInt(wifi[i][1]);
//                        a = a * a;
//                        distance += Math.sqrt(a);
//                        check[i] = 1;
//                    }
//                }
//
//                Toast.makeText(MainActivity.this, "distance = " + distance, Toast.LENGTH_LONG).show();
//            }
//        });

    }

    //존재하는 DB 복사하기
    private void copyDatabaseFromAssets() {
        try {
            InputStream inputStream = getAssets().open("person.db");
            String outFileName = getDatabasePath("person.db").getPath();
            OutputStream outputStream = new FileOutputStream(outFileName);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[][] select() {
        db = dbHelper.getReadableDatabase();
        String[] columns = {"mac", "rss", "pos"};
        String[][] result = new String[300][5];
        int cursorPos = 0;

        //Cursor cursor = db.query("fingerprint", columns, null, null, null, null, null);
        Cursor cursor = db.rawQuery("Select * From fingerprint", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String macValue = cursor.getString(0);
                int rssValue = cursor.getInt(1);
                String posValue = cursor.getString(2);

                result[cursorPos][0] = macValue;
                result[cursorPos][1] = Integer.toString(rssValue);
                result[cursorPos][2] = posValue;

                cursorPos += 1;
            } while (cursor.moveToNext());
        }

        Log.i("result", result.toString());
        return result;
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

}