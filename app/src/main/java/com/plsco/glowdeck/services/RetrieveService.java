package com.plsco.glowdeck.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.plsco.glowdeck.auth.LoginActivity;
import com.plsco.glowdeck.glowdeck.CurrentGlowdecks;
import com.plsco.glowdeck.ui.MainActivity;
import com.plsco.glowdeck.ui.StreamsApplication;

import org.apache.http.client.utils.URIUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static com.android.volley.VolleyLog.TAG;


/**
 * Created by smartdev on 8/23/17.
 */

public class RetrieveService extends Service {

    static RetrieveService msCurrentContext ;
    public static String uuids = "";
    public  userLocation userLocation;
    public ArrayList<String> streamArray = new ArrayList();
//    public HashMap<String,String> streamDict = new HashMap<String,String>();

    public int weatherUpdated  = 0;
    public boolean firmwareUpdateActive  = false;


    private Handler handler;
    private Runnable runnable;
    private final int runTime = 10*1000;
    private final int waittime = 60*1000;
    public String keyquery = "news,politics,technology,war,sports";
    /* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    /* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();

        msCurrentContext = this;

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {

                handler.postDelayed(runnable, runTime);
                updateWeather(true);

            }
        };
        handler.post(runnable);

        final Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                getRequestStreams(keyquery);
            }
        }, waittime);

        getAddress();

    }


    /*

     */
    public void updateWeather(boolean tx) {

        if (!isWeatherAvailable()) { return; }

        if (userLocation == null)
            return;

        if (userLocation.country.length() == 2 && weatherUpdated > -3) {

            // "&APPID=0dc5670d4037da812f743888ecf5a3e3"

            String weatherString = "http://api.openweathermap.org/data/2.5/weather?q=";

            weatherString += userLocation.city;

            weatherString += ",";

            weatherString += userLocation.country;

            weatherString += "&APPID=0dc5670d4037da812f743888ecf5a3e3";

            URI uri = URI.create(weatherString);

            String weatherUrl = uri.toASCIIString();

            getWeatherData(weatherUrl, tx);

        }
    }

    public void getWeatherData(String urlString, final boolean tx) {

        if (!isWeatherAvailable()) { return; };

        RequestQueue queue = Volley.newRequestQueue(msCurrentContext);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, urlString, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        setWeatherData(response, tx);

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });


        queue.add(jsObjRequest);
    }

    public void setWeatherData(JSONObject json, boolean tx) {


        try {

            JSONArray weather = (JSONArray) json.get("weather");

            if (weather.length() > 0 && weather != null) {
                userLocation.forecast = weather.getJSONObject (0).getString("main");

            }else {
                userLocation.forecast = "Fair";
            }

            JSONObject main = json.getJSONObject("main");

            int tempConvert = 0;

            if (main.has("temp")) {
                double temp = main.getDouble("temp");

                if (userLocation.country.equals("US"))
                    tempConvert = (int)(temp * (9.0/5.0) - 459.67);
                else
                    tempConvert = (int)(temp - 273.15);

                userLocation.temperature = String.valueOf(tempConvert);
            }else
                userLocation.temperature = "72";


//            String currentWeatherString =
            //tickerTextRightToLeft


            String glowdeckTransmit = String.format("WTR:%s|%s|%s^", userLocation.temperature, userLocation.forecast, userLocation.city);

            bleSend(glowdeckTransmit);
            weatherUpdated = 1;
        } catch (JSONException e) {
            weatherUpdated -= 1;
        }

    }

    public void bleSend(String sendCmd) {

        /*
        if (firmwareUpdateActive && !sendCmd.contains("GFU")) {
            Log.d("[TX CANCEL] %s", sendCmd);
            return;
        }
        */

        final StreamsApplication streamsApplication = (StreamsApplication) MainActivity.getMainActivity().getApplication();

        if (streamsApplication.getBluetoothSppManager().firmwareUpdateInProgress == false) {

            Log.d("[SEND] %s", sendCmd);

            streamsApplication.getBluetoothSppManager().sendMessage(sendCmd);

        }
        else {

            Log.d("[TX CANCEL] %s", sendCmd);

        }

    }

    public  boolean isWeatherAvailable() {

        return true;

//        var zeroAddress = sockaddr_in()
//        zeroAddress.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
//        zeroAddress.sin_family = sa_family_t(AF_INET)
//
//        guard let defaultRouteReachability = SCNetworkReachabilityCreateWithName(kCFAllocatorDefault, ("api.openweathermap.org" as NSString).utf8String!) else { return false }
//        /*
//         guard let defaultRouteReachability = withUnsafePointer(to: &zeroAddress, {
//         $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
//         SCNetworkReachabilityCreateWithAddress(nil, $0)
//         }
//         }) else {
//         return false
//         }
//         */
//
//        var flags: SCNetworkReachabilityFlags = []
//        if !SCNetworkReachabilityGetFlags(defaultRouteReachability, &flags) {
//        return false
//    }
//
//        let isReachable = flags.contains(.reachable)
//        let needsConnection = flags.contains(.connectionRequired)
//
//        return (isReachable && !needsConnection)
    }


    public void showStream() {

        boolean streamSwitch;

        CurrentGlowdecks currentGlowdecks = MainActivity.getMainActivity().getCurrentGlowdecks();

        CurrentGlowdecks.GlowdeckDevice connectedGlowdeck = currentGlowdecks.getCurrentlyConnected();

        if (connectedGlowdeck == null) {

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    showStream();
                }
            }, waittime);
            return;


        }





        if (!connectedGlowdeck.getCurrentDisplayStreamsSwitch()) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    showStream();
                }
            }, waittime);
            return;
        }
        if (streamArray.size() > 0) {
            String pop = streamArray.remove(0);
            bleSend(pop);
        }

        if (streamArray.size() == 0) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    getRequestStreams(keyquery);
                }
            }, 60*1000);
        }
        else {

            Random r = new Random();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    showStream();
                }
            }, waittime);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    //-----------------------------------------------------------------------------------------------

    static <T> T[] append(T[] arr, T element) {
        final int N = arr.length;
        arr = Arrays.copyOf(arr, N + 1);
        arr[N] = element;
        return arr;
    }

    public void getRequestStreams( String query) {
        String urlString = "https://webhose.io/search?token=dc0d4206-31f8-482c-90ce-515b9e312254&format=json&q=";
        ArrayList keywords = new ArrayList();
        int timeInterval = (int)(new Date().getTime() * 1000.0) - (60*60*24*3);

        String timeString = String.format("%d" , timeInterval);


        String suffix = "%20language%3A(english)%20site_category%3Amedia%20(site_type%3Anews)&sort=relevancy";

        if (query.contains(",")) {
            keywords = new ArrayList(Arrays.asList(query.split(",")));
        }
        else {
            keywords.add(query);
        }


        String params = "(";
        try {
            if (keywords.size() == 1) {

                params += URLEncoder.encode((String) keywords.get(0), "utf-8");
                params += ")";

            }
            else {
                for (int i = 0; i <keywords.size(); i++) {
                    if (i < keywords.size()-1) {
                        params += URLEncoder.encode((String) keywords.get(0), "utf-8") + "%20OR%20";
                    }
                    else {
                        params += URLEncoder.encode((String) keywords.get(0), "utf-8") + ")";
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        urlString += params;
        urlString += suffix;

        Log.d("dbg", "Stream Query Endpoint URL:" + urlString);


        RequestQueue queue = Volley.newRequestQueue(msCurrentContext);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, urlString, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject jsonDict) {

                        try {

                            String uuidAdd = "";
                            String result = "";

                            if (jsonDict.has("totalResults")) {
                                int results = jsonDict.getInt("totalResults");
                                if (results == 0) {
                                    return;
                                } else {

                                    if (jsonDict.has("posts")) {

                                        JSONArray postsDict = jsonDict.getJSONArray("posts");

                                        for (int i = 0; i < postsDict.length() ; i++)
                                        {
                                            JSONObject postDict = postsDict.getJSONObject(i);

                                            if (postDict.has("uuid")) {

                                                uuidAdd = postDict.getString("uuid");

                                                if (!uuids.contains(uuidAdd)) {
                                                    if (uuids == "") {
                                                        uuids += uuidAdd;
                                                    } else {
                                                        uuids += String.format(",%s", uuidAdd);
                                                    }

                                                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(msCurrentContext);
                                                    SharedPreferences.Editor editor = sharedPrefs.edit();

                                                    editor.putString("uuids", uuids);

                                                    editor.commit();


                                                    if (postDict.has("thread")) {
                                                        JSONObject threadDict = postDict.getJSONObject("thread");
                                                        if (threadDict.has("site") && threadDict.has("title") && threadDict.has("url")) {
                                                            String result1 = "NOT:";
                                                            String source = threadDict.getString("site");
                                                            if (source.contains(".")) {

                                                                ArrayList<String> srcComps = new ArrayList(Arrays.asList(source.split("\\.")));


                                                                source = srcComps.get(0);
                                                            }
                                                            if (source.length() > 5) {
                                                                result1 += source.substring(0, 1).toUpperCase() + source.substring(1);
                                                            } else {
                                                                result1 += source.toUpperCase();
                                                            }
                                                            result1 += "|N|";

                                                            String headline = threadDict.getString("title");

                                                            result1 += headline;
                                                            result1 += "|1d^\r";

                                                            String streamUrl = threadDict.getString("url");

                                                            streamArray.add(result1);
//                                                            streamDict.put(result1, streamUrl);

                                                        }
                                                    }
                                                }
                                            }
                                        }


                                        if (!streamArray.isEmpty()) {

                                            final Handler handler = new Handler();
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // Do something after 5s = 5000ms
                                                    showStream();
                                                }
                                            }, waittime);

                                        } else {

                                            final Handler handler = new Handler();
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // Do something after 5s = 5000ms
                                                    showStream();
                                                }
                                            }, 180*1000);
                                        }

                                    }
                                }

                            }
                        }catch (JSONException e) {
                            weatherUpdated -= 1;
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                        final Handler handler1 = new Handler();
                        handler1.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Do something after 5s = 5000ms
                                getRequestStreams(keyquery);
                            }
                        }, 10*1000);
                    }
                });

        queue.add(jsObjRequest);

    }



    //location service



    protected void getAddress() {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String errorMessage = "";

        // Get the location passed to this service through an extra.

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Float latitude = sharedPrefs.getFloat(LoginActivity.PrefsLatitude, 0);
        Float longitude = sharedPrefs.getFloat(LoginActivity.PrefsLongitude, 0);



        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = "Service not available";
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "invalid user lat and long";
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + latitude +
                    ", Longitude = " +
                    longitude, illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "no address found";
                Log.e(TAG, errorMessage);
            }

        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, "address found");

            userLocation = new userLocation();
            userLocation.city = address.getLocality();
            userLocation.country = address.getCountryCode();
            userLocation.state = address.getSubAdminArea();

            updateWeather(true) ;

        }
    }







}
