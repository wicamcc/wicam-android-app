/**
 * Created by yliu on 9/14/16.
 */

package co.armstart.openwicam;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;


import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Wicam {

    public final static int SIZEOF_DEV_INFO_T = 69;
    public final static int SSID_LEN_MAX = 32;
    public final static int IP_LEN_MAX = 16;
    public final static byte DEV_INFO_SIG_START = (byte)0xCA;
    public final static byte DEV_INFO_SIG_END = (byte)0x12;

    public final static int MAIN_CONF_T_SIZE = 129;

    public final static int STATUS_NULL = 0;
    public final static int STATUS_DISCONNECTED = 1;
    public final static int STATUS_CONNECTED = 2;
    public final static int STATUS_SIGNEDIN = 3;
    public final static int STATUS_VIDEO_MODE = 4;
    public final static int STATUS_PICTURE_MODE = 5;

    public int mStatus = STATUS_NULL;

    public byte   mFWVerssion = 0;
    public String mIP = "";
    public String mLANIP = "";
    public String mApSSID = "";
    public String mApPassword = "";
    public String mStaSSID = "";
    public String mStaPassword = "";
    public String mWanIP = "";
    public int    mWanPort = 0;

    public WicamDelegate mDelegate = null;

    public WebSocket mWebsocketClient;

    public static Hashtable<String, Wicam> WicamBundles;

    public static boolean SavedLoaded;

    public static File MEDIA_PATH = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Wicam");

    public static WebSocketFactory WSFACTORY = new WebSocketFactory();

    protected WebSocketListener mWSListener = new WebSocketAdapter() {


        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            mStatus = STATUS_CONNECTED;
            mDelegate.onConnected();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {

        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {

        }


        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
            mStatus = STATUS_DISCONNECTED;
            mDelegate.onDisconnected();
        }

    };


    static {
        WicamBundles = new Hashtable<>();
    }

    public interface WicamDelegate {
        void onConnected();
        void onDisconnected();
        void onSignin (boolean success);
        void onClosed ();

    }

    public void setIP (String ip) {
        if (ip == "192.168.240.1") {
            if (mIP == "" || mIP.contains(":")) {
                // If mIP is empty or mIP is set to wan IP. but we detected Hotspot, then set it back to Hotspot.
                mIP = ip;
            }
        } else {
            mIP = ip;
        }
    }

    public boolean isRemote () {
        return mIP.contains(":");
    }
    public boolean isHotspot () {
        return mIP == "192.168.240.1";
    }
    public boolean isWLAN () {
        return !isRemote() && !isHotspot();
    }



    public void commitChanges () {
        ContextWrapper context = WicamApplication.getAppContextWrapper();
        SharedPreferences.Editor editor = context.getSharedPreferences("Wicam", Context.MODE_PRIVATE).edit();
        editor.remove(mApSSID);
        try {
            JSONObject json = new JSONObject();
            json.put(context.getString(R.string.ap_ssid), mApSSID);
            json.put(context.getString(R.string.ap_pin), mApPassword);
            json.put(context.getString(R.string.sta_ssid), mStaSSID);
            json.put(context.getString(R.string.sta_pin), mStaPassword);
            if (isWLAN()) {
                json.put(context.getString(R.string.lan_address), mIP);
            }
            if (!mWanIP.isEmpty()) {
                json.put(context.getString(R.string.wan_address), mWanIP);
            }
            if (mWanPort != 0) {
                json.put(context.getString(R.string.wan_port), mWanPort);
            }
            editor.putString(mApSSID, json.toString());
        } catch (JSONException e) {}
        editor.commit();
        //editor.putString(mApSSID, )
    }

    protected void onSignin(ByteBuffer bytes) {

        if (bytes.limit() != MAIN_CONF_T_SIZE) {
            if (mDelegate != null) mDelegate.onSignin(false);
        }
        mFWVerssion = bytes.get(0);
        byte[] tmp_bytes = new byte[33];
        bytes.get(tmp_bytes, 4, 33);
        String ap_ssid = new String(tmp_bytes, Charset.forName("UTF-8"));
        bytes.get(tmp_bytes, 37, 13);
        String ap_pin = new String(tmp_bytes, Charset.forName("UTF-8"));
        bytes.get(tmp_bytes, 50, 33);
        String sta_ssid = new String(tmp_bytes, Charset.forName("UTF-8"));
        bytes.get(tmp_bytes, 83, 13);
        String sta_pin = new String(tmp_bytes, Charset.forName("UTF-8"));
        mApSSID = ap_ssid;
        mApPassword = ap_pin;
        mStaSSID = sta_ssid;
        mStaPassword = sta_pin;
    }

    protected void onDisconnected() {

    }

    protected void onError() {

    }

    protected void onVideoFrame(ByteBuffer bytes) {

    }
    protected void onPictureFrame(ByteBuffer bytes) {

    }


    public void connect() throws IOException, IllegalArgumentException {
        if (mWebsocketClient != null) {
            mWebsocketClient.disconnect();
            mWebsocketClient = null;
        }
        mWebsocketClient = WSFACTORY.createSocket("ws://" + mApSSID + mApPassword, 5000);
        mWebsocketClient.addListener(mWSListener);
        mWebsocketClient.connectAsynchronously();
    }
    public void Signin() {

    }


    @Override
    public String toString() {
        if (isRemote()) return "[Remote] " + mApSSID;
        if (isWLAN()) return "[Home] " + mApSSID;
        if (isHotspot()) return "[Hotspot] " + mApSSID;
        return mApSSID;
    }

    public static void loadSavedWicams() {
        if (SavedLoaded == true) return;
        SavedLoaded = true;
        ContextWrapper context = WicamApplication.getAppContextWrapper();
        Log.d("Wicam", "Application context: " + context);
        Map<String, ?> saved =context.getSharedPreferences("Wicam", Context.MODE_PRIVATE).getAll();

        for(Map.Entry<String,?> entry : saved.entrySet()){
            String ssid = entry.getKey();
            String value = entry.getValue().toString();
            Wicam wicam = fromJSONString(value);
            WicamBundles.put(ssid, wicam);
        }
    }
    public static Wicam createIfNotExist (String ssid, String ip) {
        ContextWrapper context = WicamApplication.getAppContextWrapper();
        if (!WicamBundles.containsKey(ssid)) {
            String jsonString = context.getSharedPreferences("Wicam", Context.MODE_PRIVATE).getString(ssid, null);
            Wicam wicam = fromJSONString(jsonString, ssid, ip);
            WicamBundles.put(ssid, wicam);
            wicam.commitChanges();
            return wicam;
        }

        Wicam wicam = WicamBundles.get(ssid);
        wicam.setIP(ip);
        wicam.commitChanges();
        return wicam;
    }

    public static Wicam fromJSONObject (JSONObject json) throws JSONException {
        ContextWrapper context = WicamApplication.getAppContextWrapper();
        Wicam wicam = new Wicam();
        wicam.mApSSID = json.getString(context.getString(R.string.ap_ssid));
        wicam.mApPassword = json.getString(context.getString(R.string.ap_pin));
        if (json.has(context.getString(R.string.sta_ssid))) {
            wicam.mStaSSID = json.getString(context.getString(R.string.sta_ssid));
        }
        if (json.has(context.getString(R.string.sta_pin))) {
            wicam.mStaPassword = json.getString(context.getString(R.string.sta_pin));
        }
        if (json.has(context.getString(R.string.wan_address))) {
            wicam.mWanIP = json.getString(context.getString(R.string.wan_address));
        }
        if (json.has(context.getString(R.string.wan_port))) {
            wicam.mWanPort = json.getInt(context.getString(R.string.wan_port));
        }
        if (wicam.mWanIP.isEmpty() == false && wicam.mWanPort != 0) {
            wicam.mIP = wicam.mWanIP + ":" + wicam.mWanPort;
        }
        if (json.has(context.getString(R.string.lan_address))) {
            wicam.mLANIP = json.getString(context.getString(R.string.lan_address));
        }
        return wicam;
    }

    public static Wicam fromJSONString (String jsonString, String new_ssid, String new_ip) {
        Wicam wicam;
        ContextWrapper context = WicamApplication.getAppContextWrapper();
        try {
            JSONObject json = new JSONObject(jsonString);
            wicam = fromJSONObject(json);
        } catch (Exception e) {
            context.getSharedPreferences("Wicam", Context.MODE_PRIVATE).edit().remove(new_ssid).commit();
            wicam = new Wicam(); wicam.mIP = new_ip; wicam.mApSSID = new_ssid; wicam.mApPassword = "wicam.cc";
        }
        return wicam;
    }
    public static Wicam fromJSONString (String jsonString) {
        Wicam wicam;
        try {
            JSONObject json = new JSONObject(jsonString);
            wicam = fromJSONObject(json);
        } catch (JSONException e) {
            return null;
        }
        return wicam;
    }
}
