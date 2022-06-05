package badda3mon.cheese.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import badda3mon.cheese.controller.additional.PersistenceStorage;
import badda3mon.cheese.controller.additional.Utils;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.*;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private static final String SERVER_IP = "tcp://46.160.179.227:8883";

	private int mMode;

	private TextView mNowDateTextView;
	private TextView mNowTimeTextView;

	private BroadcastReceiver mTimeTickReceiver;

	private MqttAndroidClient mMqttClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PersistenceStorage.init(this);

		mMode = PersistenceStorage.getIntProperty("mode");

		mNowDateTextView = findViewById(R.id.date_text_view);
		mNowTimeTextView = findViewById(R.id.time_text_view);

		mTimeTickReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateDateTime();
			}
		};
	}

	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
		registerReceiver(mTimeTickReceiver, filter);

		updateDateTime();

		if (mMqttClient == null || !mMqttClient.isConnected()) {
			Log.d(TAG,"MQTT not connected, then connect!");

			connectToMqttServer();
		} else Log.e(TAG,"MQTT already connected!");
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mTimeTickReceiver);

		if (mMqttClient != null && mMqttClient.isConnected()) {
			Log.d(TAG,"MQTT connected, then disconnect!");

			disconnectFromMqttServer();
		} else Log.e(TAG,"MQTT already disconnected!");
	}

	private void subscribeToTopics(){
		if (mMqttClient != null && mMqttClient.isConnected()){
			try {
				IMqttToken token = mMqttClient.subscribe("cheese_test/1/amount",1);
				token.setActionCallback(new IMqttActionListener() {
					@Override
					public void onSuccess(IMqttToken asyncActionToken) {
						Log.d(TAG,"Subscribe success!");
					}

					@Override
					public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
						Log.e(TAG,"Failed to subscribe: " + exception.toString());
					}
				});
			} catch (MqttException e) {
				Log.e(TAG,"subscribeToTopics error: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			Log.e(TAG,"mMqttClient == null? -> " + (mMqttClient == null) + ", isConnected? -> false");
		}
	}

	private void connectToMqttServer(){
		updateConnecting(1);

		String clientId = MqttClient.generateClientId();
		mMqttClient = new MqttAndroidClient(getApplicationContext(), SERVER_IP, clientId);

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);

		try {
			mMqttClient.connect(options);
			mMqttClient.setCallback(new MqttCallbackExtended() {
				@Override
				public void connectComplete(boolean reconnect, String serverURI) {
					Log.d(TAG,"Connection completed: " + serverURI + ", reconnect? -> " + reconnect);

					runOnUiThread(() -> updateConnecting(2));
					subscribeToTopics();
				}

				@Override
				public void connectionLost(Throwable cause) {
					Log.d(TAG,"Lost connecting: " + cause.toString());

					runOnUiThread(() -> updateConnecting(0));
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					Log.d(TAG,"Message: \"" + message + "\" arrived to topic \"" + topic + "\"");
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					Log.d(TAG,"Delivery completed: " + token.toString());
				}
			});
		} catch (MqttException e) {
			Log.e(TAG,"connectToMqttServer: " + e.getMessage());
			e.printStackTrace();

			updateConnecting(0);
		}
	}

	private void disconnectFromMqttServer(){
		try {
			mMqttClient.disconnect();
			mMqttClient = null;
		} catch (MqttException e) {
			Log.e(TAG,"disconnectFromMqttServer: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void updateConnecting(int type){
		// 0 - not connected, 1 - connecting, 2 - connected
		String connectString;
		int color;

		if (type == 0){
			connectString = getResources().getString(R.string.not_connected);
			color = getResources().getColor(R.color.red);
		} else if (type == 1){
			connectString = getResources().getString(R.string.connecting);
			color = getResources().getColor(R.color.yellow);
		} else {
			connectString = getResources().getString(R.string.connecting);
			color = getResources().getColor(R.color.green);
		}

		TextView connStatusTextView = findViewById(R.id.connecting_status_text_view);
		connStatusTextView.setText(connectString);
		connStatusTextView.setTextColor(color);
	}

	private void updateDateTime(){
		String date = Utils.getCurrentDate("YYYY-MM-dd");
		String time = Utils.getCurrentDate("HH:mm");

		mNowDateTextView.setText(date);
		mNowTimeTextView.setText(time);
	}
}