package co.armstart.openwicam;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.github.jksiezni.permissive.PermissionsGrantedListener;
import com.github.jksiezni.permissive.PermissionsRefusedListener;
import com.github.jksiezni.permissive.Permissive;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.List;

import static co.armstart.openwicam.Wicam.SIZEOF_DEV_INFO_T;
import static co.armstart.openwicam.Wicam.WicamBundles;

public class ScanActivity extends AppCompatActivity {

    // MARK: UI Properties
    protected ListView mScannedListView;
    protected FloatingActionButton mScanButton;
    protected FloatingActionButton mGalleryButton;
    protected ProgressBar   mScanprogressBar;

    protected WLANDiscoveryTask mWLANScanTask;
    protected boolean isPermitted = false;

    private boolean isScanStarted = false;
    private boolean isWLANScanDone = false;
    private boolean isWiFiScanDone = false;

    public Wicam[] mWicams = {};


    // MARK: Listeners
    protected View.OnClickListener mScanOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            startScanning();

            Snackbar.make(view, "Scanning started.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    };
    protected View.OnClickListener mGalleryOnClick = new View.OnClickListener() {
        @Override
        public  void onClick(View view) {
            //Snackbar.make(view, "Gallery View clicked", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            Intent it = new Intent(ScanActivity.this, GallaryActivity.class);
            startActivity(it);
        }
    };
    protected BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wm = (WifiManager) ScanActivity.this.getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> lsr = wm.getScanResults();
            boolean found = false;
            for (int i = 0; i < lsr.size(); i++) {
                Log.d("ScanActivity", "WiFi:" + lsr.get(i));
                if (!lsr.get(i).SSID.startsWith("\"WiCam-")) continue;
                if (!lsr.get(i).capabilities.toLowerCase().contains("wpa2")) continue;
                String ssid = lsr.get(i).SSID.substring(1, lsr.get(i).SSID.length()-1);
                Wicam wicam = Wicam.createIfNotExist(ssid, "192.168.240.1");
                Log.d("ScanActivity", "mBroadCastReceiver found Wicam: " + wicam);
                found = true;
            }
            isWiFiScanDone = true;
            if (isWLANScanDone) {
                // TODO: update UI List
                doRerenderScanItems();
            }
        }
    };

    private AdapterView.OnItemClickListener mListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d("@@@", "clicked " + position);
        }
    };

    protected void doRerenderScanItems() {
        mWicams = WicamBundles.values().toArray(mWicams);
        mScanprogressBar.setVisibility(View.INVISIBLE);
        if (mWicams.length == 0) {
            Snackbar.make(mScannedListView, "No Wicams found. Try again.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        mScannedListView.setAdapter(new ArrayAdapter<Wicam>(this, R.layout.scan_item, mWicams));
    }

    // MARK: Overrided methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Scanned List View
        mScannedListView = (ListView) findViewById(R.id.scanned_list);
        mScannedListView.setOnItemClickListener(mListClickListener);

        mScanprogressBar = (ProgressBar) findViewById(R.id.scan_progressBar);
        mScanprogressBar.setVisibility(View.INVISIBLE);

        // listen ScanButton click event
        mScanButton = (FloatingActionButton) findViewById(R.id.fab_scan);
        mScanButton.setOnClickListener(mScanOnClick);

        // Listen GalleryButton click event
        mGalleryButton = (FloatingActionButton) findViewById(R.id.fab_gallery);
        mGalleryButton.setOnClickListener(mGalleryOnClick);

    }

    private void startScanning () {
        Log.d("ScanActivity", "startScanning");
        if (isScanStarted) {
            stopScanning();
        }
        Log.d("ScanActivity", "Scan WiFi");
        // Do WiFi scanning
        startWifiScanning();
        // Do WLAN scanning
        startWLANScanning();
        isScanStarted = true;
        mScanprogressBar.setVisibility(View.VISIBLE);


    }

    private void stopScanning() {
        if (isScanStarted == false) return;
        stopWifiScanning();
        stopWLANScanning();
        isScanStarted = false;
        mScanprogressBar.setVisibility(View.INVISIBLE);
    }

    private void startWifiScanning() {
        isWiFiScanDone = false;
        registerReceiver(mBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        wm.startScan();
    }
    private void stopWifiScanning() {
        unregisterReceiver(mBroadcastReceiver);
    }
    private void startWLANScanning() {
        isWLANScanDone = false;
        mWLANScanTask = new WLANDiscoveryTask();
        mWLANScanTask.execute();
    }
    private void onWLANScanDone () {
        isWLANScanDone = true;
        if (isWiFiScanDone) {
            doRerenderScanItems();
        }
    }
    private void stopWLANScanning() {
        if (mWLANScanTask != null) {
            mWLANScanTask.cancel(false);
            mWLANScanTask = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("ScanActivity", "onStart");
        if (isPermitted == false) {
            requestPermissions(new PermissionsGrantedListener() {
                @Override
                public void onPermissionsGranted(String[] permissions) throws SecurityException {
                    isPermitted = true;
                }
            }, new PermissionsRefusedListener() {
                @Override
                public void onPermissionsRefused(String[] permissions) {
                    Snackbar.make(ScanActivity.this.mScanButton, permissions + " are not granted. Open Wicam may not function properly.", Snackbar.LENGTH_LONG)
                            .setAction("Permission Refused", null).show();
                }
            });
        } else {
            isPermitted = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ScanActivity", "onStop");
        stopScanning();
    }

    // MARK: Permission functions
    protected void requestPermissions(PermissionsGrantedListener granted, PermissionsRefusedListener refused) {
        new Permissive.Request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CONTACTS
        ).whenPermissionsGranted(granted).whenPermissionsRefused(refused).execute(this);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onScanButtonClicked(View view) {
        startScanning();
    }

    // AsyncTask
    public class WLANDiscoveryTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPostExecute(Void result) {
            onWLANScanDone();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Integer listenPort = 4277;
            Integer sendPort = 4211;
            byte dev_info[] = new byte[SIZEOF_DEV_INFO_T];
            try {
                Log.d("WLANDiscoveryTask", "Started");
                DatagramSocket listenSocket = new DatagramSocket(listenPort, InetAddress.getByName("0.0.0.0"));
                listenSocket.setBroadcast(true);
                listenSocket.setSoTimeout(10);
                if (listenSocket.getBroadcast() == false) {
                    return null;
                }
                byte[] wicam = "WiCam".getBytes();
                DatagramSocket sendSocket = new DatagramSocket();
                sendSocket.setBroadcast(true);
                DatagramPacket sp = new DatagramPacket(wicam,
                        wicam.length,
                        InetAddress.getByName("255.255.255.255"),
                        sendPort);
                sendSocket.send(sp);



                // Listen incoming broadcast
                int tm = 200;
                int count = 0;
                while (tm-- != 0 && isCancelled() == false) {

                    if (tm == 100) {
                        sendSocket.send(sp);
                    }
                    // listen for incoming data
                    DatagramPacket dp = new DatagramPacket(dev_info, dev_info.length);
                    try {
                        listenSocket.receive(dp);
                    } catch (SocketTimeoutException e) {
                        //Log.d("WicamDiscoveryTask", "Socket Timeout Exception");
                        continue;
                    }
                    Log.d("WicamDiscoveryTask", "Packet received from " + dp.getAddress().getHostAddress());
                    Log.d("WicamDiscoveryTask", "first_byte=" + String.format("%02X", dev_info[0]));
                    Log.d("WicamDiscoveryTask", "last_byte=" + String.format("%02X", dev_info[SIZEOF_DEV_INFO_T - 1]));
                    if (dev_info[0] != (byte)Wicam.DEV_INFO_SIG_START || dev_info[SIZEOF_DEV_INFO_T - 1] != (byte)Wicam.DEV_INFO_SIG_END) {
                        Log.e("WicamDiscoveryTask", "Invalid Discovery Packet.");
                        continue;
                    }
                    int i = 0;
                    for (i = 0; i < (Wicam.IP_LEN_MAX + 1) && dev_info[1 + i] != 0; i++) { }
                    if (i == (Wicam.IP_LEN_MAX + 1)) {
                        continue;
                    }
                    final String rcvdAddress = new String(dev_info, 1, i, "US-ASCII");
                    if (rcvdAddress.equals(dp.getAddress().getHostAddress()) == false) {
                        continue;
                    }
                    for (i = 0; i < (Wicam.SSID_LEN_MAX + 1) && dev_info[17 + i] != 0; i++) { }
                    if (i <= 6 || i == (Wicam.SSID_LEN_MAX + 1)) {
                        continue;
                    }
                    final String ssid = new String(dev_info, 17, i, "US-ASCII");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("runOnUiThread", "Adding " + ssid + " " + rcvdAddress);
                            Wicam.createIfNotExist(ssid, rcvdAddress);
                        }
                    });
                    count++;
                }
                listenSocket.close();
                sendSocket.close();

            } catch (SocketException e) {
                Log.d("WLANDiscoveryTask", "SocketException: " + e);
            } catch (UnknownHostException e) {
                Log.d("WLANDiscoveryTask", "UnknownHostException: " + e);
            } catch (IOException e) {
                Log.d("WLANDiscoveryTask", "IOException: " + e);
            }
            return null;
        }
    }
}
