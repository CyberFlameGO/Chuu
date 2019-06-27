package main.Parsers;

import DAO.DaoImplementation;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Collections;
import java.util.List;

public class OnlyUsernameParser extends DaoParser {
	public OnlyUsernameParser(DaoImplementation dao) {
		super(dao);
	}

	@Override
	public String[] parse(MessageReceivedEvent e) {

		String[] message = getSubMessage(e.getMessage());

		String discordName = getLastFmUsername1input(message, e.getAuthor().getIdLong(), e);
		if (discordName == null) {
			return null;
		}
		return new String[]{discordName};
	}

	@Override
	public List<String> getUsage(String commandName) {
		return Collections.singletonList("**" + commandName + " *username*** \n" +
				"\t If username is not specified defaults to authors account\n\n");
	}
}