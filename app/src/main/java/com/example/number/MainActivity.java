package com.example.number;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.Editable;
import android.text.Selection;
import android.text.format.Formatter;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.number.MVVM.VM.NPNHomeViewModel;
import com.example.number.MVVM.View.NPNHomeView;
import com.example.number.Network.ApiResponseListener;
import com.example.number.Network.VolleyRemoteApiClient;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.content.Intent;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.Locale;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity implements View.OnClickListener, NPNHomeView, OnInitListener{

    private static final String TAG = "NPNIoTs";
    private int DATA_CHECKING = 0;
    private TextToSpeech niceTTS;

    //GPIO Configuration Parameters
    private static final String LED_PIN_NAME = "BCM26"; // GPIO port wired to the LED
    private Gpio mLedGpio;

    //SPI Configuration Parameters
    private static final String SPI_DEVICE_NAME = "SPI0.1";
    private SpiDevice mSPIDevice;
    private static final String CS_PIN_NAME = "BCM12"; // GPIO port wired to the LED
    private Gpio mCS;


    // UART Configuration Parameters
    private static final int BAUD_RATE = 115200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;
    private UartDevice mUartDevice;

    byte[] test_data = new byte[]{0,(byte)0x8b,0,0};


    private String DOOR_OPEN = "1";
    private String DOOR_CLOSE = "0";


    public enum DOOR_STATE{
        NONE, WAIT_DOOR_OPEN, WAIT_DOOR_CLOSE, DOOR_OPENED, DOOR_CLOSED
    }
    DOOR_STATE door_state = DOOR_STATE.NONE;
    private int door_timer = 0;
    private int TIME_OUT_DOOR_OPEN = 3;

    private static final int CHUNK_SIZE = 512;

    NPNHomeViewModel mHomeViewModel; //Request server object
    Timer mBlinkyTimer;             //Timer
    Timer mDeletePress;

//    private VideoView myVideoView;
//    private TextView txtClock;
    private TextView txtIPAddress;
    private EditText txtPinCode;

//    private ImageView imgLogo;
    private ImageView imgWifi;

    private Button btnNum0, btnNum1, btnNum2, btnNum3, btnNum4, btnNum5, btnNum6, btnNum7, btnNum8, btnNum9;
    private Button btnDelete;

    private Button btnEnter;
    private Button btnClear;
    private Button btnShow;
    private TextView time, date;
    private boolean togglePassword = false;
    private WifiManager wifi;
    private Context context = this;
    private int level = 0;
    private ImageButton back;
    private Handler initTimeAndWifi = new Handler();
    private int idleCount = 0;
    private int codeLength = 0;
    private Handler mIdle = new Handler();
    private boolean idle = false;

    private boolean isAllowProcess = true;

    private String link = "http://192.168.0.188:3000/api/android/android?code=";

    long lastDown, lastDuration;
    boolean isButtonDeletePress = false;

    int testCounter = 0;
    String name = "KSD";


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //do they have the data
        if (requestCode == DATA_CHECKING) {
            //yep - go ahead and instantiate
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
                niceTTS = new TextToSpeech(this, this);
                //no data, prompt to install it
            else {
                Intent promptInstall = new Intent();
                promptInstall.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(promptInstall);
            }
        }
    }

    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS) {
            niceTTS.setLanguage(Locale.forLanguageTag("VI"));
        }
    }

//    private void visibleAllControls(boolean isVisible){
//        if(isVisible == false) {
//            btnNum0.setVisibility(View.GONE);
//            btnNum1.setVisibility(View.GONE);
//            btnNum2.setVisibility(View.GONE);
//            btnNum3.setVisibility(View.GONE);
//            btnNum4.setVisibility(View.GONE);
//            btnNum5.setVisibility(View.GONE);
//            btnNum6.setVisibility(View.GONE);
//            btnNum7.setVisibility(View.GONE);
//            btnNum8.setVisibility(View.GONE);
//            btnNum9.setVisibility(View.GONE);
//            btnDelete.setVisibility(View.GONE);
//            btnEnter.setVisibility(View.GONE);
//            imgWifi.setVisibility(View.GONE);
////            imgLogo.setVisibility(View.GONE);
////            txtClock.setVisibility(View.GONE);
//            txtPinCode.setVisibility(View.GONE);
//            txtIPAddress.setVisibility(View.GONE);
//
//            btnClear.setVisibility(View.GONE);
//            btnShow.setVisibility(View.GONE);
//            time.setVisibility(View.GONE);
//            date.setVisibility(View.GONE);
//            back.setVisibility(View.GONE);
//
//            myVideoView.setVisibility(View.VISIBLE);
//            myVideoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kitkat));
//
//            //myVideoView.setVideoURI(Uri.parse("https://hjyjrvmlsk.vcdn.com.vn/hls/elgfjdh/index.m3u8"));
//
//            myVideoView.start();
//
//        }else {
//            btnNum0.setVisibility(View.VISIBLE);
//            btnNum1.setVisibility(View.VISIBLE);
//            btnNum2.setVisibility(View.VISIBLE);
//            btnNum3.setVisibility(View.VISIBLE);
//            btnNum4.setVisibility(View.VISIBLE);
//            btnNum5.setVisibility(View.VISIBLE);
//            btnNum6.setVisibility(View.VISIBLE);
//            btnNum7.setVisibility(View.VISIBLE);
//            btnNum8.setVisibility(View.VISIBLE);
//            btnNum9.setVisibility(View.VISIBLE);
//            btnDelete.setVisibility(View.VISIBLE);
//            btnEnter.setVisibility(View.VISIBLE);
//            imgWifi.setVisibility(View.VISIBLE);
////            imgLogo.setVisibility(View.VISIBLE);
////            txtClock.setVisibility(View.VISIBLE);
//            txtPinCode.setVisibility(View.VISIBLE);
//            txtIPAddress.setVisibility(View.VISIBLE);
//
//            btnClear.setVisibility(View.VISIBLE);
//            btnShow.setVisibility(View.VISIBLE);
//            time.setVisibility(View.VISIBLE);
//            date.setVisibility(View.VISIBLE);
//            back.setVisibility(View.VISIBLE);
//
//            myVideoView.setVisibility(View.GONE);
//            myVideoView.stopPlayback();
//
//        }
//    }

    public void connectNetwork(String ssid, String pass){
        String txtUserName = ssid;
        String txtPassWord = pass;
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", txtUserName);
        wifiConfig.preSharedKey = String.format("\"%s\"", txtPassWord);


        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        //remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        Log.d("NPNIoTs","Connected!!!!");
    }



    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        imgWifi = findViewById(R.id.imgWifi);
//        imgLogo = findViewById(R.id.imgLogo);

//        txtClock = findViewById(R.id.txtClock);
        txtPinCode = findViewById(R.id.txtPinCode);
        txtIPAddress = findViewById(R.id.txtIPAddress);

        btnNum0 = findViewById(R.id.btnNum0);
        btnNum1 = findViewById(R.id.btnNum1);
        btnNum2 = findViewById(R.id.btnNum2);
        btnNum3 = findViewById(R.id.btnNum3);
        btnNum4 = findViewById(R.id.btnNum4);
        btnNum5 = findViewById(R.id.btnNum5);
        btnNum6 = findViewById(R.id.btnNum6);
        btnNum7 = findViewById(R.id.btnNum7);
        btnNum8 = findViewById(R.id.btnNum8);
        btnNum9 = findViewById(R.id.btnNum9);
        btnDelete = findViewById(R.id.btnDelete);

        btnEnter = findViewById(R.id.btnEnter);
        btnClear = findViewById(R.id.btnClear);
        btnShow = findViewById(R.id.btnShow);
        time = findViewById(R.id.time);
        date = findViewById(R.id.date);
        back = findViewById(R.id.back);
        initTimeAndWifi.post(initTimeAndWifiRunnable);
        mIdle.post(mIdleRunnable);

//        myVideoView = findViewById(R.id.myVideoView);
//
//        myVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer) {
//                myVideoView.start();
//            }
//        });
//
//        myVideoView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                visibleAllControls(true);
//                txtPinCode.setText("");
//            }
//        });
//
//        myVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mediaPlayer) {
//                mediaPlayer.setVolume(100f, 100f);
//            }
//        });


        mHomeViewModel = new NPNHomeViewModel();
        mHomeViewModel.attach(this, this);

        //override the focus cursor to hide the software keyboard
        txtPinCode.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        txtPinCode.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(isAllowProcess == true) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        String url = link + txtPinCode.getText();
                        mHomeViewModel.updateToServer(url);
                        txtPinCode.setText("");
                        door_state = DOOR_STATE.WAIT_DOOR_OPEN;
                        isAllowProcess = false;
                        return true;
                    }
                    return false;
                }
                else
                {
                    txtPinCode.setText("");
                    return true;
                }
            }
        });

        initGPIO();
        initUart();
        initSPI();
        setupBlinkyTimer();
        setupButtonClickEvent();
        txtPinCode.requestFocus();

        //create an Intent
        Intent checkData = new Intent();
        //set it up to check for tts data
        checkData.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        //start it so that it returns the result
        startActivityForResult(checkData, DATA_CHECKING);

//        visibleAllControls(false);


        Ultis.writeToInternalFile("test.txt", "abcdefgh");
        String read = Ultis.readFromInternalFile("test.txt");
        Log.d(TAG, "Data is: " + read);

        //connectNetwork("NPNLab2","rhm6i6fd");
    }

    private void setupButtonClickEvent() {
        btnNum0.setOnClickListener(this);
        btnNum1.setOnClickListener(this);
        btnNum2.setOnClickListener(this);
        btnNum3.setOnClickListener(this);
        btnNum4.setOnClickListener(this);
        btnNum5.setOnClickListener(this);
        btnNum6.setOnClickListener(this);
        btnNum7.setOnClickListener(this);
        btnNum8.setOnClickListener(this);
        btnNum9.setOnClickListener(this);
        btnEnter.setOnClickListener(this);
        
        btnShow.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        back.setOnClickListener(this);

        lastDown = System.currentTimeMillis();

        btnDelete.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastDown = System.currentTimeMillis();
                    int length = txtPinCode.getText().length();
                    if (length > 0) {
                        txtPinCode.getText().delete(length - 1, length);
                    }
                    isButtonDeletePress = true;
                    setupDeleteTimer();
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    lastDuration = System.currentTimeMillis() - lastDown;
                    Log.d("NPNIoTs", "Duration is: " + lastDuration);
                    isButtonDeletePress = false;
                    stopDeleteTimer();
                }

                return false;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.d("NPNIoTs", "Key code is: " + keyCode);

        if(keyCode == KeyEvent.KEYCODE_0)
        {
            onClick(btnNum0);
        }
        else if(keyCode == KeyEvent.KEYCODE_1)
        {
            onClick(btnNum1);
        }
        else if(keyCode == KeyEvent.KEYCODE_2)
        {
            onClick(btnNum2);
        }
        else if(keyCode == KeyEvent.KEYCODE_3)
        {
            onClick(btnNum3);
        }
        else if(keyCode == KeyEvent.KEYCODE_4)
        {
            onClick(btnNum4);
        }
        else if(keyCode == KeyEvent.KEYCODE_5)
        {
            onClick(btnNum5);
        }
        else if(keyCode == KeyEvent.KEYCODE_6)
        {
            onClick(btnNum6);
        }
        else if(keyCode == KeyEvent.KEYCODE_7)
        {
            onClick(btnNum7);
        }
        else if(keyCode == KeyEvent.KEYCODE_8)
        {
            onClick(btnNum8);
        }
        else if(keyCode == KeyEvent.KEYCODE_9)
        {
            onClick(btnNum9);
        }
        else if(keyCode == KeyEvent.KEYCODE_DEL)
        {
            int length = txtPinCode.getText().length();
            //remove the last character
            if (length > 0) {
                txtPinCode.getText().delete(length - 1, length);
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_ENTER)
        {
            onClick(btnEnter);
            txtPinCode.setText("");
        }
        txtPinCode.requestFocus();


        return true;
        //return super.onKeyDown(keyCode, event);
    }
    private String currentDoor = "";
    @Override
    public void onSuccessUpdateServer(String message) {
        Log.d(TAG, "Request server is successful " + message);
        //message = "1";
        if(message.equals("0")) {
            Log.d(TAG, "Wrong code");
            Toast.makeText(context, "INVALID CODE!", Toast.LENGTH_SHORT).show();
        }
        else {
            writeUartData(message);
            String speakWords = "Xin vui lòng đến ô số " + message;
//        niceTTS.speak(speakWords, TextToSpeech.QUEUE_FLUSH, null);
            door_state = DOOR_STATE.WAIT_DOOR_OPEN;
            door_timer = TIME_OUT_DOOR_OPEN;
            currentDoor = message;
            readStatus(currentDoor);
            Toast.makeText(context, "Please go to door" + message, Toast.LENGTH_SHORT).show();
        }
    }

    public void talkToMe(String sentence) {
        String speakWords = sentence;
        niceTTS.speak(speakWords, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onErrorUpdateServer(String message) {
        //txtConsole.setText("Request server is fail");
        Log.d(TAG, "Request server is fail");
        Toast.makeText(context, "CAN'T REACH THE SERVER!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {

        if(view == btnEnter)
        {
            Log.d(TAG, txtPinCode.getText().toString());
//            if(txtPinCode.getText().toString().length() < 8){
//                talkToMe("Mã nhập vào không đủ 8 kí tự");
//            }
//            else if(txtPinCode.getText().toString().length() > 8){
//                talkToMe("Mã nhập vào nhiều hơn 8 kí tự");
//            }
//            else{
                String url = link + txtPinCode.getText() ;
                mHomeViewModel.updateToServer(url);
                txtPinCode.setText("");
                door_state = DOOR_STATE.DOOR_CLOSED;
            //}
            return;
        }


        //remove the first character is the length is 8
        if (txtPinCode.getText().length() > 7) {
            txtPinCode.setText(txtPinCode.getText().subSequence(1, 8));
        }

        //add one more character
        if (view == btnNum0) {

            txtPinCode.setText(txtPinCode.getText() + "0");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum1) {
            txtPinCode.setText(txtPinCode.getText() + "1");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum2) {
            txtPinCode.setText(txtPinCode.getText() + "2");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum3) {
            txtPinCode.setText(txtPinCode.getText() + "3");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum4) {
            txtPinCode.setText(txtPinCode.getText() + "4");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum5) {
            txtPinCode.setText(txtPinCode.getText() + "5");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum6) {
            txtPinCode.setText(txtPinCode.getText() + "6");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum7) {
            txtPinCode.setText(txtPinCode.getText() + "7");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum8) {
            txtPinCode.setText(txtPinCode.getText() + "8");
            txtPinCode.setSelection(txtPinCode.getText().length());

        } else if (view == btnNum9) {
            txtPinCode.setText(txtPinCode.getText() + "9");
            txtPinCode.setSelection(txtPinCode.getText().length());

        }
        else if (view == btnShow){
            if(togglePassword) txtPinCode.setTransformationMethod(new PasswordTransformationMethod());
            else txtPinCode.setTransformationMethod(null);
            txtPinCode.setSelection(txtPinCode.length());
            togglePassword = !togglePassword;
        }
        else if (view == btnClear){
            txtPinCode.setText("");
        }
        else if (view == back){
            Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.example.xfoodz.home");
            if(LaunchIntent != null) {
                context.startActivity( LaunchIntent );
                finish();
                System.exit(0);
            }
        }
    }

    private void setupDeleteTimer()
    {
        mDeletePress = new Timer();
        TimerTask deleteTask = new TimerTask() {
            @Override
            public void run() {
                if(isButtonDeletePress == true)
                {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        int length = txtPinCode.getText().length();
                        //remove the last character
                        if (length > 0) {
                            txtPinCode.getText().delete(length - 1, length);
                        }
                        }
                    });

                }
            }
        };
        mDeletePress.schedule(deleteTask, 300, 300);
    }

    private void stopDeleteTimer()
    {
        mDeletePress.cancel();
    }

    private int counterWifi = 0;
    private void setupBlinkyTimer()
    {
        mBlinkyTimer = new Timer();
        TimerTask blinkyTask = new TimerTask() {
            @Override
            public void run() {
                counterWifi++;

                if(counterWifi >= 5) {
                    counterWifi = 0;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                            int numberOfLevels = 5;
                            WifiInfo wifiInfo = wifi.getConnectionInfo();
                            level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
                            if(wifi.isWifiEnabled() == false) level = -1;
                            switch(level){
                                case 0:
                                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_48dp);
                                    break;
                                case 1:
                                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_48dp);
                                    break;
                                case 2:
                                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_48dp);
                                    break;
                                case 3:
                                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_48dp);
                                    break;
                                case 4:
                                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_48dp);
                                    break;
                                case -1:
                                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_off_bar_black_48dp);
                                    break;
                            }
                            txtIPAddress = findViewById(R.id.txtIPAddress);
                            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                                int ipAddress = wifiInfo.getIpAddress();
                                String ipString = Formatter.formatIpAddress(ipAddress);
                                txtIPAddress.setText(ipString);
                            } else txtIPAddress.setText("No connection");

                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                            date.setText(format.format(new Date()));
                            format = new SimpleDateFormat("hh:mm");
                            time.setText(format.format(new Date()));

                        }
                    });

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mLedGpio.setValue(!mLedGpio.getValue());

                            } catch (Throwable t) {
                                Log.d(TAG, "Error in Blinky LED " + t.getMessage());
                            }
                        }
                    });
                }
                //readStatus("1");
                switch (door_state){
                    case NONE:
                        if(door_timer > 0)
                        {
                            door_timer--;
                            if(door_timer == 0){
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
//                                        visibleAllControls(false);
                                        isAllowProcess = true;
                                    }
                                });

                            }
                        }
                        break;
                    case WAIT_DOOR_OPEN:
                        door_timer--;
                        if(door_status.equals(DOOR_OPEN) == true){
                            door_state = DOOR_STATE.DOOR_OPENED;
                        }else {
                            readStatus(currentDoor);
                        }
                        if(door_timer == 0)
                        {
                            Log.d("NPNIoTs", "Open again the door: " + currentDoor);
                            writeUartData(currentDoor);
                            door_timer = 3;
                        }
                        break;
                    case DOOR_OPENED:
                        door_timer = 10;
                        readStatus(currentDoor);
                        door_state = DOOR_STATE.WAIT_DOOR_CLOSE;
                        break;
                    case WAIT_DOOR_CLOSE:
                        door_timer--;
                        readStatus(currentDoor);
                        if(door_status.equals(DOOR_CLOSE)){
                            door_state = DOOR_STATE.DOOR_CLOSED;
                        }
                        if(door_timer <= 0)
                        {
                            talkToMe("Xin vui lòng đóng cửa số " + currentDoor);
                            door_timer = 5;
                        }
                        break;
                    case DOOR_CLOSED:
                        talkToMe("Xin cám ơn quý khách");
                        door_state = DOOR_STATE.NONE;
                        door_timer = 5;
                        break;
                    default:
                        break;
                }
            }
        };
        mBlinkyTimer.schedule(blinkyTask, 5000, 1000);
    }

    public void writeUartData(String message) {
        try {
            byte[] buffer = {'W',' ',' '};
            buffer[2] =  (byte)(Integer.parseInt(message));
            int count = mUartDevice.write(buffer, buffer.length);
            Log.d(TAG, "Send: "   + buffer[2]);
        }catch (IOException e)
        {
            Log.d(TAG, "Error on UART");
        }
    }

    public void readStatus(String ID)
    {
        try {
            byte[] buffer = {'R',' ',' '};
            buffer[2] =  (byte)(Integer.parseInt(ID));
            int count = mUartDevice.write(buffer, buffer.length);
            //Log.d(TAG, "Wrote " + count + " bytes to peripheral  "  + buffer[2]);
        }catch (IOException e)
        {
            Log.d(TAG, "Error on UART");
        }
    }


    private void initSPI()
    {
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> deviceList = manager.getSpiBusList();
        if(deviceList.isEmpty())
        {
            Log.d(TAG,"No SPI bus is not available");
        }
        else
        {
            Log.d(TAG,"SPI bus available: " + deviceList);
            //check if SPI_DEVICE_NAME is in list
            try {
                mSPIDevice = manager.openSpiDevice(SPI_DEVICE_NAME);

                mSPIDevice.setMode(SpiDevice.MODE1);
                mSPIDevice.setFrequency(1000000);
                mSPIDevice.setBitsPerWord(8);
                mSPIDevice.setBitJustification(SpiDevice.BIT_JUSTIFICATION_MSB_FIRST);


                Log.d(TAG,"SPI: OK... ");


            }catch (IOException e)
            {
                Log.d(TAG,"Open SPI bus fail... ");
            }
        }
    }



    private void sendCommand(SpiDevice device, byte[] buffer) throws  IOException{


        mCS.setValue(false);
        for(int i = 0; i < 100; i++) {}

        //send data to slave
        device.write(buffer, buffer.length);


        //read the response
        byte[] response = new byte[2];
        device.read(response, response.length);


        for(int i = 0; i< 2; i++) {

            Log.d(TAG, "Response byte " + Integer.toString(i) + " is: " + response[i]);
        }
        mCS.setValue(true);
        for(int i = 0; i < 100; i++){}

        double value = (double)(response[0] * 256 + response[1]);
        double adc = value * 6.144/32768;


    }

    private void initGPIO()
    {
        PeripheralManager manager = PeripheralManager.getInstance();
        try {
            mLedGpio = manager.openGpio(LED_PIN_NAME);
            // Step 2. Configure as an output.
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            mCS = manager.openGpio(CS_PIN_NAME);
            mCS.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);


        } catch (IOException e) {
            Log.d(TAG, "Error on PeripheralIO API");
        }
    }

    private void initUart()
    {
        try {
            openUart("UART0", BAUD_RATE);
        }catch (IOException e) {
            Log.d(TAG, "Error on UART API");
        }
    }
    /**
     * Callback invoked when UART receives new incoming data.
     */
    private String door_status = "";
    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
           //read data from Rx buffer
            try {
                byte[] buffer = new byte[CHUNK_SIZE];
                int noBytes = -1;
                while ((noBytes = mUartDevice.read(buffer, buffer.length)) > 0) {
                    Log.d(TAG,"Number of bytes: " + Integer.toString(noBytes));

                    String str = new String(buffer,0,noBytes, "UTF-8");

                    Log.d(TAG,"Buffer is: " + str);
                    door_status = str;

                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to transfer data over UART", e);
            }
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    private void openUart(String name, int baudRate) throws IOException {
        mUartDevice = PeripheralManager.getInstance().openUartDevice(name);
        // Configure the UART
        mUartDevice.setBaudrate(baudRate);
        mUartDevice.setDataSize(DATA_BITS);
        mUartDevice.setParity(UartDevice.PARITY_NONE);
        mUartDevice.setStopBits(STOP_BITS);
        mUartDevice.registerUartDeviceCallback(mCallback);
    }

    private void closeUart() throws IOException {
        if (mUartDevice != null) {
            mUartDevice.unregisterUartDeviceCallback(mCallback);
            try {
                mUartDevice.close();
            } finally {
                mUartDevice = null;
            }
        }
    }

    private void closeSPI() throws IOException {
        if(mSPIDevice != null)
        {
           try {
               mSPIDevice.close();
           }finally {
               mSPIDevice = null;
           }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Attempt to close the UART device
        try {
            closeUart();
            mUartDevice.unregisterUartDeviceCallback(mCallback);
            closeSPI();
        } catch (IOException e) {
            Log.e(TAG, "Error closing UART device:", e);
        }
    }

    private Runnable initTimeAndWifiRunnable = new Runnable() {
        @Override
        public void run() {
            wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int numberOfLevels = 5;
            WifiInfo wifiInfo = wifi.getConnectionInfo();
            level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
            if(wifi.isWifiEnabled() == false) level = -1;
            if(isEthernetConnected()) level = -2;
            switch(level){
                case 0:
                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_48dp);
                    break;
                case 1:
                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_48dp);
                    break;
                case 2:
                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_48dp);
                    break;
                case 3:
                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_48dp);
                    break;
                case 4:
                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_48dp);
                    break;
                case -1:
                    imgWifi.setImageResource(R.drawable.ic_signal_wifi_off_bar_black_48dp);
                    break;
                case -2:
                    imgWifi.setImageResource(R.drawable.ic_computer_black_24dp);
                    break;
            }
            txtIPAddress = findViewById(R.id.txtIPAddress);
            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                int ipAddress = wifiInfo.getIpAddress();
                String ipString = Formatter.formatIpAddress(ipAddress);
                txtIPAddress.setText(ipString);
            } else txtIPAddress.setText("No connection");

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            date.setText(format.format(new Date()));
            format = new SimpleDateFormat("hh:mm");
            time.setText(format.format(new Date()));
        }
    };
    private Runnable mIdleRunnable = new Runnable() {
        @Override
        public void run() {
            if(idleCount == 30) {
                idle = true;
                idleCount = 0;
                Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.example.xfoodz.home");
                if(LaunchIntent != null) {
                    context.startActivity( LaunchIntent );
                    finish();
                    System.exit(0);
                }
            }
            else {
                if(codeLength == txtPinCode.length()) idleCount++;
                else codeLength = txtPinCode.length();
            }
            if(!idle) mIdle.postDelayed(mIdleRunnable, 1000);
        }
    };

    private Boolean isNetworkAvailable() {
        ConnectivityManager cm
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public Boolean isEthernetConnected(){
        if(isNetworkAvailable()){
            ConnectivityManager cm
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            return (cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_ETHERNET);
        }
        return false;
    }
}

