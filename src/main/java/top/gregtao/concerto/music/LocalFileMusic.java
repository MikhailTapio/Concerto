package top.gregtao.concerto.music;

import com.goxr3plus.streamplayer.enums.AudioType;
import com.goxr3plus.streamplayer.tools.TimeTool;
import com.mojang.datafixers.util.Pair;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import top.gregtao.concerto.ConcertoClient;
import top.gregtao.concerto.api.*;
import top.gregtao.concerto.music.lyrics.DefaultFormatLyrics;
import top.gregtao.concerto.music.lyrics.Lyrics;
import top.gregtao.concerto.music.meta.music.BasicMusicMetaData;
import top.gregtao.concerto.enums.Sources;
import top.gregtao.concerto.util.FileUtil;
import top.gregtao.concerto.util.HttpUtil;
import top.gregtao.concerto.util.TextUtil;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalFileMusic extends PathFileMusic {
    public static List<String> FORMATS = List.of("mp3", "ogg", "wav", "flac", "aac");

    public LocalFileMusic(String rawPath) throws UnsafeMusicException {
        super(rawPath.charAt(0) == '"' && rawPath.charAt(rawPath.length() - 1) == '"' ?
                rawPath.substring(1, rawPath.length() - 1) : rawPath);
        String suffix = HttpUtil.getSuffix(this.getRawPath()).substring(1);
        if (!FORMATS.contains(suffix)) {
            ConcertoClient.LOGGER.warn("Unsupported source: {}", suffix);
            throw new UnsafeMusicException("Unsupported source: " + suffix);
        }
    }

    @Override
    public InputStream getMusicSource() {
        try {
            return AudioSystem.getAudioInputStream(new File(this.getRawPath()));
        } catch (FileNotFoundException e) {
            throw new MusicSourceNotFoundException(e);
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Pair<Lyrics, Lyrics> getLyrics() throws IOException {
        return Pair.of(new DefaultFormatLyrics().load(String.join("\n",
                Files.readAllLines(Path.of(HttpUtil.getRawPathWithoutSuffix(this.getRawPath()) + ".lrc")))), null);
    }

    @Override
    public void load() {
        String author, title;
        try {
            AudioFile file = AudioFileIO.read(new File(this.getRawPath()));
            Tag tag = file.getTagAndConvertOrCreateDefault();
            title = tag.getFirst(FieldKey.TITLE);
            author = FileUtil.getLocalAudioAuthors(file);
        } catch (Exception e) {
            author = title = null;
        }
        this.setMusicMeta(new BasicMusicMetaData(
                author == null || author.isEmpty() ? TextUtil.getTranslatable("concerto.unknown") : author,
                title == null || title.isEmpty() ? this.getRawPath() : title,
                Sources.LOCAL_FILE.getName().getString(),
                TimeTool.durationInMilliseconds(new File(this.getRawPath()).getAbsolutePath(), AudioType.FILE)
        ));
        super.load();
    }

    @Override
    public JsonParser<Music> getJsonParser() {
        return MusicJsonParsers.LOCAL_FILE;
    }

    public static ArrayList<Music> getMusicsInFolder(File file) {
        ArrayList<Music> list = new ArrayList<>();
        if (!file.isDirectory()) return list;
        File[] files = file.listFiles((dir, name) -> FORMATS.contains(FileUtil.getSuffix(name).toLowerCase()));
        if (files == null) return list;
        for (File file1 : files) {
            list.add(new LocalFileMusic(file1.getAbsolutePath()));
        }
        return list;
    }
}
