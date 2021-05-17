package core.commands.abstracts;

import core.commands.Context;
import core.commands.utils.ChuuEmbedBuilder;
import core.imagerenderer.GraphicUtils;
import core.imagerenderer.util.pie.IPieableList;
import core.parsers.params.CommandParameters;
import dao.ChuuService;
import net.dv8tion.jda.api.EmbedBuilder;
import org.knowm.xchart.PieChart;

import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

public abstract class PieableListCommand<T, Y extends CommandParameters> extends ConcurrentCommand<Y> {
    public final IPieableList<T, Y> pie;

    protected PieableListCommand(ChuuService dao) {
        super(dao);
        this.pie = null;
    }


    @Override
    protected void onCommand(Context e, @NotNull Y params) {

        if (params.hasOptional("pie")) {
            doPie(getList(params), params);
            return;
        }

        printList(getList(params), params);
    }

    public abstract void doPie(T data, Y parameters);

    public EmbedBuilder initList(List<String> strList, Context e) {
        StringBuilder a = new StringBuilder();
        for (int i = 0, size = strList.size(); i < 10 && i < size; i++) {
            String text = strList.get(i);
            a.append(i + 1).append(text);
        }
        return new ChuuEmbedBuilder(e)
                .setDescription(a);
    }

    public void doPie(PieChart pieChart, Y chartParameters, int count) {
        BufferedImage bufferedImage = new BufferedImage(1000, 750, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();
        String s = fillPie(pieChart, chartParameters, count);

        GraphicUtils.setQuality(g);
        pieChart.paint(g, 1000, 750);

        Font annotationsFont = pieChart.getStyler().getAnnotationsFont();
        pieChart.paint(g, 1000, 750);
        g.setFont(annotationsFont.deriveFont(11.0f));
        Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(s, g);
        g.drawString(s, 1000 - 10 - (int) stringBounds.getWidth(), 740 - 2);
        sendImage(bufferedImage, chartParameters.getE());
    }

    protected abstract String fillPie(PieChart pieChart, Y params, int count);

    public abstract T getList(Y params);

    public abstract void printList(T data, Y params);
}
