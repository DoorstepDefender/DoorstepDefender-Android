package io.github.doorstepdefender;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {
    public Button updateButton;
    public EditText editText;
    public ImageView imageView;

    private String lastUpdateName = null;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        updateButton = (Button)view.findViewById(R.id.button);
        editText = ((TextInputLayout)view.findViewById(R.id.textInputLayout)).getEditText();
        imageView = (ImageView)view.findViewById(R.id.imageView);

        updateButton.setOnClickListener(v -> {
            lastUpdateName = editText.getText().toString();
            BluetoothService.setDeviceName(lastUpdateName);
            editText.setCursorVisible(false);
        });

        updateButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Update button enabled status.

                String text = editText.getText().toString();
                boolean disabled = text.equals(lastUpdateName);
                updateButton.setEnabled(!(disabled || text.isEmpty()));
                updateButton.setClickable(!(disabled || text.isEmpty()));
                updateButton.postDelayed(this, 1000);
            }
        }, 1000);

        editText.setOnClickListener(v -> {
            editText.setCursorVisible(true);
        });
        editText.setOnKeyListener((v, keyCode, event) -> {
            editText.setCursorVisible(true);
            return false;
        });

        imageView.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Update image based on Bluetooth Service status.
                BluetoothService.PackageStatus status = BluetoothService.getPackageStatus();


                switch (status) {
                    case NOT_CONNECTED:
                        break;
                    case PRESENT:
                        break;
                    case GONE:
                        break;
                }

                imageView.postDelayed(this, 1000);
            }
        }, 1000);

        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}