package core.commands;

import com.neovisionaries.i18n.CountryCode;
import core.apis.discogs.DiscogsApi;
import core.apis.discogs.DiscogsSingleton;
import core.apis.last.TopEntity;
import core.apis.last.chartentities.ChartUtil;
import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.exceptions.InstanceNotFoundException;
import core.exceptions.LastFmException;
import core.imagerenderer.ChartQuality;
import core.imagerenderer.CollageMaker;
import core.otherlisteners.Reactionary;
import core.parsers.CountryParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.ChartParameters;
import core.parsers.params.CountryParameters;
import dao.ChuuService;
import dao.entities.ArtistUserPlays;
import dao.entities.ChartMode;
import dao.entities.DiscordUserDisplay;
import dao.entities.UrlCapsule;
import dao.musicbrainz.MusicBrainzService;
import dao.musicbrainz.MusicBrainzServiceSingleton;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ArtistFromCountryCommand extends ConcurrentCommand<CountryParameters> {

    final DiscogsApi discogsApi;
    final Spotify spotifyApi;

    private final MusicBrainzService mb;

    public ArtistFromCountryCommand(ChuuService dao) {
        super(dao);
        mb = MusicBrainzServiceSingleton.getInstance();
        discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();
        spotifyApi = SpotifySingleton.getInstance();
    }

    @Override
    protected CommandCategory getCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public Parser<CountryParameters> getParser() {
        CountryParser countryParser = new CountryParser(getService());
        countryParser.addOptional(new OptionalEntity("--image", "show this with a chart instead of a list "));
        return countryParser;
    }

    @Override
    public String getDescription() {
        return "Your top artist that are from a specific country";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("from");
    }

    @Override
    public String getName() {
        return "Artist from a country";
    }

    void doImage(List<ArtistUserPlays> list, BlockingQueue<UrlCapsule> ogQuee, CountryParameters countryParameters) {
        AtomicInteger integer = new AtomicInteger(0);
        List<UrlCapsule> collect = ogQuee.stream()
                .filter(x -> list.stream().anyMatch(y -> y.getArtistName().equalsIgnoreCase(x.getArtistName())))
                .takeWhile(x -> {
                    x.setPos(integer.getAndIncrement());
                    return x.getPos() < 25;
                })
                .peek(x -> {
                    try {
                        String artistImageUrl = CommandUtil.getArtistImageUrl(getService(), x.getArtistName(), lastFM, discogsApi, spotifyApi);
                        x.setUrl(artistImageUrl);
                    } catch (LastFmException ignored) {

                    }
                })
                .collect(Collectors.toList());
        int rows = (int) Math.floor(Math.sqrt(collect.size()));
        rows = Math.min(rows, 5);
        int cols = rows;

        BufferedImage bufferedImage = CollageMaker.generateCollageThreaded(rows, cols, new LinkedBlockingDeque<>(collect), ChartQuality.PNG_BIG,
                false);
        sendImage(bufferedImage, countryParameters.getE());

    }

    @Override
    void onCommand(MessageReceivedEvent e) throws LastFmException, InstanceNotFoundException {
        CountryParameters parameters = parser.parse(e);
        if (parameters == null) {
            return;
        }

        CountryCode country = parameters.getCode();
        BlockingQueue<UrlCapsule> queue = new ArrayBlockingQueue<>(2000);
        String name = parameters.getLastFMData().getName();
        lastFM.getChart(name, parameters.getTimeFrame().toApiFormat(), 2000, 1, TopEntity.ARTIST, ChartUtil.getParser(parameters.getTimeFrame(), TopEntity.ARTIST, ChartParameters.toListParams(), lastFM, name), queue);

        Long discordId = parameters.getLastFMData().getDiscordId();
        List<ArtistUserPlays> list = this.mb.getArtistFromCountry(country, queue, discordId);
        DiscordUserDisplay userInformation = CommandUtil.getUserInfoConsideringGuildOrNot(e, discordId);
        String userName = userInformation.getUsername();
        String userUrl = userInformation.getUrlImage();
        String countryRep;
        if (country.getAlpha2().equalsIgnoreCase("su")) {
            countryRep = "☭";
        } else {
            countryRep = ":flag_" + country.getAlpha2().toLowerCase();
        }
        String usableTime = parameters.getTimeFrame().getDisplayString();
        if (list.isEmpty()) {
            sendMessageQueue(e, userName + " doesnt have any artist from " + countryRep + ": " + usableTime);
            return;
        }
        if (parameters.hasOptional("--image")) {
            doImage(list, queue, parameters);
            return;
        }
        StringBuilder a = new StringBuilder();

        for (int i = 0; i < 10 && i < list.size(); i++) {
            a.append(i + 1).append(list.get(i).toString());
        }

        String title = userName + "'s top artists from " + countryRep + (":");
        MessageBuilder messageBuilder = new MessageBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(CommandUtil.randomColor())
                .setThumbnail(userUrl)
                .setFooter(CommandUtil.markdownLessUserString(userName, discordId, e) + " has " + list.size() +
                        (list.size() == 1 ? " artist " : " artists ") + "from " + country.getName() + " " + usableTime, null)
                .setTitle(title)
                .setDescription(a);
        messageBuilder.setEmbed(embedBuilder.build()).sendTo(e.getChannel()).queue(mes ->
                new Reactionary<>(list, mes, embedBuilder));
    }
}
