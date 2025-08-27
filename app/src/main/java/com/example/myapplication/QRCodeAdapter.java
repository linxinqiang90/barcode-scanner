package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class QRCodeAdapter extends RecyclerView.Adapter<QRCodeAdapter.QRCodeViewHolder> {

    private final List<String> qrCodes = new ArrayList<>();

    @NonNull
    @Override
    public QRCodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qrcode, parent, false);
        return new QRCodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QRCodeViewHolder holder, int position) {
        String code = qrCodes.get(position);
        holder.tvQRCode.setText(code);
    }

    @Override
    public int getItemCount() {
        return qrCodes.size();
    }

    // 添加新二维码到列表顶端
    public void addQRCode(String code) {
        if (qrCodes.contains(code)) {
            return;
        }
        qrCodes.add(0, String.format("%s.%s", qrCodes.size(), code)); // 最新的显示在最上面
        notifyItemInserted(0);
    }

    public void clear() {
        int size = qrCodes.size();
        if (size > 0) {
            qrCodes.clear();
            notifyItemRangeRemoved(0, size);
        }
    }


    static class QRCodeViewHolder extends RecyclerView.ViewHolder {
        TextView tvQRCode;

        QRCodeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQRCode = itemView.findViewById(R.id.tvQRCode);
        }
    }
}
