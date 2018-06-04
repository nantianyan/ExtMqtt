package com.cmteam.cloudmedia;

import android.util.Log;

import java.util.HashMap;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginManager {
    private static final String TAG = "LoginManager";
    private static final String LOGIN_URL = "http://139.224.128.15:8085/login_app";
    private static final String KEY_RESULT = "result";
    private static final String RESULT_OK = "OK";
    private static final String RESULT_ERROR = "ERROR";
    private static final String KEY_USER_ROLE = "role";
    private static final String KEY_USER_ACCOUNT = "account";
    private static final String KEY_USER_PASSWORD = "password";
    private static final String KEY_USER_TOKEN = "token";
    private static final String KEY_USER_NODE_ID = "node_id";
    private static final String KEY_USER_VENDOR_ID = "vendor_id";
    private static final String KEY_USER_VENDOR_NICK = "vendor_nick";
    private static final String KEY_USER_GROUP_ID = "group_id";
    private static final String KEY_USER_GROUP_NICK = "group_nick";

    private String mLoginURL;
    private HashMap<String, LoginSession> mLoginSessions = new HashMap<>();

    public LoginManager() {

    }

    public boolean login(final String ip, final String port, final String account, final String passwd) {
        URL url = null;
        HttpURLConnection connection = null;
        String responseBody = null;
        mLoginURL = buildLoginURL(ip, port);

        try {
            url = new URL(mLoginURL+"?action=in");
            Log.i(TAG, "Login URL: " + url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            byte[] requestBody = null;

    		try {
                JSONObject jobj = new JSONObject();
                jobj.put(KEY_USER_ACCOUNT, account);
                jobj.put(KEY_USER_PASSWORD, passwd);
                requestBody = jobj.toString().getBytes("UTF-8");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            os.write(requestBody);
            os.flush();
            os.close();
            InputStream is = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            responseBody = getResponseBody(is);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (responseBody == null) {
            Log.e(TAG, "Response body is null!");
            return false;
        }

        CloudMedia.CMUser user = new CloudMedia.CMUser();
        user.account = account;
        user.password = passwd;
        try {
            JSONObject jsonObj = new JSONObject(responseBody);
            if (jsonObj.has(KEY_RESULT)) {
                String result = jsonObj.getString(KEY_RESULT);
                if (!result.equals(RESULT_OK))
                    return false;
            } else
                return false;

            if (jsonObj.has(KEY_USER_ROLE)) {
                user.role = jsonObj.getString(KEY_USER_ROLE);
            }
            if (jsonObj.has(KEY_USER_TOKEN)) {
                user.token = jsonObj.getString(KEY_USER_TOKEN);
            }
            if (jsonObj.has(KEY_USER_NODE_ID)) {
                user.nodeID = jsonObj.getString(KEY_USER_NODE_ID);
            }
            if (jsonObj.has(KEY_USER_VENDOR_ID)) {
                user.vendorID = jsonObj.getString(KEY_USER_VENDOR_ID);
            }
            if (jsonObj.has(KEY_USER_VENDOR_NICK)) {
                user.vendorNick = jsonObj.getString(KEY_USER_VENDOR_NICK);
            }
            if (jsonObj.has(KEY_USER_GROUP_ID)) {
                user.groupID = jsonObj.getString(KEY_USER_GROUP_ID);
            }
            if (jsonObj.has(KEY_USER_GROUP_NICK)) {
                user.groupNick = jsonObj.getString(KEY_USER_GROUP_NICK);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        LoginSession session = new LoginSession();
        session.setUser(user);
        mLoginSessions.put(user.account, session);

        return true;
    }

    public boolean logout(String account) {
        URL url = null;
        HttpURLConnection connection = null;

        try {
            url = new URL(mLoginURL+"?action=out");
            Log.i(TAG, "Logout URL: " + url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            byte[] requestBody = null;

            try {
                JSONObject jobj = new JSONObject();
                jobj.put(KEY_USER_ACCOUNT, account);
                requestBody = jobj.toString().getBytes("UTF-8");
            } catch (JSONException e) {
                e.printStackTrace();
    		}
            os.write(requestBody);
            os.flush();
            os.close();
            InputStream is = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Logout response failed, don't care it anyway!");
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        mLoginSessions.remove(account);

        return true;
    }

    public CloudMedia.CMUser getUser(String account) {
        LoginSession session = mLoginSessions.get(account);
        if (session == null)
            return null;

        return session.getUser();
    }

    private static final String buildLoginURL(final String ip, final String port) {
        return "http://" + ip + ":" + port + "/login_app";
    }

    private class LoginSession {
        private CloudMedia.CMUser mUser;

        private LoginSession() {
        }

        private void setUser(CloudMedia.CMUser user) {
            mUser = user;
        }

        private CloudMedia.CMUser getUser() {
            CloudMedia.CMUser user = new CloudMedia.CMUser();
            user.role = mUser.role;
            user.account = mUser.account;
            user.password = mUser.password;
            user.token = mUser.token;
            user.vendorID = mUser.vendorID;
            user.vendorNick = mUser.vendorNick;
            user.groupID = mUser.groupID;
            user.groupNick = mUser.groupNick;
            return user;
        }
    }

    private static String getResponseBody(InputStream response) {
        BufferedReader bufReader = new BufferedReader(new InputStreamReader(response));
        StringBuilder body = new StringBuilder();
        String line = null;
        try {
            while ((line = bufReader.readLine()) != null) {
                body.append(line);
            }
        } catch (IOException e) {
            
        }
        Log.i(TAG, "Server Response: " + body.toString());
        
        return body.toString();
    }
}
