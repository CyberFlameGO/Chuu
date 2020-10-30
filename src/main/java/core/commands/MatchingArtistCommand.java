package core.commands;

import core.commands.utils.PrivacyUtils;
import core.exceptions.LastFmException;
import core.otherlisteners.Reactionary;
import core.parsers.NumberParser;
import core.parsers.OnlyUsernameParser;
import core.parsers.Parser;
import core.parsers.params.ChuuDataParams;
import core.parsers.params.NumberParameters;
import dao.ChuuService;
import dao.entities.DiscordUserDisplay;
import dao.entities.LbEntry;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.parsers.ExtraParser.LIMIT_ERROR;

public class MatchingArtistCommand extends ConcurrentCommand<NumberParameters<ChuuDataParams>> {


    public MatchingArtistCommand(ChuuService dao) {
        super(dao);
        this.respondInPrivate = false;
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public Parser<NumberParameters<ChuuDataParams>> initParser() {
        Map<Integer, String> map = new HashMap<>(2);
        map.put(LIMIT_ERROR, "The number introduced must be positive and not very big");
        String s = "You can also introduce a number to vary the number of plays needed to award a match, " +
                "defaults to 1";
        return new NumberParser<>(new OnlyUsernameParser(getService()),
                null,
                Integer.MAX_VALUE,
                map, s, false, true);
    }

    @Override
    public String getDescription() {
        return "Users ordered by matching number of artists";
    }

    @Override
    public List<String> getAliases() {
        return List.of("matching");
    }

    @Override
    public String getName() {
        return "Matching artists";
    }

    @Override
    public void onCommand(MessageReceivedEvent e) throws LastFmException, InstanceNotFoundException {
        NumberParameters<ChuuDataParams> outer = parser.parse(e);
        if (outer == null) {
            return;
        }
        ChuuDataParams params = outer.getInnerParams();

        long discordId = params.getLastFMData().getDiscordId();
        int threshold = outer.getExtraParam() == null ? 1 : Math.toIntExact(outer.getExtraParam());
        List<LbEntry> list = getService().matchingArtistsCount(params.getLastFMData().getName(), e.getGuild().getIdLong(), threshold);
        list.forEach(cl -> cl.setDiscordName(getUserString(e, cl.getDiscordId(), cl.getLastFmId())));
        MessageBuilder messageBuilder = new MessageBuilder();

        DiscordUserDisplay userInformation = CommandUtil.getUserInfoConsideringGuildOrNot(e, discordId);
        String url = userInformation.getUrlImage();
        String usableName = userInformation.getUsername();

        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(CommandUtil.randomColor())
                .setThumbnail(url);
        StringBuilder a = new StringBuilder();

        if (list.isEmpty()) {
            sendMessageQueue(e, "No one has any matching artist with you :(");
            return;
        }

        for (int i = 0; i < 10 && i < list.size(); i++) {
            a.append(i + 1).append(PrivacyUtils.toString(list.get(i)));
        }
        embedBuilder.setDescription(a).setTitle("Matching artists with " + usableName)
                .setFooter(String.format("%s has %d total artist!%n", CommandUtil.markdownLessUserString(usableName, discordId, e), getService().getUserArtistCount(params.getLastFMData().getName(), 0)), null);
        e.getChannel().sendMessage(messageBuilder.setEmbed(embedBuilder.build()).build()).queue(mes ->
                new Reactionary<>(list, mes, embedBuilder));
    }
}
