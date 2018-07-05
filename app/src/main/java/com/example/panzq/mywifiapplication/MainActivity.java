package com.example.panzq.mywifiapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.IpConfiguration;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.security.Credentials;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.panzq.mywifiapplication.adapter.WifiAdapter;
import com.example.panzq.mywifiapplication.wifi.LinkWifi;
import com.example.panzq.mywifiapplication.wifi.MScanWifi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    public WifiManager mWifiManager;
    public List<MScanWifi> mScanWifiList;
    public List<ScanResult> mWifiList;
    public List<WifiConfiguration> mWifiConfiguration;
    public Context context = null;
    public Scanner mScanner;
    public View view;
    public TextView wifi_status_txt;
    public Switch wifiSwitch;
    public ListView listView;
    private IntentFilter mFilter;
    private LinkWifi linkWifi;
    public LayoutInflater Inflater;
    private LinearLayout layout;
    private WifiAdapter mWifiAdapter;

    private Spinner spEapMethod;
    private Spinner spPhase2;
    private Spinner spCACertificate;
    private Spinner spUserCert;
    private EditText etIdentity;
    private EditText etAnnoymous;
    private EditText etPassword;
    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_NONE = 0;
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2 = 1;
    public static final int WIFI_PEAP_PHASE2_GTC = 2;
    private String unspecifiedCert;
    private Spinner spProxy;

    private WifiManager.ActionListener mConnectListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 1);
        }

        context = this;
        Inflater = LayoutInflater.from(context);
        mWifiManager = (WifiManager) context.getSystemService(Service.WIFI_SERVICE);
        mScanner = new Scanner(this);
        linkWifi = new LinkWifi(context);
        mConnectListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
                Activity activity = MainActivity.this;
                if (activity != null) {
                    Toast.makeText(activity,
                            "......",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(int reason) {
                Activity activity = MainActivity.this;
                if (activity != null) {
                    Toast.makeText(activity,
                            "无法连接到网络",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        initView();
        initIntentFilter();
        registerListener();
        registerBroadcast();

    }

    public void initIntentFilter() {
        // TODO Auto-generated method stub
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        //用户操作activity时更新UIs
        mScanner.forceScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        context.unregisterReceiver(mReceiver);
    }

    public void initView() {
        // TODO Auto-generated method stub
        //获得要加载listview的布局
        layout = (LinearLayout) findViewById(R.id.ListView_LinearLayout);
        //动态获得listview布局
        listView = (ListView) Inflater.inflate(
                R.layout.my_listview, null).findViewById(R.id.mlistview);
        wifi_status_txt = (TextView) findViewById(R.id.switch_txt);
        wifiSwitch = (Switch) findViewById(R.id.switch_status);
        layout.addView(listView);
        initWifiData();
    }

    public void registerListener() {
        // TODO Auto-generated method stub
        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if (buttonView.isChecked()) {
                    wifi_status_txt.setText("开启");

                    if (!mWifiManager.isWifiEnabled()) { // ��ǰwifi������
                        mWifiManager.setWifiEnabled(true);
                    }
                    mWifiManager.startScan();

                } else {
                    wifi_status_txt.setText("关闭");
                    if (mWifiManager.isWifiEnabled()) {
                        mWifiManager.setWifiEnabled(false);
                    }

                }
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                // 本机已经配置过的wifi
                final ScanResult wifi = mScanWifiList.get(position).scanResult;
                final WifiConfiguration wifiConfig = linkWifi.IsExsits(wifi.SSID);
                if (wifiConfig != null) {
                    final int netID = wifiConfig.networkId;
                    String actionStr;
                    // 如果目前连接了此网络
                    if (mWifiManager.getConnectionInfo().getNetworkId() == netID) {
                        actionStr = "断开";
                    } else {
                        actionStr = "连接";
                    }
                    android.app.AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("提示");
                    builder.setMessage("请选择你要进行的操作？");
                    builder.setPositiveButton(actionStr,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {

                                    if (mWifiManager.getConnectionInfo()
                                            .getNetworkId() == netID) {
                                        mWifiManager.disconnect();
                                    } else {

                                        linkWifi.setMaxPriority(wifiConfig);
                                        linkWifi.ConnectToNetID(wifiConfig.networkId);
                                    }

                                }
                            });
                    builder.setNeutralButton("忘记",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    mWifiManager.removeNetwork(netID);
                                    return;
                                }
                            });
                    builder.setNegativeButton("取消",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    return;
                                }
                            });
                    builder.show();

                    return;

                }
                if (mScanWifiList.get(position).getIsLock()) {
                    // 有密码，提示输入密码进行连接

                    // final String encryption = capabilities;

                    LayoutInflater factory = LayoutInflater.from(context);
                    final View inputPwdView = factory.inflate(R.layout.dialog_inputpwd,
                            null);
                    if (wifi.capabilities.contains("WPA-EAP")) {
                        createEapDailog(wifi, factory);
                    } else {
                        //createEapDailog(wifi, factory);
                        new AlertDialog.Builder(context)
                                .setTitle("请输入该无线的连接密码")
                                .setMessage("无线SSID：" + wifi.SSID)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setView(inputPwdView)
                                .setPositiveButton("确定",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                EditText pwd = (EditText) inputPwdView
                                                        .findViewById(R.id.etPassWord);
                                                String wifipwd = pwd.getText().toString();

                                                // 此处加入连接wifi代码
                                                int netID = linkWifi.CreateWifiInfo2(
                                                        wifi, wifipwd);

                                                linkWifi.ConnectToNetID(netID);
                                            }
                                        })
                                .setNegativeButton("取消",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                            }
                                        }).setCancelable(false).show();
                    }

                } else {
                    // 无密码
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("你选择的wifi无密码，可能不安全，确定继续连接？")
                            .setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int whichButton) {

                                            // 此处加入连接wifi代码
                                            int netID = linkWifi.CreateWifiInfo2(
                                                    wifi, "");

                                            linkWifi.ConnectToNetID(netID);
                                        }
                                    })
                            .setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int whichButton) {
                                            return;
                                        }
                                    }).show();

                }

            }

        });
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                mWifiAdapter.setSelectedItem(position);
                mWifiAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    /**
     * 获取到自定义的ScanResult
     **/
    public void initScanWifilist() {
        MScanWifi mScanwifi;
        mScanWifiList = new ArrayList<MScanWifi>();
        for (int i = 0; i < mWifiList.size(); i++) {
            int level = WifiManager.calculateSignalLevel(mWifiList.get(i).level, 4);
            String mwifiName = mWifiList.get(i).SSID;
            boolean boolean1 = false;
            if (mWifiList.get(i).capabilities.contains("WEP") || mWifiList.get(i).capabilities.contains("PSK") ||
                    mWifiList.get(i).capabilities.contains("EAP")) {
                boolean1 = true;
            } else {
                boolean1 = false;
            }
            mScanwifi = new MScanWifi(mWifiList.get(i), mwifiName, level, boolean1);
            if (linkWifi.IsExsits(mwifiName) != null) {
                mScanwifi.setIsExsit(true);
            } else {
                mScanwifi.setIsExsit(false);
            }
            mScanWifiList.add(mScanwifi);
        }
        if (mScanWifiList.size() > 0) {
            mWifiAdapter = new WifiAdapter((ArrayList<MScanWifi>) mScanWifiList, MainActivity.this);
            listView.setAdapter(mWifiAdapter);
        }
    }

    public void registerBroadcast() {
        context.registerReceiver(mReceiver, mFilter);
    }

    public void unregisterBroadcast() {
        context.unregisterReceiver(mReceiver);
    }

    /**
     * 广播接收，监听网络
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleEvent(context, intent);
        }
    };

    public void handleEvent(Context context, Intent intent) {
        // TODO Auto-generated method stub
        final String action = intent.getAction();
        // wifi状态发生改变。
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
			/*int wifiState = intent.getIntExtra(
					WifiManager.EXTRA_WIFI_STATE, 0);*/
            int wifiState = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            updateWifiStateChanged(wifiState);
        } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            updateWifiList();

        }
    }

    /**
     * 更新WiFi列表UI
     **/
    public void updateWifiList() {

        final int wifiState = mWifiManager.getWifiState();
        //获取WiFi列表并显示
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                //wifi处于开启状态
                mWifiList = mWifiManager.getScanResults();
                Log.d("panzqww", "mWifiList.size == " + mWifiList.size());
                mWifiConfiguration = mWifiManager.getConfiguredNetworks();
                initScanWifilist();
                //mWifiAdapter.notifyDataSetChanged();
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                listView.setAdapter(null);
                break;//如果WiFi处于正在打开的状态，则清除列表
        }
    }

    /**
     * 初始化wifi信息
     * mWifiList和mWifiConfiguration
     **/
    public void initWifiData() {
        // TODO Auto-generated method stub
        mWifiList = mWifiManager.getScanResults();
        Log.d("panzqww", "mWifiList.size == " + mWifiList.size());
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
        initScanWifilist();
    }

    private void updateWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:// 正在打开WiFi
                wifiSwitch.setEnabled(false);
                Log.i("aaaaaa", "正在打开WiFi");
                break;
            case WifiManager.WIFI_STATE_ENABLED:// WiFi已经打开
                //setSwitchChecked(true);
                wifiSwitch.setEnabled(true);
                wifiSwitch.setChecked(true);
                layout.removeAllViews();
                layout.addView(listView);
                mScanner.resume();
                Log.i("aaaaaa", "WiFi已经打开");
                break;
            case WifiManager.WIFI_STATE_DISABLING://正在关闭WiFi
                wifiSwitch.setEnabled(false);
                Log.i("aaaaaa", "正在关闭WiFi");
                break;
            case WifiManager.WIFI_STATE_DISABLED://WiFi已经关闭
                //setSwitchChecked(false);
                wifiSwitch.setEnabled(true);
                wifiSwitch.setChecked(false);
                layout.removeAllViews();
                Log.i("aaaaaa", "WiFi已经关闭 ");
                break;
            default:
                //setSwitchChecked(false);
                wifiSwitch.setEnabled(true);
                break;
        }
        mScanner.pause();//移除message通知
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                }
                break;
        }
    }

    /**
     * 这个类使用startScan()方法开始扫描wifi
     * WiFi扫描结束时系统会发送该广播，
     * 用户可以监听该广播通过调用WifiManager
     * 的getScanResults方法来获取到扫描结果
     *
     * @author zwh
     */
    public static class Scanner extends Handler {
        private final WeakReference<MainActivity> mActivity;
        private int mRetry = 0;

        public Scanner(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mActivity.get().mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                return;
            }
            sendEmptyMessageDelayed(0, 10 * 1000);//10s后再次发送message
        }

    }

    private void initEapDialogView(View inputEapView) {
        spEapMethod = (Spinner) inputEapView
                .findViewById(R.id.sp_eap_method);
        spPhase2 = (Spinner) inputEapView
                .findViewById(R.id.sp_phase2_auth);
        spCACertificate = (Spinner) inputEapView
                .findViewById(R.id.sp_ca_certificate);
        spUserCert = (Spinner) inputEapView
                .findViewById(R.id.sp_user_certificate);
        etIdentity = (EditText) inputEapView
                .findViewById(R.id.et_identity);
        etAnnoymous = (EditText) inputEapView
                .findViewById(R.id.et_annoymous);
        etPassword = (EditText) inputEapView
                .findViewById(R.id.et_password);
        //spProxy = (Spinner) inputEapView.findViewById(R.id.sp_proxy);
        loadCertificates(spCACertificate, Credentials.CA_CERTIFICATE);
        loadCertificates(spUserCert, Credentials.USER_PRIVATE_KEY);
    }


    private void createEapDailog(final ScanResult wifi, LayoutInflater factory) {
        final View inputEapView = factory.inflate(R.layout.dialog_inputeap,
                null);
        new AlertDialog.Builder(context)
                .setTitle("802.1xEAP")
                .setMessage("无线SSID：" + wifi.SSID)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(inputEapView)
                .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        initEapDialogView(inputEapView);
                        WifiConfiguration config = new WifiConfiguration();
                        config.SSID = wifi.SSID;
                        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                        config.enterpriseConfig = new WifiEnterpriseConfig();
                        int eapMethod = spEapMethod.getSelectedItemPosition();
                        int phase2Method = spPhase2.getSelectedItemPosition();
                        config.enterpriseConfig.setEapMethod(eapMethod);
                        Log.d("panzqww", "-----eapMethod-----" + eapMethod);
                        Log.d("panzqww", "-----phase2Method-----" + phase2Method);
                        switch (eapMethod) {
                            case WifiEnterpriseConfig.Eap.PEAP:
                                // PEAP supports limited phase2 values
                                // Map the index from the PHASE2_PEAP_ADAPTER to the one used
                                // by the API which has the full list of PEAP methods.
                                switch (phase2Method) {
                                    case WIFI_PEAP_PHASE2_NONE:
                                        config.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.NONE);
                                        break;
                                    case WIFI_PEAP_PHASE2_MSCHAPV2:
                                        config.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
                                        break;
                                    case WIFI_PEAP_PHASE2_GTC:
                                        config.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.GTC);
                                        break;
                                    default:
                                        Log.e("panzqww", "Unknown phase2 method" + phase2Method);
                                        break;
                                }
                            default:
                                // The default index from PHASE2_FULL_ADAPTER maps to the API
                                config.enterpriseConfig.setPhase2Method(phase2Method);
                                break;
                        }
                        String caCert = (String) spCACertificate.getSelectedItem();
                        Log.d("panzqww", "----caCert ==== " + caCert);
                        if (caCert.equals(unspecifiedCert)) caCert = "";
                        config.enterpriseConfig.setCaCertificateAlias(caCert);
                        String clientCert = (String) spUserCert.getSelectedItem();
                        if (clientCert.equals(unspecifiedCert)) clientCert = "";
                        Log.d("panzqww", "------clientCert ====" + clientCert);
                        config.enterpriseConfig.setClientCertificateAlias(clientCert);
                        config.enterpriseConfig.setIdentity(etIdentity.getText().toString());
                        config.enterpriseConfig.setAnonymousIdentity(
                                etAnnoymous.getText().toString());
                        config.enterpriseConfig.setPassword(etPassword.getText().toString());
                        config.setProxySettings(IpConfiguration.ProxySettings.NONE);
                        config.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
                        Log.d("panzqww", "Identity ==== " + etIdentity.getText().toString());
                        Log.d("panzqww", "AnonymousIdentity ==== " + etAnnoymous.getText().toString());
                        Log.d("panzqww", "getProxySettings ==== " + config.getProxySettings());
                        Log.d("panzqww", "getIpAssignment ==== " + config.getIpAssignment());
			config.networkId = -1;
                        int wcgId = mWifiManager.addNetwork(config);
                        Log.d("panzqww", " wcgId==== " + wcgId);
                        mWifiManager.enableNetwork(wcgId, true);
                       // mWifiManager.save(config, mConnectListener);
                        //mWifiManager.connect(config, mConnectListener);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).setCancelable(false).show();
    }

    private WifiConfiguration getEapCofnig(ScanResult wifi) {
        WifiConfiguration config = new WifiConfiguration();
        int priority;
        config = new WifiConfiguration();
        /* 清除之前的连接信息 */
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + wifi.SSID + "\"";
        priority = getMaxPriority() + 1;
        if (priority > 99999) {
            priority = shiftPriorityAndSave();
        }
        config.priority = priority; // 2147483647;
        /*Log.w("panzqww", "WPA_EAP加密，密码" + password);
        config.preSharedKey = "\"" + password + "\"";*/
        config.hiddenSSID = true;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);

        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        return config;
    }

    private int getMaxPriority() {
        List<WifiConfiguration> localList = mWifiManager
                .getConfiguredNetworks();
        int i = 0;
        Iterator<WifiConfiguration> localIterator = localList.iterator();
        while (true) {
            if (!localIterator.hasNext())
                return i;
            WifiConfiguration localWifiConfiguration = (WifiConfiguration) localIterator
                    .next();
            if (localWifiConfiguration.priority <= i)
                continue;
            i = localWifiConfiguration.priority;
        }
    }

    private int shiftPriorityAndSave() {
        List<WifiConfiguration> localList = mWifiManager
                .getConfiguredNetworks();
        sortByPriority(localList);
        int i = localList.size();
        for (int j = 0; ; ++j) {
            if (j >= i) {
                mWifiManager.saveConfiguration();
                return i;
            }
            WifiConfiguration localWifiConfiguration = (WifiConfiguration) localList
                    .get(j);
            localWifiConfiguration.priority = j;
            mWifiManager.updateNetwork(localWifiConfiguration);
        }
    }

    private void sortByPriority(List<WifiConfiguration> paramList) {
        Collections.sort(paramList, new WifiManagerCompare());
    }

    class WifiManagerCompare implements Comparator<WifiConfiguration> {
        public int compare(WifiConfiguration paramWifiConfiguration1,
                           WifiConfiguration paramWifiConfiguration2) {
            return paramWifiConfiguration1.priority
                    - paramWifiConfiguration2.priority;
        }
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        unspecifiedCert = "unspecified";
        //String[] certs = KeyStore.getInstance().saw(prefix, android.os.Process.WIFI_UID);
        String[] certs = null;
        if (certs == null || certs.length == 0) {
            certs = new String[]{unspecifiedCert};
        } else {
            final String[] array = new String[certs.length + 1];
            array[0] = unspecifiedCert;
            System.arraycopy(certs, 0, array, 1, certs.length);
            certs = array;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}
