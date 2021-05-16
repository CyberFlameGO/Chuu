package core.commands.config;

import core.commands.Context;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.ChuuEmbedBuilder;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.commands.utils.PrivacyUtils;
import core.otherlisteners.Confirmator;
import core.otherlisteners.util.ConfirmatorItem;
import core.parsers.NoOpParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.ChuuService;
import dao.entities.DiscordUserDisplay;
import dao.entities.LastFMData;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;

import javax.validation.constraints.NotNull;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class UnsetCommand extends ConcurrentCommand<CommandParameters> {
    public UnsetCommand(ChuuService dao) {
        super(dao);
    }


    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.STARTING;
    }

    @Override
    public Parser<CommandParameters> initParser() {
        return NoOpParser.INSTANCE;
    }

    @Override
    public String getDescription() {
        return "Removes a user completely from the bot system";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("unset");
    }

    @Override
    public String getName() {
        return "Unset";
    }

    @Override
    protected void onCommand(Context e, @NotNull CommandParameters params) throws InstanceNotFoundException {
        long idLong = e.getAuthor().getIdLong();
        // Check if it exists
        LastFMData data = db.findLastFMData(idLong);
        String userString = getUserString(e, idLong);
        DiscordUserDisplay uInfo = CommandUtil.getUserInfoNotStripped(e, idLong);

        EmbedBuilder embedBuilder = new ChuuEmbedBuilder()
                .setColor(Color.RED)
                .setAuthor("User Deletion Confirmation", PrivacyUtils.getLastFmUser(data.getName()), uInfo.getUrlImage())
                .setFooter("Unsetting doesn't fix any issue that might exist with scrobbles and will delete all your info including pictures submitted!")
                .setDescription(String.format("%s, are you sure you want to delete all your info from the bot?", userString));

        List<ConfirmatorItem> list = List.of(
                new ConfirmatorItem("\u2714", who -> who.clear().setTitle(String.format("%s was removed completely from the bot", userString)), (z) -> db.removeUserCompletely(idLong)),
                new ConfirmatorItem("\u274c", who -> who.clear().setTitle(String.format("Didn't do anything with user %s", userString)), (z) -> {
                }));
        e.sendMessage(embedBuilder.build())
                .queue(message -> new Confirmator(embedBuilder, message, idLong, list));
    }
}
