package io.github.doorstepdefender;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {
    public Button updateButton;
    public EditText editText;
    public ImageView imageView;
    public TextView textView;
    public CheckBox checkBox;

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
        textView = (TextView)view.findViewById(R.id.textView);
        checkBox = (CheckBox)view.findViewById(R.id.checkBox);

        lastUpdateName = BluetoothService.getDeviceName();
        if (lastUpdateName != null) {
            editText.setText(lastUpdateName);
        }

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

        checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            BluetoothService.setAlarmEnabled(b);
        });

        imageView.postDelayed(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                // Update image based on Bluetooth Service status.
                BluetoothService.PackageStatus status = BluetoothService.getPackageStatus();

                switch (status) {
                    case NOT_CONNECTED:
                        imageView.setImageResource(R.drawable.not_connected);
                        textView.setText("Not Connected");
                        break;
                    case PRESENT:
                        imageView.setImageResource(R.drawable.yes_package);
                        textView.setText("Package");
                        break;
                    case GONE:
                        imageView.setImageResource(R.drawable.no_package);
                        textView.setText("No Package");
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