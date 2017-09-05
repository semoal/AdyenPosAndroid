package com.example.adyen.pos_payment;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import com.adyen.library.AddDeviceListener;
import com.adyen.library.AdyenLibraryInterface;
import com.adyen.library.AppInfo;
import com.adyen.library.DeviceData;
import com.adyen.library.DeviceInfo;
import com.adyen.library.RegistrationCompleteListener;
import com.adyen.library.ServerMode;
import com.adyen.library.TransactionListener;
import com.adyen.library.TransactionRequest;
import com.adyen.library.callbacks.PrintReceiptListener;
import com.adyen.library.callbacks.PrintReceiptRequest;
import com.adyen.library.callbacks.PrintReceiptResponse;
import com.adyen.library.exceptions.AlreadyRegisteredException;
import com.adyen.library.exceptions.AlreadyRegisteringAppException;
import com.adyen.library.exceptions.AppAlreadyRegisteredException;
import com.adyen.library.exceptions.InvalidRequestException;
import com.adyen.library.exceptions.NotYetRegisteredException;
import com.adyen.library.real.LibraryReal;
import com.adyen.library.util.TenderOptions;
import com.adyen.posregister.Receipt;
import com.adyen.posregister.TenderStates;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends ActionBarActivity {
    private static final String LOG_TAG = "MainActivity";
    private static final String MERCHANT_CODE = "MerchantExample";
    private static final String USER_NAME = "ws";
    private static final String PASSWORD =  "ijeijgjigijgijijg";
    private static final String terminalIP = "192.168.1.222";
    private static final String currentCode = "EUR";
    private PrintReceiptRequest receiptRequest;
    public List<Receipt> merchantReceipts = new ArrayList<Receipt>();
    public List<Receipt> cardHolderReceipts = new ArrayList<Receipt>();
    boolean printCardHolderReceipt;
    boolean printMerchantReceipt;

    TransactionRequest tr;
    AdyenLibraryInterface lib = null;
    DeviceInfo deviceInfo = new DeviceInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void buttonOnClick(View v){
        try {
            //1.0 -> We register the library
            //Parameters needed: the context, and a name for the app
            LibraryReal.registerLib(MainActivity.this, "ExampleApp");
            //2.0 -> We get the lib
            lib = LibraryReal.getLib();
            //3.0 -> If your account on adyen is TEST, login in ca-test.adyen.com
            lib.setServerMode(ServerMode.TEST);
            //3.1 -> If your account on adyen is LIVE, login in ca-live.adyen.com
            //lib.setServerMode(ServerMode.LIVE);
            //3.2 -> If your account on adyen is DEV or BETA just change the server mode

            //4 -> Try to register the app
            try {
                RegistrationCompleteListener rcl = new RegistrationCompleteListener() {
                    @Override
                    public void onRegistrationComplete(CompletedStatus completedStatus, AppInfo appInfo, String s) {
                        Log.d(LOG_TAG, "App registration completed......"+completedStatus.toString()+ "----appInfo: "+ appInfo + " s: "+ s );
                    }};
                //4.1 -> Register the app with the merchant code, username, password and the registration listener
                //4.2 -> MerchantCode is the account linked to your MAIN account
                //4.3 -> You can see your usernames in the customer area -> Settings -> Users and the WEB SERVICE user role is what you need
                lib.registerApp(MERCHANT_CODE, USER_NAME, PASSWORD, rcl);
                //4.4 -> We need to set the printreveiptlistener because is not optional to print, we developed the printer independently so we set this always to true
                lib.setPrintReceiptListener(printReceiptListener);
                //4.5 -> We set the connection type to wifi, can be bluetooth too
                deviceInfo.setConnectionType(DeviceInfo.TYPE_WIFI);
                //4.6 -> We set the ip of the terminal (lan network)
                deviceInfo.setDeviceId(terminalIP);
                //4.7 -> A name for searching it in the customer area, be original mate :)
                deviceInfo.setFriendlyName("05_09_dominos");

                //4.8 -> We try to add the device to the platform
                try {
                    lib.addDevice(new AddDeviceListener() {
                        @Override
                        public void onAddDeviceComplete(final AddDeviceListener.CompletedStatus completedStatus, final String message, DeviceData deviceData) {
                            switch (completedStatus) {
                                case OK:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            System.out.println("---------------------> Terminal connected");
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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("Trying to connect");
                                }
                            });
                        }
                    }, deviceInfo);
                    //4.9 -> We re-check the connection of the device
                    enableEagerConnection();
                    //5.0 -> Initiate the transaction request is the information detailed of the payment process
                    tr = new TransactionRequest();
                    //5.1 -> Quantity of money you want to pay
                    tr.setValue(5000);
                    //5.2 -> Description of the pay
                    tr.setVendorDescription("Payment description");
                    //5.3 -> CurrencyCode for example: EUR
                    tr.setCurrencyCode(currentCode);
                    //5.4 -> Shopper email is necessary so set one.
                    tr.setShopperEmail("sergio.moreno@enzymeadvisinggroup.com");
                    //5.5 -> Tenderoptions are necessary to handle all the receipts
                    List<TenderOptions> tenderOptions = new ArrayList<TenderOptions>();
                    tenderOptions.add(TenderOptions.ReceiptHandler);
                    tr.setTenderOptions(tenderOptions);
                    //5.6 -> Initiate the transaction, with the requestTransaction(info) and the transactionlistener(details for pinpad/pos)
                    lib.initiateTransaction(tr,mTransactionListener);
                } catch (final Exception e) {
                    System.out.println("add device threw exception"+e);
                }
            } catch (AppAlreadyRegisteredException e) {
                e.printStackTrace();
            } catch (AlreadyRegisteringAppException e) {
                e.printStackTrace();
            } catch (InvalidRequestException e) {
                e.printStackTrace();
            }

        } catch (NotYetRegisteredException exception) {
            Log.d(LOG_TAG, exception.getMessage());
        } catch (AlreadyRegisteredException e) {
            e.printStackTrace();
        }

    }

    /*
    * Checks connection of the device (POS/pinpad)
    */
    public void enableEagerConnection(){
        // starts the terminal connect intent service
        // eager connection starts a DeviceInfoTask that checks for available updates
        lib.enableEagerConnection();
    }

    private PrintReceiptListener printReceiptListener = new PrintReceiptListener() {
        @Override
        public PrintReceiptResponse onPrintReceiptRequested(final PrintReceiptRequest req) {
            receiptRequest = req;
            PrintReceiptResponse response = new PrintReceiptResponse();

            if (req.getMerchantReceipt() != null) {
                merchantReceipts.add(req.getMerchantReceipt());
            }
            if (req.getCardholderReceipt() != null) {
                cardHolderReceipts.add(req.getCardholderReceipt());
            }

            if (printCardHolderReceipt || printMerchantReceipt) {
                boolean printed = true;
                //boolean printed = false;
                if (printed) {
                    response.setStatus(com.adyen.posregister.PrintReceiptResponse.Status.Printed);
                } else {
                    response.setStatus(com.adyen.posregister.PrintReceiptResponse.Status.Error);
                }
            } else {
                response.setStatus(com.adyen.posregister.PrintReceiptResponse.Status.Printed);
            }
            return response;
        }
    };

    /*
    *
    *  Method that implements the transaction listener --> it changes the view in the POS device
    */
    private TransactionListener mTransactionListener = new TransactionListener() {

        // integer - code of the error
        @Override
        public void onTransactionComplete(final CompletedStatus completedStatus, final String message, final Integer integer, Map<String, String> stringStringMap) {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    switch (completedStatus) {
                        case APPROVED:
                            break;
                        // happens for: amount too low, card reasons
                        // when canceling the tx via lib.cancelTransaction while the signature screen is displayed
                        // the tx is declined
                        case DECLINED:
                            // from a merchant or shopper
                        case CANCELLED:
                        case UNKNOWN:
                            // may occur when communication is lost; check outcome on a terminal or send cancelOrRefund.
                        case ERROR:
                            break;
                    }

                }
            });
        }

        // state of the communication between the app and the terminal
        @Override
        public void onProgress(final ProcessStatus processStatus, final String message) {

            switch (processStatus) {
                // app is connecting to the terminal - just starts a connection
                case CONNECTING:
                    // identifying the terminal - the connection was established and now the terminal is identified
                case IDENTIFYING:
                    // the terminal is processing the payment
                case PROCESSING:
                    // the phone doesn't have internet connection
                case NONETWORK:
                    // the phone cannot connect to the Adyen backend (even if there is internet connection)
                case NOROUTE:
                    // the merchant or the shopper cancelled the tx
                case CANCELLING:
                    break;

            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // Toast.makeText(PaymentActivity.this, "tx listener: process status:" + processStatus.name() + " " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // called when the tendered state changed (state of a payment)
        @Override
        public void onStateChanged(final TenderStates tenderStates, final String s, Map<String, String> stringStringMap) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String newStatus = tenderStates.name() + " " + s;
                   // updateStatus(newStatus);
                }
            });

            switch (tenderStates) {
                //
                case PIN_DIGIT_ENTERED:
                    Log.d(LOG_TAG, "!! PIN_DIGIT_ENTERED !!");
                    break;
                // tx was initiated
                case INITIAL:
                    // a new tender was created
                case TENDER_CREATED:
                    // the card was inserted
                case CARD_INSERTED:
                    // the card was swiped
                case CARD_SWIPED:
                    // waiting for some input from the customer on the terminal
                case WAIT_FOR_APP_SELECTION:
                    // the customer selected something from the previous step
                case APPLICATION_SELECTED:
                    // waiting for an amount to be adjusted based on the gratuity
                case WAIT_FOR_AMOUNT_ADJUSTMENT:
                    // gratuity option was selected in tender options and the shuttle requests for it
                case ASK_GRATUITY:
                    // gratuity amount was entered
                case GRATUITY_ENTERED:
                    // dynamic currency conversion was selected in tender options
                case ASK_DCC:
                    // the conversion amount was accepted
                case DCC_ACCEPTED:
                    // the conversion amount was rejected
                case DCC_REJECTED:
                    // the payment is being processed (on the terminal you can see the progress bar)
                case PROCESSING_TENDER:
                    // pin is being requested on the terminal
                case WAIT_FOR_PIN:
                    // the user accepted the pin
                case PIN_ENTERED:
                    // for MKE the card details are requested
                case PROVIDE_CARD_DETAILS:
                    // for MKE the card details were provided
                case CARD_DETAILS_PROVIDED:
                    // starting printing the receipt (receipt generation)
                case PRINT_RECEIPT:
                    return;
                    // the receipt printing was finished
                case RECEIPT_PRINTED:
                    return;
                    // the acquirer sends a referral status
                case REFERRAL:
                    // the referral code was checked
                case REFERRAL_CHECKED:
                    // after the user has entered the signature, there's a check between the signature on the card and the one on the screen/receipt
                case CHECK_SIGNATURE:
                    // the signature matched the one on the card
                case SIGNATURE_CHECKED:
                    // additional data (like currency conversion rate, adjusted amount for gratuity and others) are available
                case ADDITIONAL_DATA_AVAILABLE:
                    // final states of a tx.
                case ERROR:
                case APPROVED:
                case DECLINED:
                case CANCELLED:
                    // transaction cancelled
                case UNKNOWN:
                case ACKNOWLEDGED:
                    break;
            }

        }
    };

}
