package io.hotwop.dungeonbl.integrations;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.session.DungeonSession;
import io.papermc.paper.plugin.configuration.PluginMeta;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class PlaceholderAPIIntegration extends PlaceholderExpansion{
    private final String author;
    private final String version;

    private static final List<String> placeholders=List.of(
        "rfd_session_count",
        "rfd_player_dungeon",
        "rfd_player_dungeon_player_count",
        "rfd_player_dungeon_players",
        "rfd_player_in_dungeon"
    );

    public PlaceholderAPIIntegration(DungeonBl plugin){
        PluginMeta meta=plugin.getPluginMeta();
        author=meta.getAuthors().get(0);
        version=meta.getVersion();
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "rfd";
    }
    @Override
    @NotNull
    public String getAuthor() {
        return author;
    }
    @Override
    @NotNull
    public String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @NotNull
    @Override
    public List<String> getPlaceholders(){
        return placeholders;
    }

    @Nullable
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch(params){
            case "session_count" -> {
                return Integer.toString(DungeonSession.sessionsCount());
            }
            case "player_dungeon"->{
                Optional<DungeonSession> session= DungeonSession.all().stream().filter(sess->sess.players().contains((Player)player)).findFirst();
                if(session.isPresent())return session.get().dungeonName;
                return "";
            }
            case "player_dungeon_player_count"->{
                Optional<DungeonSession> session= DungeonSession.all().stream().filter(sess->sess.players().contains((Player)player)).findFirst();
                return session.map(value -> Integer.toString(value.playerCount())).orElse("0");
            }
            case "player_dungeon_players"->{
                Optional<DungeonSession> session= DungeonSession.all().stream().filter(sess->sess.players().contains((Player)player)).findFirst();
                if(session.isEmpty())return "";
                StringBuilder out= new StringBuilder();
                for(Player plr:session.get().players()) out.append(plr.getName()).append("\n");
                return out.toString();
            }
            case "player_in_dungeon"->{
                Optional<DungeonSession> session= DungeonSession.all().stream().filter(sess->sess.players().contains((Player)player)).findFirst();
                if(session.isPresent())return "1";
                return "0";
            }
        }

        return null;
    }
}
