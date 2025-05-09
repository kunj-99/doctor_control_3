package com.example.doctor_control.adapter; // Use your actual package name

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.doctor_control.PaymentModel;  // Add this import statement

import com.example.doctor_control.R;

import java.util.List;

public class PendingPaymentAdapter extends BaseAdapter {

    private Context context;
    private List<PaymentModel> list;

    public PendingPaymentAdapter(Context context, List<PaymentModel> list) {
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
        // ViewHolder pattern to improve performance (optional)
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_pending_payment, parent, false);
        }

        ImageView image = convertView.findViewById(R.id.img_patient);
        TextView name = convertView.findViewById(R.id.tv_patient_name);
        TextView amount = convertView.findViewById(R.id.tv_amount);
        TextView status = convertView.findViewById(R.id.tv_status);

        PaymentModel model = list.get(position);
        image.setImageResource(model.getImage()); // Set the image using the resource ID
        name.setText(model.getName());
        amount.setText(model.getAmount());
        status.setText(model.getStatus());

        return convertView;
    }
}
