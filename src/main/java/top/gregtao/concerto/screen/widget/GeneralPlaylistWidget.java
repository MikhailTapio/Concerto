package top.gregtao.concerto.screen.widget;

import top.gregtao.concerto.music.Music;
import top.gregtao.concerto.player.MusicPlayer;
import top.gregtao.concerto.player.MusicPlayerHandler;

public class GeneralPlaylistWidget extends MetadataListWidget<Music> {

    public GeneralPlaylistWidget(int width, int height, int top, int itemHeight) {
        super(width, height, top, itemHeight);
        this.reset();
    }

    @Override
    public void onDoubleClicked(ConcertoListWidget<Music>.Entry entry) {
        MusicPlayer.INSTANCE.skipTo(entry.index);
    }

    public void reset() {
        super.reset(MusicPlayerHandler.INSTANCE.getMusicList(), MusicPlayerHandler.INSTANCE.currentMusic);
    }

    public void reset(String keyword) {
        super.reset(MusicPlayerHandler.INSTANCE.getMusicList(), MusicPlayerHandler.INSTANCE.currentMusic, keyword);
    }
}
