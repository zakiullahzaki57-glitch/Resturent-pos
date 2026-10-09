package com.myrestaurant.pos;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;
import java.io.OutputStream;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
  private WebView printView;

  public class PrintBridge {
    @JavascriptInterface
    public void print(final String html) {
      runOnUiThread(new Runnable() {
        public void run() {
          printView = new WebView(MainActivity.this);
          printView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView v, String u) {
              PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
              pm.print("Bill", v.createPrintDocumentAdapter("Bill"), new PrintAttributes.Builder().build());
            }
          });
          printView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }
      });
    }
  }

  public class BtBridge {
    @JavascriptInterface
    public String list() {
      try {
        BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
        if (a == null) return "ERR:no bluetooth";
        JSONArray arr = new JSONArray();
        for (BluetoothDevice d : a.getBondedDevices()) {
          JSONObject o = new JSONObject();
          o.put("name", d.getName());
          o.put("addr", d.getAddress());
          arr.put(o);
        }
        return arr.toString();
      } catch (Exception e) {
        return "ERR:" + e.getMessage();
      }
    }

    @JavascriptInterface
    public void print(final String addr, final String text) {
      new Thread(new Runnable() {
        public void run() {
          String r = "OK";
          try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            BluetoothSocket s = a.getRemoteDevice(addr).createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            a.cancelDiscovery();
            s.connect();
            OutputStream os = s.getOutputStream();
            os.write(new byte[]{0x1B, 0x40});
            os.write(text.getBytes("ISO-8859-1"));
            os.write(new byte[]{0x0A, 0x0A, 0x0A, 0x1D, 0x56, 0x00});
            os.flush();
            Thread.sleep(800);
            s.close();
          } catch (Exception e) {
            r = String.valueOf(e.getMessage());
          }
          final String msg = r.replace("'", " ").replace("\n", " ");
          runOnUiThread(new Runnable() {
            public void run() {
              getBridge().getWebView().evaluateJavascript("window.btDone('" + msg + "')", null);
            }
          });
        }
      }).start();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    getBridge().getWebView().addJavascriptInterface(new PrintBridge(), "AndroidPrint");
    getBridge().getWebView().addJavascriptInterface(new BtBridge(), "BtPrint");
    if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"}, 7);
    }
  }
}
