package com.zebra.basicintent1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ScanItemAdapter extends ArrayAdapter<ScanItem> {

    public ScanItemAdapter(Context context, List<ScanItem> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScanItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_scan, parent, false);
        }

        TextView tvBarcode = convertView.findViewById(R.id.tvBarcode);
        TextView tvWeight = convertView.findViewById(R.id.tvWeight);

        tvBarcode.setText(item.getBarcodeData());
        
        String weight = item.getWeight();
        if (weight != null && !weight.isEmpty()) {
            tvWeight.setText(weight + " g");
            tvWeight.setVisibility(View.VISIBLE);
        } else {
            tvWeight.setText("No weight");
            tvWeight.setVisibility(View.VISIBLE);
        }

        return convertView;
    }
}
