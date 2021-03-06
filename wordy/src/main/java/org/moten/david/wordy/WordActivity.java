package org.moten.david.wordy;

import java.util.Arrays;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class WordActivity extends Activity {

	private static String TAG = "wordy";
	private WordAdapter dbAdapter;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            If the activity is being re-initialized after previously being
	 *            shut down then this Bundle contains the data it most recently
	 *            supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it
	 *            is null.</b>
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.main);
		dbAdapter = new WordAdapter(this);
		dbAdapter.open();

		// Cursor c = dbAdapter.getAnagrams("era");
		// startManagingCursor(c);
		// String[] columns = new String[] { "WORD" };
		// int[] to = new int[] { R.id.list_item };
		//
		// SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
		// R.layout.main, c, columns, to);
		// list.setAdapter(adapter);
		EditText text = (EditText) this.findViewById(R.id.entry);
		text.addTextChangedListener(createTextWatcher(text));
	}

	private TextWatcher createTextWatcher(final EditText text) {
		return new TextWatcher() {

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			public void afterTextChanged(Editable s) {
				if (dbAdapter.isValidWord(s.toString()))
					text.setBackgroundColor(Color.GREEN);
				else
					text.setBackgroundColor(Color.RED);
				String[] words = dbAdapter.getAnagramsExtra(s.toString())
						.toArray(new String[0]);
				Arrays.sort(words);
				for (int i = 0; i < words.length; i++)
					words[i] = words[i].toLowerCase();
				ListView lv = (ListView) WordActivity.this
						.findViewById(R.id.list);
				lv.setAdapter(new ArrayAdapter<String>(WordActivity.this,
						android.R.layout.simple_list_item_1, words));

			}
		};
	}
}
