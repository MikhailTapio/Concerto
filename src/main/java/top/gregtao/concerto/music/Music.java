package top.gregtao.concerto.music;

import com.mojang.datafixers.util.Pair;
import top.gregtao.concerto.api.CacheableMusic;
import top.gregtao.concerto.api.JsonParsable;
import top.gregtao.concerto.api.LazyLoadable;
import top.gregtao.concerto.api.WithMetaData;
import top.gregtao.concerto.config.ClientConfig;
import top.gregtao.concerto.config.MusicCacheManager;
import top.gregtao.concerto.music.lyric.Lyric;
import top.gregtao.concerto.music.meta.music.MusicMetaData;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public abstract class Music implements JsonParsable<Music>, LazyLoadable, WithMetaData {

    private boolean isMetaLoaded = false;
    private MusicMetaData musicMetaData = null;

    public MusicSource getMusicSourceOrNull() {
        if (this instanceof CacheableMusic cacheable) {
            if (!ClientConfig.INSTANCE.options.cacheBeforePlay) {
                MusicSource source = this.getMusicSource();
                try {
                    return source == null ? null : MusicSource.of(new ByteArrayInputStream(source.getAudioStream().readAllBytes()));
                } catch (UnsupportedAudioFileException | IOException e) {
                    return null;
                }
            }
            File child = MusicCacheManager.INSTANCE.getChild(cacheable);
            try {
                if (child == null) {
                    MusicCacheManager.INSTANCE.addMusic(cacheable);
                }
                child = MusicCacheManager.INSTANCE.getChild(cacheable);
                return child == null ? null : MusicSource.of(child);
            } catch (MusicSourceNotFoundException e) {
                return null;
            } catch (IOException | UnsupportedAudioFileException e) {
                try {
                    return this.getMusicSource();
                } catch (MusicSourceNotFoundException e1) {
                    return null;
                }
            }
        } else {
            try {
                return this.getMusicSource();
            } catch (MusicSourceNotFoundException e) {
                return null;
            }
        }
    }

    public Pair<Lyric, Lyric> getLyric() throws IOException {
        return null;
    }

    public MusicMetaData getMeta() {
        if (!this.isLoaded()) {
            this.load();
            this.isMetaLoaded = true;
        }
        return this.musicMetaData;
    }

    public void load() {
        this.isMetaLoaded = true;
    }

    public void setMusicMeta(MusicMetaData musicMetaData) {
        this.musicMetaData = musicMetaData;
        this.isMetaLoaded = true;
    }

    public boolean isLoaded() {
        return this.isMetaLoaded;
    }

    public abstract MusicSource getMusicSource() throws MusicSourceNotFoundException;
}
