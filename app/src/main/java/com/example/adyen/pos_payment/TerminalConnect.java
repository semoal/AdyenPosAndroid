package com.example.adyen.pos_payment;

/**
 * Created by sergiomoreno on 5/9/17.
 */


import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.adyen.library.AddDeviceListener;
import com.adyen.library.AdyenLibraryInterface;
import com.adyen.library.DeviceData;
import com.adyen.library.DeviceInfo;
import com.adyen.library.exceptions.NotYetRegisteredException;
import com.adyen.library.real.LibraryReal;


public class TerminalConnect extends Fragment {
    private DeviceInfo deviceInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //// TODO: 30/8/17
        // Revisar que viene de terminals por si es necesario su utilidad aquí
        //deviceInfo = getArguments().getParcelable(Terminals.DEVICE_INFO);
        deviceInfo = getArguments().getParcelable("deviceInfo");

    }

    @Override
    public void onStart() {
        super.onStart();

        AdyenLibraryInterface lib = null;
        try {
            lib = LibraryReal.getLib();
        } catch (NotYetRegisteredException e) {
            //Log.e(tag, "", e);
            System.out.println("TERMINALCONNECT-> NO SE HA REGISTRADO EL DISPOSITIVO");
        }

        try {
            lib.addDevice(new AddDeviceListener() {
                @Override
                public void onAddDeviceComplete(final AddDeviceListener.CompletedStatus completedStatus, final String message, DeviceData deviceData) {
                    if (!TerminalConnect.this.isVisible()) {
                        return;
                    }

                    switch (completedStatus) {
                        case OK:
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // addRow(getString(R.string.boarding_complete_ok), MessageType.Success);
                                    System.out.println("Terminal conectado");
                                }
                            });
                            break;
                        case ERROR_IDENTIFY:
                        case ERROR:
                        case ERROR_REGISTER:
                        case ERROR_SYNCACTION:
                        case ERROR_SYNCDEVICE:
                        case ERROR_VERIFY:
                        case ERROR_NONETWORK:
                        case ERROR_NOROUTE:
                        case TIMEOUT:
                        default:
                            System.out.println("Error");
                            break;
                    }
                }

                @Override
                public void onProgress(final AddDeviceListener.ProcessStatus processStatus, final String message) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Intentando conectar");
                        }
                    });
                }
            }, deviceInfo);

        } catch (final Exception e) {
            System.out.println("add device threw exception"+e);
        }
    }
}