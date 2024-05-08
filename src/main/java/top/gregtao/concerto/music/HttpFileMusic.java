package top.gregtao.concerto.music;

import top.gregtao.concerto.api.CacheableMusic;
import top.gregtao.concerto.api.JsonParser;
import top.gregtao.concerto.api.MusicJsonParsers;
import top.gregtao.concerto.enums.Sources;
import top.gregtao.concerto.http.HttpURLInputStream;
import top.gregtao.concerto.music.meta.music.TimelessMusicMetaData;
import top.gregtao.concerto.util.FileUtil;
import top.gregtao.concerto.util.HttpUtil;
import top.gregtao.concerto.util.TextUtil;

import java.io.InputStream;
import java.net.URL;

public class HttpFileMusic extends PathFileMusic implements CacheableMusic {

    public HttpFileMusic(String rawPath) {
        super(rawPath);
    }

    @Override
    public InputStream getMusicSource() throws MusicSourceNotFoundException {
        try {
            return FileUtil.createBuffered(new HttpURLInputStream(new URL(this.getRawPath())));
        } catch (Exception e) {
            throw new MusicSourceNotFoundException(e);
        }
    }

    @Override
    public void load() {
        this.setMusicMeta(new TimelessMusicMetaData(
                TextUtil.getTranslatable("ah.unknown"), this.getRawPath(),
                Sources.INTERNET.getName().getString()
        ));
        super.load();
    }

    @Override
    public JsonParser<Music> getJsonParser() {
        return MusicJsonParsers.HTTP_FILE;
    }

    @Override
    public String getSuffix() {
        return HttpUtil.getSuffix(this.getRawPath());
    }

    @Override
    public Music getMusic() {
        return this;
    }
}
