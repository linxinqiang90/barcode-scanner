package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QRCodeAdapter extends RecyclerView.Adapter<QRCodeAdapter.QRCodeViewHolder> {

    private final List<String> qrCodes = new ArrayList<>();
    private final Set<String> seenCodes = new HashSet<>(); // 只保存原始二维码内容

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
    public void addQRCode(Set<String> codes) {
        boolean update = false;
        for (String code : codes) {
            if (seenCodes.add(code)) {
                qrCodes.add(0, String.format("%s.%s", qrCodes.size() + 1, code)); // 最新的显示在最上面
                update = true;
            }
        }
        if (update) {
            notifyItemInserted(0);
        }
    }

    public void clear() {
        int size = qrCodes.size();
        qrCodes.clear();
        seenCodes.clear();
        if (size > 0) {
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
