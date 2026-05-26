package com.wolza.arduinoapp;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends BaseAdapter {

    private ArrayList<String> deviceList;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private LayoutInflater inflater;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }

    public DeviceListAdapter(BluetoothConnectionActivity context, ArrayList<String> deviceList,
                             ArrayList<BluetoothDevice> bluetoothDevices, OnItemClickListener listener) {
        this.deviceList = deviceList;
        this.bluetoothDevices = bluetoothDevices;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_bluetooth_device, parent, false);
            holder = new ViewHolder();
            holder.ivDeviceIcon = convertView.findViewById(R.id.ivDeviceIcon);
            holder.tvDeviceName = convertView.findViewById(R.id.tvDeviceName);
            holder.tvDeviceAddress = convertView.findViewById(R.id.tvDeviceAddress);
            holder.tvDeviceStatus = convertView.findViewById(R.id.tvDeviceStatus);
            holder.llContainer = convertView.findViewById(R.id.llContainer);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String deviceInfo = deviceList.get(position);
        String[] parts = deviceInfo.split("\n");

        if (parts.length >= 2) {
            String name = parts[0];
            String address = parts[1];

            holder.tvDeviceName.setText(name);
            holder.tvDeviceAddress.setText(address);

            if (name.contains("HC-06") || name.contains("Arduino") || address.contains("HC-06")) {
                holder.ivDeviceIcon.setImageResource(R.drawable.ic_arduino);
                holder.tvDeviceStatus.setText("✓ HC-06 Compatible");
                holder.tvDeviceStatus.setTextColor(0xFF4CAF50);
            } else {
                holder.ivDeviceIcon.setImageResource(R.drawable.ic_bluetooth);
                holder.tvDeviceStatus.setText("Other Device");
                holder.tvDeviceStatus.setTextColor(0xFF757575);
            }
        }

        final int pos = position;
        holder.llContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null && bluetoothDevices != null && pos < bluetoothDevices.size()) {
                    listener.onItemClick(bluetoothDevices.get(pos));
                }
            }
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView ivDeviceIcon;
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        TextView tvDeviceStatus;
        LinearLayout llContainer;
    }
}