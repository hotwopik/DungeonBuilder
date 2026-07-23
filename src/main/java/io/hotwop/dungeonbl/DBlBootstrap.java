package io.hotwop.dungeonbl;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import io.hotwop.dungeonbl.mark.MarkType;
import io.hotwop.dungeonbl.mark.type.*;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;


public final class DBlBootstrap implements PluginBootstrap{
    public static ComponentLogger logger;

    @Override
    public void bootstrap(@NotNull BootstrapContext context){
        LifecycleEventManager<BootstrapContext> manager = context.getLifecycleManager();
        logger=context.getLogger();

        CommandAPI.onLoad(new CommandAPIPaperConfig(context)
            .initializeNBTAPI(ReadableNBT.class,NBT::wrapNMSTag)
            .silentLogs(true)
        );

        manager.registerEventHandler(LifecycleEvents.COMMANDS,e->{
            MarkType.standardTypes.forEach(MarkType::register);
            DBlCommand.pluginCommandRoot.register("dungeonbl");
        });
    }
}
