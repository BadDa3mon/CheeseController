package badda3mon.cheese.controller.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import badda3mon.cheese.controller.R;
import badda3mon.cheese.controller.additional.PersistenceStorage;
import badda3mon.cheese.controller.additional.Topics;
import badda3mon.cheese.controller.additional.Utils;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private static final String SERVER_IP = "tcp://46.160.179.227:8883";

	private int mMode;

	private Handler mHandler;

	private TextView mNowDateTextView;
	private TextView mNowTimeTextView;

	private TextView mCurrentCookingTextView;
	private int mCurrentCookingNumber;
	private int mPreviousCookingNumber;

	private EditText mDateCookingEditText;

	private TextView mCheeseCountFP; // first panel

	private TextView mCheeseOnlineCountSP; // second panel
	private TextView mCheeseInCookingCountSP; // second panel

	private BroadcastReceiver mTimeTickReceiver;

	private MqttAndroidClient mMqttClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PersistenceStorage.init(this);

		mMode = PersistenceStorage.getIntProperty("mode");

		mHandler = new Handler(Looper.getMainLooper());

		mNowDateTextView = findViewById(R.id.date_text_view);
		mNowTimeTextView = findViewById(R.id.time_text_view);

		mCurrentCookingTextView = findViewById(R.id.current_cooking_count_text_view);

		mCheeseCountFP = findViewById(R.id.cheese_global_count_text_view);
		mCheeseOnlineCountSP = findViewById(R.id.cheese_online_count_text_view);
		mCheeseInCookingCountSP = findViewById(R.id.cheese_in_cooking_count_text_view);

		mCurrentCookingNumber = 0;
		mPreviousCookingNumber = 0;

		mTimeTickReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateDateTime();
			}
		};

		mDateCookingEditText = findViewById(R.id.date_cooking_edit_text);
		mDateCookingEditText.setText(Utils.getCurrentDate("YYYY-MM-dd"));

		initMode();
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

		if (mMode == ModeActivity.SECOND_PANEL_MODE){
			updateCheeseInCooking();
		}
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

	private void initMode(){
//		if (BuildConfig.DEBUG) return;

		LinearLayout cheeseGlobalLayout = findViewById(R.id.cheese_global_layout);
		LinearLayout cheeseOnlineLayout = findViewById(R.id.cheese_online_layout);
		LinearLayout cheeseInCookingLayout = findViewById(R.id.cheese_in_cooking_layout);

		if (mMode == ModeActivity.FIRST_PANEL_MODE){
			cheeseGlobalLayout.setVisibility(View.VISIBLE);
			cheeseOnlineLayout.setVisibility(View.GONE);
			cheeseInCookingLayout.setVisibility(View.GONE);
		} else {
			cheeseGlobalLayout.setVisibility(View.GONE);
			cheeseOnlineLayout.setVisibility(View.VISIBLE);
			cheeseInCookingLayout.setVisibility(View.VISIBLE);
		}

		Log.i(TAG,"Mode initialized!");
	}

	private boolean isCookingStarted = false;
	public void onNewCookingButtonClick(View view){
		if (mCurrentCookingNumber == (mPreviousCookingNumber + 1) || !isCookingStarted){
			String currentDate = Utils.getCurrentDate("YYYY-MM-dd");
			String nextDate = Utils.getNextDate("YYYY-MM-dd");

			String date = mDateCookingEditText.getText().toString();

			if (date.equals(currentDate) || date.equals(nextDate)){
				String msg = mMode + "," + date + "," + mCurrentCookingNumber;

				sendMessageByMqtt(msg, Topics.BASE_CHANNEL);

				mPreviousCookingNumber = mCurrentCookingNumber;

				if (!isCookingStarted) isCookingStarted = true;
			} else {
				Log.e(TAG,"Date incorrect!\n" +
						"Current: " + currentDate + "\n" +
						"Next: " + nextDate + "\n" +
						"Inputted: " + date);

				Toast.makeText(this, "Дата варки может быть только сегодняшняя или завтрашняя, проверьте введённые данные и повторите попытку!", Toast.LENGTH_LONG).show();
			}
		} else {
			Log.e(TAG,"Current number has bad number");

			String msg = "Предыдущий номер варки был \"" + mPreviousCookingNumber + "\", текущий должен быть \"" + (mPreviousCookingNumber + 1) + "\"";
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		}
	}

	public void onChangeCookingButtonsClick(View view){
		boolean isIncrement = (view.getId() == R.id.current_cooking_new_value_button);

		String text;
		if (isIncrement){
			mCurrentCookingNumber++;
		} else {
			if (mCurrentCookingNumber > 0){
				mCurrentCookingNumber--;
			} else {
				Toast.makeText(this, "Значение не может быть меньше нуля!", Toast.LENGTH_SHORT).show();

				mCurrentCookingNumber = 0;
			}
		}
		text = String.valueOf(mCurrentCookingNumber);

		mCurrentCookingTextView.setText(text);
	}

	private void sendMessageByMqtt(String msg, String topic){
		byte[] encodedMessage = msg.getBytes(StandardCharsets.UTF_8);

		MqttMessage message = new MqttMessage(encodedMessage);
		message.setRetained(true);

		if (mMqttClient != null && mMqttClient.isConnected()){
			try {
				mMqttClient.publish(topic, message);

				Log.i(TAG,"Message \"" + msg + "\" send to \"" + topic + "\" topic");
			} catch (MqttException e) {
				Log.e(TAG,"sendMessageByMqtt error: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			Log.e(TAG,"mMqttClient == null? -> " + (mMqttClient == null) + ", isConnected? -> false");

			Toast.makeText(this, "Вы не подключены к серверу!", Toast.LENGTH_SHORT).show();
		}
	}

	private void subscribeToTopics(){
		if (mMqttClient != null && mMqttClient.isConnected()){
			try {
				mMqttClient.subscribe(Topics.AMOUNT_CHANNEL,1);
				mMqttClient.subscribe(String.format(Locale.getDefault(), Topics.CHEESE_IN_DB_COUNT, mMode),1);

				if (mMode == ModeActivity.SECOND_PANEL_MODE){
					mMqttClient.subscribe(Topics.CHEESE_IN_COOKING_COUNT_SET,1);
				}
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
					runOnUiThread(() -> updateConnecting(2));
					subscribeToTopics();
				}

				@Override
				public void connectionLost(Throwable cause) {
					runOnUiThread(() -> updateConnecting(0));
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					handleMessage(topic, message.toString());
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
//					Log.d(TAG,"Delivery completed: " + token.toString());
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

	private void handleMessage(String topic, String msg){
		String volTopic = String.format(Locale.getDefault(), Topics.CHEESE_IN_DB_COUNT, mMode);

		Log.d(TAG,"Get answer from \"" + topic + "\": " + msg);

		if (topic.equals(volTopic)){
			int count = Integer.parseInt(msg);

			if (mMode == ModeActivity.FIRST_PANEL_MODE) mCheeseCountFP.setText(String.valueOf(count));
			else mCheeseOnlineCountSP.setText(String.valueOf(count));
		} else if (topic.equals(Topics.CHEESE_IN_COOKING_COUNT_SET)){
			int count = Integer.parseInt(msg);

			if (mMode == ModeActivity.SECOND_PANEL_MODE) mCheeseInCookingCountSP.setText(String.valueOf(count));
			else Log.e(TAG,"Current mode is first, no need to handle!");
		}
	}

	private void updateCheeseInCooking(){
		if (mMqttClient != null && mMqttClient.isConnected()){
			mHandler.postDelayed(() -> {
				sendMessageByMqtt(String.valueOf(mCurrentCookingNumber), Topics.CHEESE_IN_COOKING_COUNT_GET);

				updateCheeseInCooking();
			},5000);
		} else {
			Log.e(TAG,"mMqttClient == null? -> " + (mMqttClient == null) + ", isConnected? -> false");
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
			connectString = getResources().getString(R.string.connected);
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