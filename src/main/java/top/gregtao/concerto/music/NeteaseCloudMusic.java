package top.gregtao.concerto.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;
import top.gregtao.concerto.api.CacheableMusic;
import top.gregtao.concerto.api.JsonParser;
import top.gregtao.concerto.api.MusicJsonParsers;
import top.gregtao.concerto.api.SimpleStringIdentifiable;
import top.gregtao.concerto.enums.Sources;
import top.gregtao.concerto.http.HttpClientInputStream;
import top.gregtao.concerto.http.HttpURLInputStream;
import top.gregtao.concerto.http.netease.NeteaseCloudApiClient;
import top.gregtao.concerto.music.lyrics.Lyrics;
import top.gregtao.concerto.music.meta.music.BasicMusicMetaData;
import top.gregtao.concerto.music.meta.music.MusicMetaData;
import top.gregtao.concerto.music.meta.music.UnknownMusicMeta;
import top.gregtao.concerto.util.FileUtil;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NeteaseCloudMusic extends Music implements CacheableMusic {
    private final String id;
    private final Level level;

    public NeteaseCloudMusic(String id, Level level) {
        this.id = id;
        this.level = level;
    }

    public NeteaseCloudMusic(JsonObject object, Level level) {
        this.id = object.get("id").getAsString();
        this.level = level;
        this.setMusicMeta(parseMetaData(object));
    }

    @Override
    public InputStream getMusicSource() throws MusicSourceNotFoundException {
        try {
            return FileUtil.createBuffered(new HttpURLInputStream(new URL(this.getRawPath())));
        } catch (Exception e) {
            throw new MusicSourceNotFoundException(e);
        }
    }

    public String getRawPath() {
        JsonObject object = NeteaseCloudApiClient.INSTANCE.getMusicLink(this.id, this.level)
                .getAsJsonArray("data").get(0).getAsJsonObject();
        return object.get("url").getAsString();
    }

    @Override
    public Pair<Lyrics, Lyrics> getLyrics() {
        try {
            return NeteaseCloudApiClient.INSTANCE.getLyrics(this.id);
        } catch (Exception e) {
            return null;
        }
    }

    public static MusicMetaData parseMetaData(JsonObject object) {
        String name = object.get("name").getAsString();
        long duration = object.get("dt").getAsLong();
        JsonArray authors = object.getAsJsonArray("ar");
        List<String> authorList = new ArrayList<>();
        authors.forEach(element -> authorList.add(element.getAsJsonObject().get("name").getAsString()));
        JsonObject album = object.getAsJsonObject("al");
        String headPic = "";
        if (!album.isJsonNull()) {
            JsonElement element = album.get("picUrl");
            if (element != null && !element.isJsonNull()) headPic = element.getAsString();
        }
        return new BasicMusicMetaData(String.join(", ", authorList), name,
                Sources.NETEASE_CLOUD.getName().getString(), duration, headPic);
    }

    @Override
    public void load() {
        try {
            JsonObject object = NeteaseCloudApiClient.INSTANCE.getMusicDetail(this.id)
                    .getAsJsonArray("songs").get(0).getAsJsonObject();
            this.setMusicMeta(parseMetaData(object));
        } catch (Exception e) {
            this.setMusicMeta(new UnknownMusicMeta(Sources.NETEASE_CLOUD.getName().getString()));
        }
        super.load();
    }

    @Override
    public JsonParser<Music> getJsonParser() {
        return MusicJsonParsers.NETEASE_CLOUD;
    }

    public String getId() {
        return this.id;
    }

    public Level getLevel() {
        return this.level;
    }

    @Override
    public String getSuffix() {
        return "mp3";
    }

    @Override
    public Music getMusic() {
        return this;
    }

    public enum Level implements SimpleStringIdentifiable {
        STANDARD,
        HIGHER,
        EXHIGH,
        LOSSLESS,
        HIRES;

        public static final Codec<Level> CODEC = StringIdentifiable.createCodec(Level::values);
    }
}
