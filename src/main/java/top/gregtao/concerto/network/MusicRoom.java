package top.gregtao.concerto.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import top.gregtao.concerto.api.MusicJsonParsers;
import top.gregtao.concerto.music.Music;
import top.gregtao.concerto.player.MusicPlayer;
import top.gregtao.concerto.util.TextUtil;

import java.util.*;

public class MusicRoom {

    public static final Map<UUID, MusicRoom> ROOMS = new HashMap<>(); // Server side

    public final UUID uuid;
    public String admin;
    public Music music;
    public boolean pause = true;
    public List<String> members = new ArrayList<>();

    public boolean isAdmin = false; // Client Side

    public MusicRoom(String creator) {
        this.uuid = UUID.randomUUID();
        this.admin = creator;
        this.members.add(creator);
    }

    public MusicRoom(UUID uuid) {
        this.uuid = uuid;
    }

    public String buildArgs(boolean withMusic) {
        return this.uuid + ":" + this.admin + ":" + (this.pause ? "1" : "0") + ":" + String.join(",", this.members)
                + ":" + (!withMusic || this.music == null ? "null" : TextUtil.toBase64(MusicJsonParsers.to(music).toString()));
    }

    public void send2EachMember(String command, String args, boolean admin, MinecraftServer server) {
        this.members.forEach(member -> {
            if (admin || !member.equals(this.admin)) serverSender(command, args, server.getPlayerManager().getPlayer(member));
        });
    }

    public void serverOnRemove(String name, MinecraftServer server) throws IllegalAccessException {
        if (!name.equals(this.admin)) throw new IllegalAccessException("No permission");
        this.send2EachMember("REM", "", true, server);
    }

    public void serverOnJoin(String name, MinecraftServer server) {
        this.members.add(name);
        this.send2EachMember("UPD", this.buildArgs(false), true, server);
    }

    public void serverOnQuit(String name, MinecraftServer server) {
        this.members.remove(name);
        this.send2EachMember("UPD", this.buildArgs(false), true, server);
    }

    public void serverOnUpdate(String name, String music, MinecraftServer server) throws IllegalAccessException {
        if (!name.equals(this.admin)) throw new IllegalAccessException("No permission");
        this.music = MusicJsonParsers.from(TextUtil.fromBase64(music));
        this.pause = false;
        this.send2EachMember("UPD", this.buildArgs(true), false, server);
    }

    public void serverOnPause(String name, boolean pause, MinecraftServer server) throws IllegalAccessException {
        if (!name.equals(this.admin)) throw new IllegalAccessException("No permission");
        this.pause = pause;
        this.send2EachMember("UPD", this.buildArgs(false), false, server);
    }

    public static void serverSender(String command, String args, ServerPlayerEntity player) {
        ConcertoPayload payload = new ConcertoPayload(ConcertoPayload.Channel.MUSIC_ROOM, command + ":" + args);
        ServerPlayNetworking.send(player, payload);
    }

    public static void serverReceiver(ConcertoPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.player().getServer();
        String[] args = payload.string.split(":");
        System.out.println(Arrays.toString(args));
        switch (args[0]) {
            case "CRE": {
                MusicRoom room = new MusicRoom(player.getName().getString());
                ROOMS.put(room.uuid, room);
                serverSender("JOI", room.buildArgs(false), player);
                player.sendMessage(Text.translatable("concerto.room.create", room.uuid.toString()));
                break;
            }
            case "REM": {
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    MusicRoom room = Objects.requireNonNull(ROOMS.get(uuid1));
                    room.serverOnRemove(player.getName().getString(), server);
                    ROOMS.remove(uuid1);
                    player.sendMessage(Text.translatable("concerto.room.remove", uuid1.toString()));
                } catch (NullPointerException | IllegalArgumentException | IllegalAccessException e) {
                    player.sendMessage(Text.translatable("concerto.room.remove.fail"));
                }
                break;
            }
            case "JOI": {
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    MusicRoom room = Objects.requireNonNull(ROOMS.get(uuid1));
                    room.serverOnJoin(player.getName().getString(), server);
                    serverSender("JOI", room.buildArgs(true), player);
                    player.sendMessage(Text.translatable("concerto.room.join", uuid1.toString()));
                } catch (NullPointerException | IllegalArgumentException e) {
                    player.sendMessage(Text.translatable("concerto.room.join.fail"));
                }
                break;
            }
            case "QUI": {
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    MusicRoom room = Objects.requireNonNull(ROOMS.get(uuid1));
                    room.serverOnQuit(player.getName().getString(), server);
                    serverSender("QUI", uuid1.toString(), player);
                    player.sendMessage(Text.translatable("concerto.room.quit", uuid1.toString()));
                } catch (NullPointerException | IllegalArgumentException e) {
                    player.sendMessage(Text.translatable("concerto.room.quit.fail"));
                }
                break;
            }
            case "UPD": {
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    MusicRoom room = Objects.requireNonNull(ROOMS.get(uuid1));
                    room.serverOnUpdate(player.getName().getString(), args[2], server);
                } catch (NullPointerException | IllegalArgumentException | IllegalAccessException e) {
                    player.sendMessage(Text.translatable("concerto.room.update.fail"));
                }
                break;
            }
            case "PAU": {
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    MusicRoom room = Objects.requireNonNull(ROOMS.get(uuid1));
                    room.serverOnPause(player.getName().getString(), args[2].equals("1"), server);
                } catch (NullPointerException | IllegalArgumentException | IllegalAccessException e) {
                    player.sendMessage(Text.translatable("concerto.room.update.fail"));
                }
                break;
            }
        }
    }

    public static MusicRoom CLIENT_ROOM;

    public static void clientCreate() {
        if (CLIENT_ROOM != null) return;
        clientSender("CRE", "");
    }

    public static void clientRemove() {
        if (CLIENT_ROOM == null) return;
        clientSender("REM", CLIENT_ROOM.uuid.toString());
    }

    public static void clientJoin(String uuid) {
        if (CLIENT_ROOM != null) return;
        clientSender("JOI", uuid);
    }

    public static void clientQuit() {
        if (CLIENT_ROOM == null) return;
        if (CLIENT_ROOM.isAdmin) clientRemove();
        else clientSender("QUI", CLIENT_ROOM.uuid.toString());
    }

    public static void clientUpdate(Music music) {
        if (CLIENT_ROOM == null || !CLIENT_ROOM.isAdmin) return;
        clientSender("UPD", CLIENT_ROOM.uuid.toString() + ":" + TextUtil.toBase64(MusicJsonParsers.to(music).toString()));
    }

    public static void clientPause(boolean pause) {
        if (CLIENT_ROOM == null || !CLIENT_ROOM.isAdmin) return;
        clientSender("PAU", CLIENT_ROOM.uuid.toString() + (pause ? ":1" : ":0"));
    }

    public static void clientSender(String command, String args) {
        ConcertoPayload payload = new ConcertoPayload(ConcertoPayload.Channel.MUSIC_ROOM, command + ":" + args);
        ClientPlayNetworking.send(payload);
    }

    public static void clientReceiver(ConcertoPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        if (client.player == null) return;
        ClientPlayerEntity player = client.player;
        String[] args = payload.string.split(":");
        System.out.println(Arrays.toString(args));
        switch (args[0]) {
            case "REM": {
                CLIENT_ROOM = null;
                break;
            }
            case "JOI": {
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    CLIENT_ROOM = new MusicRoom(uuid1);
                    client.keyboard.setClipboard(uuid1.toString());
                    CLIENT_ROOM.admin = args[2];
                    if (CLIENT_ROOM.admin.equals(player.getName().getString())) {
                        CLIENT_ROOM.isAdmin = true;
                    }
                    CLIENT_ROOM.members = List.of(args[4].split(","));
                    CLIENT_ROOM.pause = args[3].equals("1");
                    if (!args[5].equals("null")) {
                        CLIENT_ROOM.music = MusicJsonParsers.from(TextUtil.fromBase64(args[5]));
                        MusicPlayer.INSTANCE.playTempMusic(CLIENT_ROOM.music, () -> {
                            if (CLIENT_ROOM.pause) MusicPlayer.INSTANCE.pause();
                        });
                    }
                    player.sendMessage(Text.translatable("concerto.room.join", uuid1.toString()));
                } catch (NullPointerException | IllegalArgumentException e) {
                    player.sendMessage(Text.translatable("concerto.room.join.fail"));
                }
                break;
            }
            case "QUI": {
                if (CLIENT_ROOM == null) break;
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    if (CLIENT_ROOM.uuid.compareTo(uuid1) == 0) CLIENT_ROOM = null;
                    player.sendMessage(Text.translatable("concerto.room.quit", uuid1.toString()));
                } catch (NullPointerException | IllegalArgumentException e) {
                    player.sendMessage(Text.translatable("concerto.room.join.fail"));
                }
                break;
            }
            case "UPD": {
                if (CLIENT_ROOM == null) break;
                try {
                    UUID uuid1 = UUID.fromString(args[1]);
                    if (CLIENT_ROOM.uuid.compareTo(uuid1) != 0) break;
                    CLIENT_ROOM.admin = args[2];
                    CLIENT_ROOM.members = List.of(args[4].split(","));
                    CLIENT_ROOM.pause = args[3].equals("1");
                    if (!args[5].equals("null")) {
                        CLIENT_ROOM.music = MusicJsonParsers.from(TextUtil.fromBase64(args[5]));
                        MusicPlayer.INSTANCE.playTempMusic(CLIENT_ROOM.music, () -> {
                            if (CLIENT_ROOM.pause) MusicPlayer.INSTANCE.pause();
                        });
                    } else if (CLIENT_ROOM.pause) {
                        MusicPlayer.INSTANCE.pause();
                    } else {
                        MusicPlayer.INSTANCE.resume();
                    }
                } catch (NullPointerException | IllegalArgumentException e) {
                    player.sendMessage(Text.translatable("concerto.room.update.fail"));
                }
                break;
            }
        }
    }
}
