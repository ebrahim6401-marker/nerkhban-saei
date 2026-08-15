package ir.saeigroup.nerkhban;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RateWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH = "ir.saeigroup.nerkhban.REFRESH";
    private static final String API = "https://didban-bazar.vercel.app/api/rates";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { refresh(context); }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) refresh(context);
    }

    private void refresh(Context context) {
        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                JSONObject data = readJson(API);
                Map<String, JSONObject> rates = new HashMap<>();
                JSONArray items = data.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) rates.put(items.getJSONObject(i).getString("key"), items.getJSONObject(i));
                RemoteViews views = baseViews(context);
                bind(views, R.id.eur, "یورو", rates.get("eur"));
                bind(views, R.id.usd, "دلار", rates.get("usd"));
                bind(views, R.id.aed, "درهم", rates.get("aed"));
                bind(views, R.id.emami, "سکه امامی", rates.get("coin_emami"));
                bind(views, R.id.half, "نیم‌سکه", rates.get("coin_half"));
                bind(views, R.id.quarter, "ربع‌سکه", rates.get("coin_quarter"));
                views.setTextViewText(R.id.updated, "بروزرسانی: " + new SimpleDateFormat("HH:mm", new Locale("fa", "IR")).format(new Date()));
                updateAll(context, views);
            } catch (Exception e) {
                RemoteViews views = baseViews(context);
                views.setTextViewText(R.id.updated, "خطا در دریافت؛ برای تلاش دوباره لمس کنید");
                updateAll(context, views);
            } finally { pending.finish(); }
        }).start();
    }

    private static RemoteViews baseViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rate_widget);
        Intent refresh = new Intent(context, RateWidgetProvider.class).setAction(ACTION_REFRESH);
        views.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(context, 1, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent open = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.title, PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return views;
    }

    private static void bind(RemoteViews views, int id, String label, JSONObject row) throws Exception {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("fa", "IR"));
        double change = row.getDouble("change");
        String arrow = change >= 0 ? "▲" : "▼";
        views.setTextViewText(id, label + "  " + nf.format(row.getDouble("price")) + "  " + arrow + Math.abs(change) + "%");
    }

    private static JSONObject readJson(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder out = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) out.append(line);
            return new JSONObject(out.toString());
        } finally { connection.disconnect(); }
    }

    private static void updateAll(Context context, RemoteViews views) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(new ComponentName(context, RateWidgetProvider.class), views);
    }
}
