package com.pixplicity.example.facebookthief;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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

		TextView text = (TextView) row.findViewById(R.id.text);
		ImageView icon = (ImageView) row.findViewById(R.id.icon);
		String line = getItem(position).line;
		String interest = getItem(position).interest;
		SpannableString span = new SpannableString(line);
		if (interest == null) {
			icon.setVisibility(View.INVISIBLE);
		} else {
			int start = line.indexOf(interest);
			if (start >= 0) {
				span.setSpan(new StyleSpan(Typeface.BOLD),
						start, start + interest.length(), 0);
			}
		}
		text.setText(span);

		return row;
	}

}
