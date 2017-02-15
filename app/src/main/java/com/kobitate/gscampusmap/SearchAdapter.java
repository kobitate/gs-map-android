package com.kobitate.gscampusmap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.vision.text.Text;

import java.util.List;

/**
 * Created by kobi on 2/15/17.
 */

public class SearchAdapter extends ArrayAdapter<String[]> {

	public SearchAdapter(Context context, int resource) {
		super(context, resource);
	}

	public SearchAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public SearchAdapter(Context context, int resource, String[][] objects) {
		super(context, resource, objects);
	}

	public SearchAdapter(Context context, int resource, int textViewResourceId, String[][] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public SearchAdapter(Context context, int resource, List<String[]> objects) {
		super(context, resource, objects);
	}

	public SearchAdapter(Context context, int resource, int textViewResourceId, List<String[]> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String[] item = getItem(position);

		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.search_result_item, parent, false);
		}

		TextView searchTitle = 		(TextView) 		convertView.findViewById(R.id.searchItemTitle);
		TextView searchSubtitle = 	(TextView)		convertView.findViewById(R.id.searchItemSubtitle);

		assert item != null;

		searchTitle.setText(item[0]);

		switch (item.length) {
			case 1:
				searchSubtitle.setVisibility(View.GONE);
				searchSubtitle.setText("");
				break;
			case 2:
				searchSubtitle.setVisibility(View.VISIBLE);
				searchSubtitle.setText(item[1]);
				break;
		}

		return convertView;
	}
}
