package test.notification;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.LinearLayout.LayoutParams;

nihaohao

12345
23456
public class Setting extends PreferenceActivity {
	public static final String PREFS_NAME = "prefs";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		if (prefs.getString("blacklist", null) == null) {
			prefs.edit().putString("blacklist",
				";Wildcard supported.\n;eg. 13712345678:*ERROR*\n;Split by \\n").commit();
		}
		setPreferenceScreen(createPreferenceHierarchy());
	}

	private PreferenceScreen createPreferenceHierarchy() {
		// Root
		PreferenceManager pm = getPreferenceManager();
		pm.setSharedPreferencesName(PREFS_NAME);
		pm.setSharedPreferencesMode(MODE_PRIVATE);
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
			this);

		//
		// rules
		//
		PreferenceCategory rules = new PreferenceCategory(this);
		rules.setTitle("Rules");
		root.addPreference(rules);

		EditTextPreference blacklist = new EditTextPreference(this) {
			@Override
			protected void onAddEditTextToDialogView(View dialogView,
					EditText editText) {
				super.onAddEditTextToDialogView(dialogView, editText);
				int height = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 80
						: 120;
				editText.setLayoutParams(new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT, height));
				editText.setGravity(Gravity.LEFT | Gravity.TOP);
			}
		};
		blacklist.setKey("blacklist");
		blacklist.setTitle("Blacklist");
		blacklist.setDialogTitle("Blacklist");
		rules.addPreference(blacklist);

		Preference startTime = new Preference(this);
		startTime.setTitle("Start Time");
		startTime.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				final SharedPreferences prefs = getSharedPreferences(
					PREFS_NAME, MODE_PRIVATE);
				TimePickerDialog dlg = new TimePickerDialog(Setting.this,
						new TimePickerDialog.OnTimeSetListener() {
							@Override
							public void onTimeSet(TimePicker view,
									int hourOfDay, int minute) {
								prefs.edit().putInt("startHour", hourOfDay).putInt(
									"startMinute", minute).commit();
							}
						}, prefs.getInt("startHour", 0), prefs.getInt(
							"startMinute", 0), true);
				dlg.show();
				return true;
			}
		});
		rules.addPreference(startTime);

		Preference endTime = new Preference(this);
		endTime.setTitle("End Time");
		endTime.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				final SharedPreferences prefs = getSharedPreferences(
					PREFS_NAME, MODE_PRIVATE);
				TimePickerDialog dlg = new TimePickerDialog(Setting.this,
						new TimePickerDialog.OnTimeSetListener() {
							@Override
							public void onTimeSet(TimePicker view,
									int hourOfDay, int minute) {
								prefs.edit().putInt("endHour", hourOfDay).putInt(
									"endMinute", minute).commit();
							}
						}, prefs.getInt("endHour", 0), prefs.getInt(
							"endMinute", 0), true);
				dlg.show();
				return true;
			}
		});
		rules.addPreference(endTime);

		//
		// notification
		//
		PreferenceCategory notification = new PreferenceCategory(this);
		notification.setTitle("Notification");
		root.addPreference(notification);

		RingtonePreference rington = new RingtonePreference(this);
		rington.setKey("rington");
		rington.setTitle("Rington");
		rington.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
		notification.addPreference(rington);

		CheckBoxPreference vibrate = new CheckBoxPreference(this);
		vibrate.setKey("vibrate");
		vibrate.setTitle("Vibrate");
		notification.addPreference(vibrate);

		Preference test = new Preference(this);
		test.setTitle("Test");
		test.setIntent(new Intent(this, Notify.class));
		notification.addPreference(test);

		return root;
	}
}
