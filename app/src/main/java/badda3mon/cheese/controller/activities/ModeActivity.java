package badda3mon.cheese.controller.activities;

import android.content.Intent;
import android.view.View;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import badda3mon.cheese.controller.R;
import badda3mon.cheese.controller.additional.PersistenceStorage;

public class ModeActivity extends AppCompatActivity {
	private static final String TAG = "ModeActivity";

	public static final int FIRST_PANEL_MODE = 1;
	public static final int SECOND_PANEL_MODE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mode);

		PersistenceStorage.init(this);
		int savedMode = PersistenceStorage.getIntProperty("mode");

		if (savedMode != -1) {
			RelativeLayout layout = findViewById(R.id.mode_base_layout);
			layout.setVisibility(View.GONE);

			startWorkActivity();
		}
	}

	public void onSelectPanelButtonsClick(View view){
		int mode = (view.getId() == R.id.first_panel_mode_button) ?  FIRST_PANEL_MODE : SECOND_PANEL_MODE;

		PersistenceStorage.addIntProperty("mode", mode);

		startWorkActivity();
	}

	private void startWorkActivity(){
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

		startActivity(intent);
	}
}