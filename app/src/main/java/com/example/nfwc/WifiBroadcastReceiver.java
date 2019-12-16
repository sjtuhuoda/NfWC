package com.example.nfwc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class WifiBroadcastReceiver extends BroadcastReceiver {
    private WifiManager mWifiManager;
    private Context receiverContext;
    public List<ScanResult> scanResults=null;
    boolean flag;

    public WifiBroadcastReceiver(Context context){
        receiverContext=context;
        mWifiManager=context==null ? null : (WifiManager)receiverContext.getApplicationContext().getSystemService(context.WIFI_SERVICE);
        scanResults=new LinkedList<ScanResult>();
        flag=false;
        IntentFilter filter=new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);//wifi开关变化广播
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);//热点扫描结果通知广播
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);//—热点连接结果通知广播
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);  //—网络状态变化广播（与上一广播协同完成连接过程通知）
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        receiverContext.registerReceiver(this,filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action){
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                //检测当前WiFi状态
                int wifiState=intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        //Log.d("receiver", "wifi disabled");
                        //OpenWifi();
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        //Log.d("receiver", "wifi enabled");
                        //startScan();
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        break;
                    default:
                        break;
                }
            case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                //getScanResult();
                break;
            default:
                break;
        }
    }

    public String Connect(String targetSSID,String password){
        //targetSSID="\""+targetSSID+"\"";
        password="\""+password+"\"";
        OpenWifi();
        startScan();
        getScanResult();
        ScanResult targetWifi=findWifiBySSID(targetSSID);
        int netId=findInConfiguration(targetWifi);
        WifiInfo info=mWifiManager.getConnectionInfo();
        if(info!=null){
            Log.d("wifiinfo",info.getSSID());
            if(info.getSSID().equals("\""+targetSSID+"\"")){
                return "This wifi is connecting";
            }
            else{
                mWifiManager.disconnect();
            }
        }

        //没找到对应wifi
        if(targetWifi==null){
            return "There is no wifi named "+targetSSID;
        }


        if(netId!=-1){
            //已保存过相应信息
            mWifiManager.disconnect();
            mWifiManager.enableNetwork(netId,true);
            return "Connected";
        }
        else{
            //找到对应wifi，但未连接过，构造config
            WifiConfiguration config=createWifiConfig(targetWifi,password);
            mWifiManager.addNetwork(config);
            List<WifiConfiguration> tmp=mWifiManager.getConfiguredNetworks();
            for(WifiConfiguration i:tmp){
                if(!TextUtils.isEmpty(i.SSID)&&TextUtils.equals(i.SSID,targetWifi.SSID)){
                    mWifiManager.disconnect();
                    mWifiManager.enableNetwork(i.networkId,true);
                    break;
                }
            }
            return "Connected new";
        }

    }

    private void OpenWifi(){
        if(!mWifiManager.isWifiEnabled()){
            Log.d("wifi", "try to open wifi");
            mWifiManager.setWifiEnabled(true);
        }
        else{
            Log.d("wifi", "wifi has been opened");
        }
    }

    private void startScan(){
        mWifiManager.startScan();
        Log.d("wifi","start scan");
    }

    private void getScanResult(){
        scanResults.clear();
        scanResults=mWifiManager.getScanResults();
        Log.d("scanResult get",Integer.toString(scanResults.size()));
        flag=true;
    }

    //Search wifi by SSID in scanResults
    private ScanResult findWifiBySSID(String targetSSID){
        ScanResult targetWifi=null;
        Log.d("find in scanResult",Integer.toString(scanResults.size()));
        for(ScanResult tempScanResult:scanResults){
            if(tempScanResult.SSID.equals(targetSSID)){
                targetWifi=tempScanResult;

            }
            //Log.d("find in scanResult",tempScanResult.SSID);
        }
        return targetWifi;
    }

    //Judge whether this wifi has been configured before
    //Return netId or -1
    private int findInConfiguration(ScanResult targetWifi){
        int netId=-1;
        List<WifiConfiguration> tempList=mWifiManager.getConfiguredNetworks();

        if(targetWifi==null){
            return -1;
        }

        for(WifiConfiguration c:tempList){
            if(c.SSID.equals("\""+targetWifi.SSID+"\"")){
                netId=c.networkId;
            }
            //Log.d("find in configuration",c.SSID);
        }
        //Log.d("find in configuration",targetWifi.SSID);
        return netId;
    }

    //Get the SSID of connecting wifi
    private String getCurrentSSID(){
        return "";
    }

    private WifiConfiguration createWifiConfig(ScanResult scanResult,String password){
        WifiConfiguration config=new WifiConfiguration();
        Log.d("create config",scanResult.SSID);
        config.SSID=scanResult.SSID;
        Log.d("create config",config.SSID);
        String capabilities=scanResult.capabilities;

        if(!TextUtils.isEmpty(capabilities)){
            if(capabilities.contains("WPA")||capabilities.contains("wpa")){
                config.preSharedKey=password;
            }
            else if(capabilities.contains("WEP")||capabilities.contains("wep")){
                config.wepKeys[0]=password;
                config.wepTxKeyIndex=0;
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            }
            else{
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
        }
        return config;
    }
}