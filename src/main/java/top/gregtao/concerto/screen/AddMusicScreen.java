package top.gregtao.concerto.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import top.gregtao.concerto.music.*;
import top.gregtao.concerto.music.list.NeteaseCloudPlaylist;
import top.gregtao.concerto.player.MusicPlayer;
import top.gregtao.concerto.player.MusicPlayerHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;

public class AddMusicScreen extends ApplyDraggedFileScreen {

    public AddMusicScreen(Screen parent) {
        super(Text.translatable("concerto.screen.manual_add"), parent);
    }

    private void addLabel(Text text, int centerX, int y, Consumer<String> onClick) {
        TextFieldWidget widget = new TextFieldWidget(this.textRenderer, centerX - 30, y, 90, 20, text);
        widget.setMaxLength(1024);
        TextWidget textWidget = new TextWidget(centerX - 120, y + 2, 90, 20, text, this.textRenderer);
        textWidget.alignLeft();
        this.addDrawableChild(widget);
        this.addDrawableChild(textWidget);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("concerto.screen.add"),
                button -> onClick.accept(widget.getText())).position(centerX + 65, y).size(60, 20).build());
        this.addSelectableChild(widget);
    }

    @Override
    protected void init() {
        super.init();
        this.addLabel(Text.translatable("concerto.screen.add.local_file"), this.width / 2, 20,
                str -> {
                    try {
                        MusicPlayer.INSTANCE.addMusicHere(new LocalFileMusic(str), true);
                    } catch (UnsafeMusicException e) {
                        this.displayAlert(Text.translatable("concerto.error.invalid_path"));
                    }
                });
        this.addLabel(Text.translatable("concerto.screen.add.local_file.folder"), this.width / 2, 45, str -> MusicPlayer.run(() -> {
            ArrayList<Music> list = LocalFileMusic.getMusicsInFolder(new File(str));
            MusicPlayer.INSTANCE.addMusic(list, () -> MusicPlayer.INSTANCE.skipTo(MusicPlayerHandler.INSTANCE.getMusicList().size() - list.size()));
        }));
        this.addLabel(Text.translatable("concerto.screen.add.internet"), this.width / 2, 70,
                str -> MusicPlayer.INSTANCE.addMusicHere(new HttpFileMusic(str), true));
        this.addLabel(Text.translatable("concerto.screen.add.netease_cloud"), this.width / 2, 95,
                str -> MusicPlayer.INSTANCE.addMusicHere(new NeteaseCloudMusic(str, NeteaseCloudMusic.Level.STANDARD), true));
        this.addLabel(Text.translatable("concerto.screen.add.netease_cloud.playlist"), this.width / 2, 120, str -> {
            NeteaseCloudPlaylist playlist = new NeteaseCloudPlaylist(str, false);
            playlist.load(() -> MinecraftClient.getInstance().setScreen(new PlaylistPreviewScreen(playlist, this)));
        });
        this.addLabel(Text.translatable("concerto.screen.add.netease_cloud.album"), this.width / 2, 145, str -> {
            NeteaseCloudPlaylist playlist = new NeteaseCloudPlaylist(str, false);
            playlist.load(() -> MinecraftClient.getInstance().setScreen(new PlaylistPreviewScreen(playlist, this)));
        });
        this.addLabel(Text.translatable("concerto.screen.add.qq"), this.width / 2, 170,
                str -> MusicPlayer.INSTANCE.addMusicHere(new QQMusic(str), true, () -> {
                    if (!MusicPlayer.INSTANCE.started) MusicPlayer.INSTANCE.start();
                }));
        this.addLabel(Text.translatable("concerto.screen.add.bilibili"), this.width / 2, 195,
                str -> MusicPlayer.INSTANCE.addMusicHere(new BilibiliMusic(str), true));
    }
}
