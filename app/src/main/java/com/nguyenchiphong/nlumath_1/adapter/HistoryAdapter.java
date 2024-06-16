package com.nguyenchiphong.nlumath_1.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nguyenchiphong.nlumath_1.R;
import com.nguyenchiphong.nlumath_1.activity.SolveActivityShort;
import com.nguyenchiphong.nlumath_1.dao.DatabaseHelper;
import com.nguyenchiphong.nlumath_1.model.History;

import java.util.ArrayList;

import maes.tech.intentanim.CustomIntent;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    Context context;
    ArrayList<History> histories;

    public HistoryAdapter(Context context, ArrayList<History> histories) {
        this.context = context;
        this.histories = histories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.row, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtTitle.setText(histories.get(position).getTitle());
        holder.txtDescription.setText(histories.get(position).getDescription());
        holder.imageView.setImageResource(histories.get(position).getImg());
        holder.checkbox.setChecked(histories.get(position).isChecked());

    }

    @Override
    public int getItemCount() {
        return histories.size();
    }

    // Xóa 1 item trong RecycleView
    public void removeItem(int position) {
        histories.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Khởi tạo DatabaseHelper
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        ImageView imageView;
        TextView txtTitle, txtDescription;
        CheckBox checkbox;

        public ViewHolder(final View itemView) {
            super(itemView);
            // ánh xạ bên row qua
            this.imageView = (ImageView) itemView.findViewById(R.id.imgViewHistory);
            this.txtTitle = (TextView) itemView.findViewById(R.id.txtTitle);
            this.txtDescription = (TextView) itemView.findViewById(R.id.txtDescription);
            this.checkbox = (CheckBox) itemView.findViewById(R.id.checkbox);

            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // nếu có chọn ô checkbox
                    if (checkbox.isChecked()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        // Cài đặt các thuộc tính
                        builder.setTitle("Thông báo!");
                        builder.setMessage("Bạn có chắc chắn muốn xóa '" + txtTitle.getText() + "' ?");
                        builder.setIcon(R.drawable.ic_notification);
                        // Cài đặt button Cancel- Hiển thị Toast
                        builder.setPositiveButton("Hủy", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Toast.makeText(HistoryActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                                // sau đó set CheckBox == false
                                checkbox.setChecked(false);
                            }
                        });
                        // Cài đặt button Yes Dismiss ẩn Dialog
                        builder.setNegativeButton("Xóa", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // sau đó set CheckBox == false
                                try {
                                    databaseHelper.deleteByName(txtTitle.getText().toString());
                                    notifyItemRemoved(getAdapterPosition());
                                    //notifyDataSetChanged();
                                    checkbox.setChecked(false);
                                    Toast.makeText(context, "Đã xóa " + txtTitle.getText(), Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    Log.e("Lỗi ", e.getMessage());
                                }
                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } else {
                        Toast.makeText(itemView.getContext(), "Bỏ chọn: " + txtTitle.getText(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            // xử lý sự kiện khi Click từng item của RecycleView ở đây
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // removeItem(getAdapterPosition());
                    // Toast.makeText(itemView.getContext(), "" + txtTitle.getText(), Toast.LENGTH_LONG).show();
                    // gửi data và chuyển data qua ResultActivity
                    Intent intent = new Intent(itemView.getContext(), SolveActivityShort.class);
                    intent.putExtra("dataHistory", txtTitle.getText());
                    context.startActivity(intent);
                    CustomIntent.customType(itemView.getContext(), "left-to-right");
                }
            });
        }
    }
}
