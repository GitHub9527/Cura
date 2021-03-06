/*
 Copyright© 2010, 2011 Ahmad Balaa, Oday Maleh

 This file is part of Cura.

	Cura is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cura is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Cura.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cura;

/*
 * Description: This is the Login Screen, the very first activity that the user is dropped into. 
 * This is where they can begin added new user accounts as well as modify or delete them. 
 * They can also access Cura's settings from this activity's menu options.  
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;

import com.cura.validation.regexValidator;

public class AccountsListActivity extends ListActivity {

	private final String connected = "cura.connected";
	private final String notConnected = "cura.not.connected";
	private final String deleteDB = "database.delete";
	private final int ADD_USER = 1;
	private final int SETTINGS = 2;
	private final int ABOUT = 3;
	private final int MODIFY_USER = 4;
	private final int DELETE_USER = 5;
	DbHelper dbHelper;
	SQLiteDatabase db;
	User user[];
	User userTemp;
	CustomArrayAdapter array;
	Intent goToMainActivity;
	BroadcastReceiver br, databaseBR;
	AlertDialog.Builder loader;
	private Vibrator vibrator;
	private SharedPreferences prefs;
	private static final int DIALOG_YES_NO_LONG_MESSAGE = 99;
	private static final int WAIT = 100;
	private regexValidator rv;
	private String loader_message = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.loginscreenname);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		rv = new regexValidator();
		user = getUser();
		// create the listView

		if (user.length == 1
				&& user[0].getUsername().equalsIgnoreCase("username")
				&& user[0].getDomain().equalsIgnoreCase("domain")) {
			showDialog(DIALOG_YES_NO_LONG_MESSAGE);
		}

		array = new CustomArrayAdapter(this, user);
		setListAdapter(array);

		// enable context menu
		registerForContextMenu(getListView());

		databaseBR = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// this is used to refresh
				// that specific screen in order to instantly see that Cura's
				// database has been wiped due to an emergency SMS having been
				// sent
				// to the phone
				user = getUser();
				array = new CustomArrayAdapter(AccountsListActivity.this, user);
				setListAdapter(array);
				Log.d("onResume", "onResume");
			}
		};

		IntentFilter databaseIntentFilter = new IntentFilter();
		databaseIntentFilter.addAction(deleteDB);
		registerReceiver(databaseBR, databaseIntentFilter);

		// initializing the vibrator object
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	}

	// new function that fetches users info from database, used in "onCreate()"
	// and to refresh activity
	public User[] getUser() {
		dbHelper = new DbHelper(this);
		db = dbHelper.getReadableDatabase();

		// select all users.
		Cursor c = db.rawQuery("select * from user", null);
		// create an array of users
		if (c.getCount() == 0) {
			user = new User[1];
			user[0] = new User("username", "domain", 22);
		} else {
			user = new User[c.getCount()];

			int counter = 0;

			// get info and store them into objects
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						String username = c.getString(c
								.getColumnIndex("username"));
						String domain = c.getString(c.getColumnIndex("domain"));
						int port = Integer.parseInt(c.getString(c
								.getColumnIndex("port")));
						// fetch the user's data.
						user[counter] = new User(username, domain, port);
						counter++;
					} while (c.moveToNext());
				}
			}
		}
		// close database
		c.close();
		db.close();
		dbHelper.close();

		return user;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_YES_NO_LONG_MESSAGE:
			return new AlertDialog.Builder(AccountsListActivity.this)
					// this is the screen that shows up with the user installs
					// Cura
					// for the very first time
					// .setIconAttribute(android.R.attr.alertDialogIcon)
					.setTitle(R.string.firstTimeUseMessageTitle)
					.setMessage(R.string.firstTimeUseMessage)
					.setPositiveButton(R.string.firstTimeUseOKButton,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked OK so do some stuff */
								}
							})
					.setNegativeButton(R.string.firstTimeUseCancelButton,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked Cancel so do some stuff */
								}
							}).create();
		case WAIT:
			loader = new AlertDialog.Builder(this);
			loader.setMessage(loader_message);
			loader.setCancelable(false);
			AlertDialog ad = loader.create();
			return ad;
		}
		return null;
	}

	// THE SECOND MENU
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// add to buttons to context menu "Modify user Info", "Delete User"
		menu.add(0, MODIFY_USER, 0, R.string.ModifyUserInfo).setIcon(
				R.drawable.ic_menu_edit);
		menu.add(0, DELETE_USER, 0, R.string.DeleteUser).setIcon(
				R.drawable.ic_menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		int userIDint = (int) info.id;
		final String usernameCode = user[userIDint].getUsername();
		final String domainCode = user[userIDint].getDomain();

		dbHelper = new DbHelper(AccountsListActivity.this);
		db = dbHelper.getWritableDatabase();

		switch (item.getItemId()) {
		// modify button is pressed
		case MODIFY_USER:
			final Dialog myDialog;
			myDialog = new Dialog(AccountsListActivity.this);
			myDialog.setContentView(R.layout.adduserscreen);
			myDialog.setTitle(R.string.DialogTitle);
			myDialog.setCancelable(true);
			myDialog.setCanceledOnTouchOutside(true);

			Button modifyUserInfo = (Button) myDialog
					.findViewById(R.id.button1);
			modifyUserInfo.setText(R.string.ModifyUserInfo);
			Button cancelButton = (Button) myDialog.findViewById(R.id.button2);

			final EditText usernameInput = (EditText) myDialog
					.findViewById(R.id.usernameTextField);
			final EditText domainInput = (EditText) myDialog
					.findViewById(R.id.domainTextField);
			final EditText portInput = (EditText) myDialog
					.findViewById(R.id.portTextField);
			// display the initial info to be replaced by the user
			usernameInput.setText(user[userIDint].getUsername());
			domainInput.setText(user[userIDint].getDomain());
			portInput.setText("" + user[userIDint].getPort());

			modifyUserInfo.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// update user info
					String usern = usernameInput.getText().toString();
					String domain = domainInput.getText().toString();
					int port = Integer.parseInt(portInput.getText().toString());

					ContentValues values = new ContentValues();

					values.put(dbHelper.C_USERNAME, usern);
					values.put(dbHelper.C_DOMAIN, domain);
					values.put(dbHelper.C_PORT, port);

					String where = "username = ? AND domain = ?";
					String[] whereArgs = { usernameCode, domainCode };
					try {
						db.update(DbHelper.userTableName, values, where,
								whereArgs);
					} catch (Exception e) {
						Log.d("SQL", e.toString());
					}

					// close database
					db.close();
					dbHelper.close();

					// CHANGED : refresh list view
					user = getUser();
					array = new CustomArrayAdapter(AccountsListActivity.this,
							user);
					setListAdapter(array);

					myDialog.cancel();
				}
			});

			cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// close dialog box
					myDialog.cancel();
				}
			});
			myDialog.show();
			return true;

		case DELETE_USER:
			// Delete user
			try {
				String table_name = "user";
				String where = "username = ? AND domain = ?";
				String[] whereArgs = { usernameCode, domainCode };
				// prepare the query.
				db.delete(table_name, where, whereArgs);
				// execute it.

			} catch (Exception e) {
				Log.d("SQL", e.toString());
				// so that we can know where to follow the errors (if any).
			}
			// close database
			db.close();
			dbHelper.close();

			// CHANGED : refresh list view
			user = getUser();
			array = new CustomArrayAdapter(AccountsListActivity.this, user);
			setListAdapter(array);

			return true;
		}
		return super.onContextItemSelected(item);
	}

	public boolean isFound(String username, String domain) {
		String userValue = "";
		String dom = "";
		// if the same username and domain are found and have already been
		// added, return the result here so that the device can vibrate and
		// display an error to the user
		for (int i = 0; i < user.length; i++) {
			userValue = user[i].getUsername();
			dom = user[i].getDomain();
			if (userValue.compareTo(username) == 0
					&& dom.compareTo(domain) == 0)
				return true;
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(databaseBR);
	}

}