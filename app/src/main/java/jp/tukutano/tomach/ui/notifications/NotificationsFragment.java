package jp.tukutano.tomach.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jp.tukutano.tomach.R;
import jp.tukutano.tomach.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private NotificationsViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        rvChat = root.findViewById(R.id.rvChat);
        adapter = new ChatAdapter(msg -> viewModel.delete(msg));
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(adapter);

        viewModel = new ViewModelProvider(this)
                .get(NotificationsViewModel.class);
        viewModel.getMessages().observe(getViewLifecycleOwner(), msgs -> {
            adapter.setItems(msgs);
            rvChat.scrollToPosition(msgs.size() - 1);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}