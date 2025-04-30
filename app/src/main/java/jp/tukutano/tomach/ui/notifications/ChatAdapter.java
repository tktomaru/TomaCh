package jp.tukutano.tomach.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jp.tukutano.tomach.R;
import jp.tukutano.tomach.db.ChatMessage;

public class ChatAdapter
        extends RecyclerView.Adapter<ChatAdapter.VH> {

    private List<ChatMessage> list = new ArrayList<>();
    private final OnDeleteListener listener;

    public interface OnDeleteListener {
        void onDelete(ChatMessage msg);
    }

    public ChatAdapter(OnDeleteListener l) {
        listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ChatMessage msg = list.get(pos);
        h.tvJa.setText(msg.contentJa);
        h.tvEn.setText(msg.contentEn);
        h.btnDel.setOnClickListener(v -> listener.onDelete(msg));
    }

    @Override public int getItemCount() { return list.size(); }

    public void setItems(List<ChatMessage> items) {
        list = items;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvJa, tvEn;
        ImageButton btnDel;
        VH(View v) {
            super(v);
            tvJa  = v.findViewById(R.id.tvBodyJa);
            tvEn  = v.findViewById(R.id.tvBodyEn);
            btnDel= v.findViewById(R.id.btnDelete);
        }
    }
}
