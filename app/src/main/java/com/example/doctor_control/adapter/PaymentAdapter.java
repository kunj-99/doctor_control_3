package com.example.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.*;
import android.widget.*;

import com.example.doctor_control.R;
import com.example.doctor_control.PaymentModel; // Import your PaymentModel class


import java.util.List;

public class PaymentAdapter extends BaseAdapter {

    private Context context;
    private List<PaymentModel> list;

    public PaymentAdapter(Context context, List<PaymentModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_payment_history, parent, false);
        }

        ImageView image = convertView.findViewById(R.id.img_patient);
        TextView name = convertView.findViewById(R.id.tv_patient_name);
        TextView amount = convertView.findViewById(R.id.tv_amount);

        // 👇 Find the header section
        View headerSection = convertView.findViewById(R.id.header_section);

        // 👇 Show header only for the first item
        if (position == 0) {
            headerSection.setVisibility(View.VISIBLE);
        } else {
            headerSection.setVisibility(View.GONE);
        }

        // Bind data
//        PaymentModel model = list.get(position);
//        image.setImageResource(model.image);
//        name.setText(model.name);
//        amount.setText(model.amount);

        return convertView;
    }

}
