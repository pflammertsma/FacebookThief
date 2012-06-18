package com.pixplicity.example.facebooktrapper;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TrapperListAdapter extends ArrayAdapter<TrapperResult> {

	public TrapperListAdapter(Context context) {
		super(context, R.layout.list_item);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row;

		if (null == convertView) {
			row = View.inflate(parent.getContext(), R.layout.list_item, null);
		} else {
			row = convertView;
		}

		TextView tv = (TextView) row.findViewById(R.id.text1);
		String line = getItem(position).line;
		String interest = getItem(position).interest;
		SpannableString span = new SpannableString(line);
		int start = line.indexOf(interest);
		if (start >= 0) {
			span.setSpan(new StyleSpan(Typeface.BOLD),
					start, start + interest.length(), 0);
		}
		tv.setText(span);

		return row;
	}

}
