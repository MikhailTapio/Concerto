package top.gregtao.concerto.music.parser;

import com.google.gson.JsonObject;
import top.gregtao.concerto.api.JsonParser;
import top.gregtao.concerto.enums.Sources;
import top.gregtao.concerto.music.BilibiliMusic;

public class BilibiliMusicJsonParser implements JsonParser<BilibiliMusic> {
    @Override
    public BilibiliMusic fromJson(JsonObject object) {
        return new BilibiliMusic(object.get("bvid").getAsString());
    }

    @Override
    public JsonObject toJson(JsonObject object, BilibiliMusic music) {
        object.addProperty("bvid", music.getBvid());
        return object;
    }

    @Override
    public String name() {
        return Sources.BILIBILI.asString();
    }
}
