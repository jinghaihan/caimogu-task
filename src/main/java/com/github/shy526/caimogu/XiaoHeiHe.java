package com.github.shy526.caimogu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.shy526.okhttp.OkHttpHelp;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.Request;
import org.apache.commons.codec.digest.DigestUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;

@Slf4j
public class XiaoHeiHe {
    public static String RMStr = "ANGLE (NVIDIA, NVIDIA GeForce RTX 2080 Ti (0x00001E07) Direct3D11 vs_5_0 ps_5_0, D3D11)" + "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36 Edg/151.0.0.0Win32Asia/Shanghaizh-CN";
    public static String domainName = "https://api.xiaoheihe.cn";
    public static String SearchGameNamePath = "/bbs/app/api/general/search/v1";
    public static String GameCommentsPath = "/bbs/app/link/game/comments";

    public static ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");


    public static List<JSONObject> SearchGameName(String cnName) {
        Map<String, String> params = getBaseParams(SearchGameNamePath);
        params.put("q", cnName);
        params.put("search_type", "game");
        params.put("offset", "0");
        params.put("limit", "30");
        String url = OkHttpHelp.buildGetUrl(domainName, SearchGameNamePath, params);
        Request request = new Request.Builder().url(url).get().build();
        List<JSONObject> appIds = OkHttpHelp.executeRequestParse(request, (JSONObject json, Headers headers) -> {
            List<JSONObject> result = new ArrayList<>();
            if (!"ok".equals(json.getString("status"))) {
                return result;
            }
            JSONObject resultObj = json.getJSONObject("result");
            if (resultObj == null) {
                return result;
            }
            JSONArray jsonArray = resultObj.getJSONArray("items");
            if (jsonArray == null) {
                return result;
            }
            for (Object itemObj : jsonArray) {
                JSONObject item = (JSONObject) itemObj;
                JSONObject info = item.getJSONObject("info");
                String name = info.getString("name");
                String appId = info.getString("steam_appid");
                JSONObject r = new JSONObject();
                r.put("name", name);
                r.put("appId", appId);
                result.add(r);
            }
            return result;
        }, 0);
        if (appIds == null) {
            appIds = Lists.newArrayList();
        }
        return appIds;
    }

    public static Set<String> getCommentsByGameName(String gameName) {
        List<JSONObject> games = SearchGameName(gameName);
        if (games.isEmpty()) {
            return Sets.newHashSet();
        }
        String appId = games.get(0).getString("appId");
        return getComments(appId);
    }

    public static Set<String> getComments(String appId) {
        Map<String, String> params = getBaseParams(SearchGameNamePath);
        params.put("appid", appId);
        params.put("limit", "30");
        Set<String> comments = Sets.newHashSet();
        for (int i = 0; i < 50; i++) {
            params.put("offset", i + "");
            String url = OkHttpHelp.buildGetUrl(domainName, GameCommentsPath, params);
            Request request = new Request.Builder().url(url).get().build();
            Set<String> temp = OkHttpHelp.executeRequestParse(request, (JSONObject json, Headers headers) -> {
                Set<String> result = Sets.newHashSet();
                if (!"ok".equals(json.getString("status"))) {
                    return result;
                }
                JSONObject resultObj = json.getJSONObject("result");
                if (resultObj == null) {
                    return result;
                }
                JSONArray jsonArray = resultObj.getJSONArray("links");
                if (jsonArray == null) {
                    return result;
                }
                for (Object itemObj : jsonArray) {
                    JSONObject item = (JSONObject) itemObj;
                    String comment = item.getString("description");
                    result.add(comment);
                }
                return result;
            }, 0);
            if (temp != null && temp.isEmpty()) {
                break;
            }
            if (temp != null) {
                comments.addAll(temp);
            }

        }
        return comments;
    }


    public static long getTimeSec() {
        return (long) System.currentTimeMillis() / 1000;
    }

    public static String getMd5Key() {
        String temp = (getTimeSec() + System.currentTimeMillis()) + (Math.random() + "");
        return DigestUtils.md5Hex(temp + RMStr + RMStr).toUpperCase();
    }

    public static Map<String, String> getBaseParams(String urlPath) {
        String md5Key = getMd5Key();
        long timeSec = getTimeSec();
        Map<String, String> result = new HashMap<>();
        String hky = getHky(urlPath, timeSec, md5Key);
        result.put("hkey", hky);
        result.put("_time", timeSec + "");
        result.put("nonce", md5Key);
        result.put("version", "999.0.4");
        result.put("os_type", "web");
        return result;
    }

    public static String getHky(String urlPath, long timeSec, String md5Key) {
        String jsCode = CaiMoGuHelp.readResourcesJs("xiaoHeiHeSignature.js");
        try {
            engine.eval(jsCode);
            Invocable inv = (Invocable) engine;
            Object o1 = inv.invokeFunction("Tr_1", urlPath, timeSec + 1, md5Key);
            return (String) inv.invokeFunction("Tr_2", DigestUtils.md5Hex(o1.toString()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
